package com.example.engine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object BinaryXmlHelper {

    /**
     * Modifies package name, app label, and version name in a compiled Android Binary XML template.
     * Android's PackageParser requires AndroidManifest.xml to be in valid binary XML format (magic 0x00080003).
     */
    fun customizeBinaryManifest(
        templateBytes: ByteArray,
        newPackageName: String,
        newAppName: String,
        newVersionName: String
    ): ByteArray {
        try {
            val buf = ByteBuffer.wrap(templateBytes).order(ByteOrder.LITTLE_ENDIAN)

            val rootChunkType = buf.short.toInt() and 0xFFFF
            val rootHeaderSize = buf.short.toInt() and 0xFFFF
            val rootChunkSize = buf.int

            if (rootChunkType != 0x0003) {
                // Not binary XML, return original
                return templateBytes
            }

            // String pool starts at offset 8
            val spType = buf.short.toInt() and 0xFFFF
            val spHeaderSize = buf.short.toInt() and 0xFFFF
            val spChunkSize = buf.int
            val stringCount = buf.int
            val styleCount = buf.int
            val flags = buf.int
            val stringsStart = buf.int
            val stylesStart = buf.int

            if (spType != 0x0001 || stringCount <= 0) {
                return templateBytes
            }

            val isUtf8 = (flags and (1 shl 8)) != 0

            // Read string offsets
            val offsets = IntArray(stringCount)
            for (i in 0 until stringCount) {
                offsets[i] = buf.int
            }

            // Extract strings
            val strings = mutableListOf<String>()
            val strDataStartPos = 8 + stringsStart

            for (i in 0 until stringCount) {
                val p = strDataStartPos + offsets[i]
                if (p >= templateBytes.size) {
                    strings.add("")
                    continue
                }

                if (isUtf8) {
                    var cur = p
                    val len1 = templateBytes[cur].toInt() and 0xFF
                    cur++
                    val len2 = templateBytes[cur].toInt() and 0xFF
                    cur++
                    val strBytes = templateBytes.copyOfRange(cur, cur + len2)
                    strings.add(String(strBytes, StandardCharsets.UTF_8))
                } else {
                    val u16len = (templateBytes[p].toInt() and 0xFF) or ((templateBytes[p + 1].toInt() and 0xFF) shl 8)
                    val strBytes = templateBytes.copyOfRange(p + 2, p + 2 + u16len * 2)
                    strings.add(String(strBytes, StandardCharsets.UTF_16LE))
                }
            }

            // Replace strings
            for (i in strings.indices) {
                val s = strings[i]
                when {
                    s == "com.aistudio.apkbuilder" -> strings[i] = newPackageName
                    s == "APK Builder Pro" -> strings[i] = newAppName
                    s == "1.0.0" -> strings[i] = newVersionName
                }
            }

            // Rebuild string pool data (UTF-16LE)
            val newStrData = ByteArrayOutputStream()
            val newOffsets = IntArray(stringCount)

            for (i in 0 until stringCount) {
                newOffsets[i] = newStrData.size()
                val s = strings[i]
                val utf16Bytes = s.toByteArray(StandardCharsets.UTF_16LE)
                val len = s.length
                newStrData.write(len and 0xFF)
                newStrData.write((len shr 8) and 0xFF)
                newStrData.write(utf16Bytes)
                newStrData.write(0)
                newStrData.write(0) // Null terminator
            }

            // Pad string data to 4-byte boundary
            val pad = (4 - (newStrData.size() % 4)) % 4
            for (p in 0 until pad) {
                newStrData.write(0)
            }

            val newStrDataBytes = newStrData.toByteArray()
            val newSpChunkSize = 28 + (stringCount * 4) + (styleCount * 4) + newStrDataBytes.size

            // Rest of XML after old string pool
            val restStart = 8 + spChunkSize
            val restData = if (restStart < templateBytes.size) {
                templateBytes.copyOfRange(restStart, templateBytes.size)
            } else {
                ByteArray(0)
            }

            val newTotalSize = 8 + newSpChunkSize + restData.size

            val result = ByteBuffer.allocate(newTotalSize).order(ByteOrder.LITTLE_ENDIAN)

            // Root header
            result.putShort(rootChunkType.toShort())
            result.putShort(rootHeaderSize.toShort())
            result.putInt(newTotalSize)

            // String pool header
            result.putShort(spType.toShort())
            result.putShort(spHeaderSize.toShort())
            result.putInt(newSpChunkSize)
            result.putInt(stringCount)
            result.putInt(styleCount)
            result.putInt(0) // UTF-16LE
            result.putInt(28 + (stringCount * 4) + (styleCount * 4))
            result.putInt(0)

            // String offsets
            for (off in newOffsets) {
                result.putInt(off)
            }

            // String data
            result.put(newStrDataBytes)

            // Rest of XML
            result.put(restData)

            return result.array()
        } catch (e: Exception) {
            // Fallback to template if error
            return templateBytes
        }
    }
}
