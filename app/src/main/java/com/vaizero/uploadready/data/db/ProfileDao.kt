package com.vaizero.uploadready.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaizero.uploadready.data.model.AppProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isPreset DESC, createdAt DESC")
    fun getAllProfiles(): Flow<List<AppProfile>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): AppProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AppProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<AppProfile>)

    @Update
    suspend fun updateProfile(profile: AppProfile)

    @Delete
    suspend fun deleteProfile(profile: AppProfile)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int
}
