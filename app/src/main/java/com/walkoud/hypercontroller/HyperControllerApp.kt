package com.walkoud.hypercontroller

import android.app.Application
import com.walkoud.hypercontroller.core.root.Sqlite3Manager

class HyperControllerApp : Application() {

    lateinit var sqlite3Manager: Sqlite3Manager
        private set

    override fun onCreate() {
        super.onCreate()
        sqlite3Manager = Sqlite3Manager(this)

        if (sqlite3Manager.isDeployed()) {
            // Binary already deployed
        }
    }
}
