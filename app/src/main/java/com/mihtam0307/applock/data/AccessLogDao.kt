package com.mihtam0307.applock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AccessLogDao {

    @Insert
    suspend fun insert(log: AccessLogEntity)

    @Query("SELECT * FROM access_log ORDER BY timestamp DESC")
    suspend fun getAll(): List<AccessLogEntity>

    @Query("SELECT * FROM access_log WHERE packageName = :packageName ORDER BY timestamp DESC")
    suspend fun getForApp(packageName: String): List<AccessLogEntity>

    @Query("DELETE FROM access_log")
    suspend fun clearAll()
}
