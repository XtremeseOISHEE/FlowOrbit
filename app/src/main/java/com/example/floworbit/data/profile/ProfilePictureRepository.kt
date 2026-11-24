package com.example.floworbit.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class ProfilePictureRepository(private val context: Context) {

    private val profilePictureDao = UserDatabase.getInstance(context).profilePictureDao()

    suspend fun getProfilePicture(userId: String): Bitmap? {
        val profilePicture = profilePictureDao.getProfilePicture(userId)
        return profilePicture?.imagePath?.let { loadBitmapFromPath(it) }
    }

    suspend fun saveProfilePicture(userId: String, bitmap: Bitmap) {
        val imagePath = saveBitmapToInternalStorage(userId, bitmap)
        val profilePicture = ProfilePicture(userId = userId, imagePath = imagePath)
        profilePictureDao.insert(profilePicture)
    }

    private fun saveBitmapToInternalStorage(userId: String, bitmap: Bitmap): String {
        val directory = context.getDir("profile_images", Context.MODE_PRIVATE)
        val file = File(directory, "$userId.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file.absolutePath
    }

    private fun loadBitmapFromPath(path: String): Bitmap? {
        return BitmapFactory.decodeFile(path)
    }
}
