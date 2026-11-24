package com.example.floworbit.data.profile

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "profile_pictures")
data class ProfilePicture(
    @PrimaryKey val userId: String,
    val imagePath: String
)

@Dao
interface ProfilePictureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profilePicture: ProfilePicture)

    @Query("SELECT * FROM profile_pictures WHERE userId = :userId")
    suspend fun getProfilePicture(userId: String): ProfilePicture?
}
