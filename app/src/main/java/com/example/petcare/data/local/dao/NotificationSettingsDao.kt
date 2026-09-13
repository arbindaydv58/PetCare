package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.NotificationSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: NotificationSettingsEntity): Long

    @Update
    suspend fun updateSettings(settings: NotificationSettingsEntity)

    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    fun observeSettings(userId: String): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettings(userId: String): NotificationSettingsEntity?
}
