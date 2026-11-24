package com.example.floworbit.data.profile

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProfilePicture::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun profilePictureDao(): ProfilePictureDao

    companion object {
        @Volatile private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase = 
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_data.db" // A separate database file
                ).build().also { INSTANCE = it }
            }
    }
}