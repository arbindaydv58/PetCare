package com.example.petcare

import android.app.Application
import com.example.petcare.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class PetCareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            try {
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        NotificationHelper.createNotificationChannel(this)
    }
}
