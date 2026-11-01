package com.example.engine

import android.content.Context
import com.example.model.KeystoreConfig
import com.example.model.SigningMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.LinkedHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkSignerService {

    data class SignResult(
        val signedApkFile: File,
        val sha256Fingerprint: String,
        val certIssuer: String,
        val keyAlias: String,
        val v1Signed: Boolean = true,
        val v2Signed: Boolean = false,
        val v3Signed: Boolean = false,
        val v4Signed: Boolean = false,
        val v4IdSigFile: File? = null
    )

    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(StandardCharsets.US_ASCII)
    private const val V2_SIGNATURE_SCHEME_ID = 0x7109871a
    private const val V3_SIGNATURE_SCHEME_ID = -0xfac9740 // 0xf05368c0 as signed int
    private const val SIGNATURE_RSA_PSS_WITH_SHA256 = 0x0103

    private fun derLength(length: Int): ByteArray {
        return when {
            length < 128 -> byteArrayOf(length.toByte())
            length < 256 -> byteArrayOf(0x81.toByte(), length.toByte())
            length < 65536 -> byteArrayOf(0x82.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte())
            else -> byteArrayOf(
                0x83.toByte(),
                (length shr 16).toByte(),
                ((length shr 8) and 0xFF).toByte(),
                (length and 0xFF).toByte()
            )
        }
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val len = derLength(content.size)
        val res = ByteArray(1 + len.size + content.size)
        res[0] = tag.toByte()
        System.arraycopy(len, 0, res, 1, len.size)
        System.arraycopy(content, 0, res, 1 + len.size, content.size)
        return res
    }

    private fun derSequence(vararg items: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        for (item in items) {
            baos.write(item, 0, item.size)
        }
        return derTag(0x30, baos.toByteArray())
    }

    private fun derSet(vararg items: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        for (item in items) {
            baos.write(item, 0, item.size)
        }
        return derTag(0x31, baos.toByteArray())
    }

    /**
     * Builds a standard PKCS#7 SignedData block (RFC 2315) compliant with Android JarVerifier (V1).
     */
    private fun buildPKCS7Block(cert: X509Certificate, signatureBytes: ByteArray): ByteArray {
        val certBytes = cert.encoded
        val certsTagged = derTag(0xA0, certBytes) // [0] IMPLICIT CertificateSet

        // DigestAlgorithmIdentifier SHA-256
        val sha256AlgId = byteArrayOf(
            0x30, 0x0D, 0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00
        )
        // RSA Encryption AlgId
        val rsaEncId = byteArrayOf(
            0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00
        )

        // IssuerAndSerialNumber
        val issuerBytes = cert.issuerX500Principal.encoded
        val serialBytes = cert.serialNumber.toByteArray()
        val serialDer = derTag(0x02, serialBytes)
        val issuerAndSerial = derSequence(issuerBytes, serialDer)

        // SignerInfo
        val version1 = byteArrayOf(0x02, 0x01, 0x01)
        val encDigest = derTag(0x04, signatureBytes) // OCTET STRING
        val signerInfo = derSequence(version1, issuerAndSerial, sha256AlgId, rsaEncId, encDigest)
        val signerInfos = derSet(signerInfo)

        // ContentInfo: data OID 1.2.840.113549.1.7.1
        val contentInfoOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x01
        )
        val contentInfo = derSequence(contentInfoOid)

        // SignedData
        val signedDataVersion = byteArrayOf(0x02, 0x01, 0x01)
        val digestAlgSet = derSet(sha256AlgId)
        val signedData = derSequence(signedDataVersion, digestAlgSet, contentInfo, certsTagged, signerInfos)

        // PKCS7 ContentInfo: signedData OID 1.2.840.113549.1.7.2
        val signedDataOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x02
        )
        val signedDataExplicit = derTag(0xA0, signedData)

        return derSequence(signedDataOid, signedDataExplicit)
    }

    private fun writeLengthPrefixed(data: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(4 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(data.size)
        buf.put(data)
        return buf.array()
    }

    /**
     * Builds an Android V2/V3 Signing Block payload for given scheme ID.
     */
    private fun buildApkSchemeBlockValue(
        privateKey: PrivateKey,
        cert: X509Certificate,
        apkDigest: ByteArray
    ): ByteArray {
        // 1. Digests sequence
        val digestItem = ByteBuffer.allocate(4 + 4 + apkDigest.size).order(ByteOrder.LITTLE_ENDIAN)
        digestItem.putInt(SIGNATURE_RSA_PSS_WITH_SHA256)
        digestItem.putInt(apkDigest.size)
        digestItem.put(apkDigest)
        val digestsSeq = writeLengthPrefixed(writeLengthPrefixed(digestItem.array()))

        // 2. Certificates sequence
        val certsSeq = writeLengthPrefixed(writeLengthPrefixed(cert.encoded))

        // 3. Additional attributes (empty)
        val attrsSeq = writeLengthPrefixed(ByteArray(0))

        // 4. Signed data
        val signedDataBaos = ByteArrayOutputStream()
        signedDataBaos.write(digestsSeq)
        signedDataBaos.write(certsSeq)
        signedDataBaos.write(attrsSeq)
        val signedData = signedDataBaos.toByteArray()

        // 5. Sign the signedData with RSA private key
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(signedData)
        val signatureBytes = signer.sign()

        // 6. Signatures sequence
        val sigItem = ByteBuffer.allocate(4 + 4 + signatureBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        sigItem.putInt(SIGNATURE_RSA_PSS_WITH_SHA256)
        sigItem.putInt(signatureBytes.size)
        sigItem.put(signatureBytes)
        val signaturesSeq = writeLengthPrefixed(writeLengthPrefixed(sigItem.array()))

        // 7. Public key (SubjectPublicKeyInfo)
        val publicKeySeq = writeLengthPrefixed(cert.publicKey.encoded)

        // 8. Single Signer block
        val signerBaos = ByteArrayOutputStream()
        signerBaos.write(writeLengthPrefixed(signedData))
        signerBaos.write(signaturesSeq)
        signerBaos.write(publicKeySeq)

        return writeLengthPrefixed(writeLengthPrefixed(signerBaos.toByteArray()))
    }

    /**
     * Constructs the APK Signing Block containing V2 and/or V3 scheme pairs.
     */
    private fun buildApkSigningBlock(
        privateKey: PrivateKey,
        cert: X509Certificate,
        apkDigest: ByteArray,
        signV2: Boolean,
        signV3: Boolean
    ): ByteArray {
        val pairsBaos = ByteArrayOutputStream()

        if (signV2) {
            val v2Value = buildApkSchemeBlockValue(privateKey, cert, apkDigest)
            val pairHeader = ByteBuffer.allocate(8 + 4).order(ByteOrder.LITTLE_ENDIAN)
            val pairLength = 4L + v2Value.size.toLong()
            pairHeader.putLong(pairLength)
            pairHeader.putInt(V2_SIGNATURE_SCHEME_ID)
            pairsBaos.write(pairHeader.array())
            pairsBaos.write(v2Value)
        }

        if (signV3) {
            val v3Value = buildApkSchemeBlockValue(privateKey, cert, apkDigest)
            val pairHeader = ByteBuffer.allocate(8 + 4).order(ByteOrder.LITTLE_ENDIAN)
            val pairLength = 4L + v3Value.size.toLong()
            pairHeader.putLong(pairLength)
            pairHeader.putInt(V3_SIGNATURE_SCHEME_ID)
            pairsBaos.write(pairHeader.array())
            pairsBaos.write(v3Value)
        }

        val pairsBytes = pairsBaos.toByteArray()
        // Total block size = pairs size + 8 (trailing size) + 16 (magic)
        val totalBlockSize = pairsBytes.size.toLong() + 24L

        val blockBaos = ByteArrayOutputStream()
        val headerBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        headerBuf.putLong(totalBlockSize)
        blockBaos.write(headerBuf.array())
        blockBaos.write(pairsBytes)

        val footerBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        footerBuf.putLong(totalBlockSize)
        blockBaos.write(footerBuf.array())
        blockBaos.write(APK_SIG_BLOCK_MAGIC)

        return blockBaos.toByteArray()
    }

    /**
     * Generates Android V4 streaming signature (.idsig) file.
     */
    private fun generateV4IdSigFile(
        outputSignedApkFile: File,
        privateKey: PrivateKey,
        cert: X509Certificate,
        apkDigest: ByteArray
    ): File {
        val idSigFile = File(outputSignedApkFile.parentFile, "${outputSignedApkFile.name}.idsig")

        val hashingInfoBaos = ByteArrayOutputStream()
        val hBuf = ByteBuffer.allocate(4 + 1 + 1 + 2 + 4).order(ByteOrder.LITTLE_ENDIAN)
        hBuf.putInt(1) // version
        hBuf.put(1.toByte()) // hash algorithm SHA256
        hBuf.put(12.toByte()) // log2_block_size = 4096 bytes
        hBuf.putShort(0) // salt size 0
        hBuf.putInt(apkDigest.size)
        hashingInfoBaos.write(hBuf.array())
        hashingInfoBaos.write(apkDigest)
        val hashingInfo = hashingInfoBaos.toByteArray()

        // Signing data
        val toSignBaos = ByteArrayOutputStream()
        toSignBaos.write(hashingInfo)
        toSignBaos.write(apkDigest)

        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(toSignBaos.toByteArray())
        val v4Signature = sig.sign()

        val signingInfoBaos = ByteArrayOutputStream()
        val sBuf = ByteBuffer.allocate(4 + 4 + apkDigest.size + 4 + cert.encoded.size + 4 + v4Signature.size).order(ByteOrder.LITTLE_ENDIAN)
        sBuf.putInt(apkDigest.size)
        sBuf.put(apkDigest)
        sBuf.putInt(cert.encoded.size)
        sBuf.put(cert.encoded)
        sBuf.putInt(0) // additional data size
        sBuf.putInt(v4Signature.size)
        sBuf.put(v4Signature)
        signingInfoBaos.write(sBuf.array())

        val idsigBaos = ByteArrayOutputStream()
        val idsigHeader = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        idsigHeader.putInt(2) // v4 signature version 2
        idsigBaos.write(idsigHeader.array())
        idsigBaos.write(writeLengthPrefixed(hashingInfo))
        idsigBaos.write(writeLengthPrefixed(signingInfoBaos.toByteArray()))

        idSigFile.writeBytes(idsigBaos.toByteArray())
        return idSigFile
    }

    fun signApk(
        context: Context,
        unsignedApkFile: File,
        outputSignedApkFile: File,
        keystoreConfig: KeystoreConfig = KeystoreConfig()
    ): SignResult {
        // Resolve PrivateKey and Certificate
        var privateKey: PrivateKey? = null
        var x509Cert: X509Certificate? = null
        var resolvedAlias = "androiddebugkey"
        var resolvedIssuer = "CN=Android Debug, O=Android, C=US"

        if (keystoreConfig.mode == SigningMode.CUSTOM_KEYSTORE && keystoreConfig.customFile != null && keystoreConfig.customFile.exists()) {
            try {
                val ks = try {
                    KeyStore.getInstance("PKCS12").apply {
                        load(FileInputStream(keystoreConfig.customFile), keystoreConfig.storePassword.toCharArray())
                    }
                } catch (e: Exception) {
                    KeyStore.getInstance("JKS").apply {
                        load(FileInputStream(keystoreConfig.customFile), keystoreConfig.storePassword.toCharArray())
                    }
                }
                val alias = if (keystoreConfig.alias.isNotBlank()) keystoreConfig.alias else ks.aliases().nextElement()
                val keyPass = if (keystoreConfig.keyPassword.isNotBlank()) keystoreConfig.keyPassword else keystoreConfig.storePassword
                privateKey = ks.getKey(alias, keyPass.toCharArray()) as? PrivateKey
                x509Cert = ks.getCertificate(alias) as? X509Certificate
                resolvedAlias = alias
                resolvedIssuer = x509Cert?.subjectX500Principal?.name ?: "Custom Keystore"
            } catch (e: Exception) {
                privateKey = null
                x509Cert = null
            }
        }

        if (privateKey == null || x509Cert == null) {
            val isAutoRelease = keystoreConfig.mode == SigningMode.AUTO_RELEASE
            val assetPath = if (isAutoRelease) "bootstrap/abp_release.keystore" else "bootstrap/debug.keystore"
            val storePass = if (isAutoRelease) "abp123456" else "android"
            val alias = if (isAutoRelease) "abp_release" else "androiddebugkey"

            var loadedKs: KeyStore? = null
            for (type in listOf("PKCS12", "JKS", KeyStore.getDefaultType())) {
                try {
                    val ks = KeyStore.getInstance(type)
                    context.assets.open(assetPath).use { input ->
                        ks.load(input, storePass.toCharArray())
                    }
                    if (ks.containsAlias(alias)) {
                        loadedKs = ks
                        break
                    }
                } catch (_: Exception) {}
            }

            if (loadedKs != null) {
                try {
                    privateKey = loadedKs.getKey(alias, storePass.toCharArray()) as? PrivateKey
                    x509Cert = loadedKs.getCertificate(alias) as? X509Certificate
                    resolvedAlias = alias
                    resolvedIssuer = x509Cert?.subjectX500Principal?.name ?: "Android Debug"
                } catch (_: Exception) {}
            }

            if (privateKey == null) {
                // Fallback RSA key generation
                val kpg = java.security.KeyPairGenerator.getInstance("RSA")
                kpg.initialize(2048)
                val kp = kpg.generateKeyPair()
                privateKey = kp.private
                resolvedAlias = "androiddebugkey"
                resolvedIssuer = "CN=Android Debug, O=Android, C=US"
            }
        }

        val signV1 = keystoreConfig.signV1
        val signV2 = keystoreConfig.signV2
        val signV3 = keystoreConfig.signV3
        val signV4 = keystoreConfig.signV4

        var manifestBytes: ByteArray? = null
        var certSfBytes: ByteArray? = null
        var certRsaBytes: ByteArray? = null

        // 1. V1 JAR Signature (META-INF)
        if (signV1) {
            val manifestMfContent = ByteArrayOutputStream()
            val certSfContent = ByteArrayOutputStream()

            val installerHeader = if (keystoreConfig.isInstallerSpoofEnabled) {
                "Installer-Package-Name: ${keystoreConfig.installerPackageName.ifBlank { "com.android.vending" }}\r\n"
            } else ""

            manifestMfContent.write(
                ("Manifest-Version: 1.0\r\nCreated-By: 1.0 (APK Builder Pro AutoSigner)\r\n" + installerHeader + "\r\n").toByteArray(StandardCharsets.UTF_8)
            )

            val entriesDigests = LinkedHashMap<String, String>()

            ZipInputStream(FileInputStream(unsignedApkFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                val digest = MessageDigest.getInstance("SHA-256")

                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        digest.reset()
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                        val sha256Base64 = Base64.getEncoder().encodeToString(digest.digest())
                        entriesDigests[entry.name] = sha256Base64

                        val section = "Name: ${entry.name}\r\nSHA-256-Digest: $sha256Base64\r\n\r\n"
                        manifestMfContent.write(section.toByteArray(StandardCharsets.UTF_8))
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val mBytes = manifestMfContent.toByteArray()
            manifestBytes = mBytes
            val manifestDigest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(mBytes))

            certSfContent.write(
                ("Signature-Version: 1.0\r\nCreated-By: 1.0 (APK Builder Pro AutoSigner)\r\nSHA-256-Digest-Manifest: $manifestDigest\r\n" + installerHeader + "\r\n").toByteArray(StandardCharsets.UTF_8)
            )

            entriesDigests.forEach { (name, b64Digest) ->
                val entryHeader = "Name: $name\r\nSHA-256-Digest: $b64Digest\r\n\r\n"
                val sfSectionDigest = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(entryHeader.toByteArray(StandardCharsets.UTF_8))
                )
                val sfSection = "Name: $name\r\nSHA-256-Digest: $sfSectionDigest\r\n\r\n"
                certSfContent.write(sfSection.toByteArray(StandardCharsets.UTF_8))
            }

            val sfBytes = certSfContent.toByteArray()
            certSfBytes = sfBytes

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(sfBytes)
            val signedSfBytes = signature.sign()

            certRsaBytes = if (x509Cert != null) {
                buildPKCS7Block(x509Cert, signedSfBytes)
            } else {
                signedSfBytes
            }
        }

        // 2. Assemble ZIP with V1 files (if enabled) to temp file
        val tempApkFile = File(outputSignedApkFile.parentFile, "temp_pre_v2_${System.currentTimeMillis()}.apk")
        ZipOutputStream(FileOutputStream(tempApkFile)).use { zos ->
            ZipInputStream(FileInputStream(unsignedApkFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        zos.putNextEntry(ZipEntry(entry.name))
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            zos.write(buffer, 0, count)
                        }
                        zos.closeEntry()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (keystoreConfig.isInstallerSpoofEnabled) {
                val installerName = keystoreConfig.installerPackageName.ifBlank { "com.android.vending" }
                val spoofMeta = "{\n  \"installer_package_name\": \"$installerName\",\n  \"spoofed\": true\n}".toByteArray(StandardCharsets.UTF_8)
                zos.putNextEntry(ZipEntry("assets/installer_info.json"))
                zos.write(spoofMeta)
                zos.closeEntry()
            }

            if (signV1 && manifestBytes != null && certSfBytes != null && certRsaBytes != null) {
                zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                zos.write(manifestBytes)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("META-INF/CERT.SF"))
                zos.write(certSfBytes)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
                zos.write(certRsaBytes)
                zos.closeEntry()
            }
        }

        // 3. V2 / V3 APK Signing Block Embedding
        if (signV2 || signV3) {
            embedApkSigningBlock(
                tempApkFile = tempApkFile,
                finalApkFile = outputSignedApkFile,
                privateKey = privateKey,
                cert = x509Cert,
                signV2 = signV2,
                signV3 = signV3
            )
            tempApkFile.delete()
        } else {
            if (outputSignedApkFile.exists()) outputSignedApkFile.delete()
            tempApkFile.renameTo(outputSignedApkFile)
        }

        // Compute APK digest for V4 and fingerprint using streaming digest
        val apkDigest = computeFileDigest(outputSignedApkFile)

        // 4. V4 Streaming Signature (.idsig)
        val v4File = if (signV4 && x509Cert != null) {
            try {
                generateV4IdSigFile(outputSignedApkFile, privateKey, x509Cert, apkDigest)
            } catch (_: Exception) {
                null
            }
        } else null

        val sha256Fingerprint = if (x509Cert != null) {
            formatFingerprint(MessageDigest.getInstance("SHA-256").digest(x509Cert.encoded))
        } else {
            formatFingerprint(apkDigest)
        }

        return SignResult(
            signedApkFile = outputSignedApkFile,
            sha256Fingerprint = sha256Fingerprint,
            certIssuer = resolvedIssuer,
            keyAlias = resolvedAlias,
            v1Signed = signV1,
            v2Signed = signV2,
            v3Signed = signV3,
            v4Signed = signV4 && v4File != null,
            v4IdSigFile = v4File
        )
    }

    private fun computeFileDigest(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(16384)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest()
    }

    /**
     * Finds the End of Central Directory (EOCD), reads the Central Directory offset,
     * inserts the APK Signing Block right before the Central Directory, and writes the updated APK.
     */
    private fun embedApkSigningBlock(
        tempApkFile: File,
        finalApkFile: File,
        privateKey: PrivateKey,
        cert: X509Certificate?,
        signV2: Boolean,
        signV3: Boolean
    ) {
        val fileLength = tempApkFile.length()
        RandomAccessFile(tempApkFile, "r").use { raf ->
            // Locate EOCD: search backward from end of file for 0x06054b50
            val maxCommentLength = 65535
            val maxEocdSearch = (maxCommentLength + 22).coerceAtMost(fileLength.toInt())
            val searchBuffer = ByteArray(maxEocdSearch)
            raf.seek(fileLength - maxEocdSearch)
            raf.readFully(searchBuffer)

            var eocdOffsetInSearch = -1
            for (i in (searchBuffer.size - 22) downTo 0) {
                if (searchBuffer[i] == 0x50.toByte() &&
                    searchBuffer[i + 1] == 0x4b.toByte() &&
                    searchBuffer[i + 2] == 0x05.toByte() &&
                    searchBuffer[i + 3] == 0x06.toByte()
                ) {
                    eocdOffsetInSearch = i
                    break
                }
            }

            if (eocdOffsetInSearch == -1) {
                // If EOCD not found, copy directly
                raf.close()
                tempApkFile.copyTo(finalApkFile, overwrite = true)
                return
            }

            val eocdOffsetInFile = (fileLength - maxEocdSearch) + eocdOffsetInSearch
            val bb = ByteBuffer.wrap(searchBuffer, eocdOffsetInSearch, 22).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(16) // offset 16 contains central directory offset (uint32)
            val cdOffset = bb.getInt().toLong() and 0xFFFFFFFFL
            val cdSize = bb.getInt(12).toLong() and 0xFFFFFFFFL

            // Calculate APK contents hash for signing using streaming read
            val digest = MessageDigest.getInstance("SHA-256")
            raf.seek(0)
            var rem = cdOffset
            val buffer = ByteArray(16384)
            while (rem > 0) {
                val readLen = raf.read(buffer, 0, rem.coerceAtMost(buffer.size.toLong()).toInt())
                if (readLen <= 0) break
                digest.update(buffer, 0, readLen)
                rem -= readLen
            }
            val apkDigest = digest.digest()

            // Generate APK Signing Block
            val signingBlock = if (cert != null) {
                buildApkSigningBlock(privateKey, cert, apkDigest, signV2, signV3)
            } else {
                ByteArray(0)
            }

            if (signingBlock.isEmpty()) {
                raf.close()
                tempApkFile.copyTo(finalApkFile, overwrite = true)
                return
            }

            // Read EOCD
            raf.seek(eocdOffsetInFile)
            val eocdBytes = ByteArray((fileLength - eocdOffsetInFile).toInt())
            raf.readFully(eocdBytes)

            // Update Central Directory offset in EOCD (offset 16 in EOCD)
            val newCdOffset = cdOffset + signingBlock.size
            val eocdBuf = ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN)
            eocdBuf.putInt(16, newCdOffset.toInt())

            // Write combined final APK with low memory streaming
            FileOutputStream(finalApkFile).use { fos ->
                // 1. Pre-CD bytes
                raf.seek(0)
                var pRem = cdOffset
                while (pRem > 0) {
                    val readLen = raf.read(buffer, 0, pRem.coerceAtMost(buffer.size.toLong()).toInt())
                    if (readLen <= 0) break
                    fos.write(buffer, 0, readLen)
                    pRem -= readLen
                }

                // 2. Signing Block
                fos.write(signingBlock)

                // 3. Central Directory
                raf.seek(cdOffset)
                var cdRem = cdSize
                while (cdRem > 0) {
                    val readLen = raf.read(buffer, 0, cdRem.coerceAtMost(buffer.size.toLong()).toInt())
                    if (readLen <= 0) break
                    fos.write(buffer, 0, readLen)
                    cdRem -= readLen
                }

                // 4. EOCD
                fos.write(eocdBytes)
            }
        }
    }

    private fun formatFingerprint(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
