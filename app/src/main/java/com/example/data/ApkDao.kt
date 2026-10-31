package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApkDao {
    @Query("SELECT * FROM converted_apks ORDER BY buildTimestamp DESC")
    fun getAllConvertedApks(): Flow<List<ConvertedApkEntity>>

    @Query("SELECT * FROM converted_apks WHERE id = :id")
    suspend fun getApkById(id: Long): ConvertedApkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApk(apk: ConvertedApkEntity): Long

    @Query("DELETE FROM converted_apks WHERE id = :id")
    suspend fun deleteApkById(id: Long)

    @Query("DELETE FROM converted_apks")
    suspend fun clearAllHistory()
}
