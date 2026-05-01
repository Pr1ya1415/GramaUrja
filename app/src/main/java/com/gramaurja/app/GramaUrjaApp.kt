package com.gramaurja.app

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class GramaUrjaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Disk Persistence for Offline Graceful Handling
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}