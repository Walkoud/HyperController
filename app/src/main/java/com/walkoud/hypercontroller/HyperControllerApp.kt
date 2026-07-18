package com.walkoud.hypercontroller

import android.app.Application
import android.util.Log
import com.walkoud.hypercontroller.core.root.Sqlite3Manager

class HyperControllerApp : Application() {

    lateinit var sqlite3Manager: Sqlite3Manager
        private set

    override fun onCreate() {
        super.onCreate()
        sqlite3Manager = Sqlite3Manager(this)
        if (!sqlite3Manager.isDeployed()) {
            val result = sqlite3Manager.deploy()
            Log.d("HyperCtrl", "sqlite3 deploy: success=${result.success} path=${result.pathOrError}")
        } else {
            Log.d("HyperCtrl", "sqlite3 already deployed at ${sqlite3Manager.getPath()}")
        }
    }

    /** Returns the deployed sqlite3 path, or "sqlite3" as fallback */
    val sqlite3Path: String
        get() = if (sqlite3Manager.isDeployed()) sqlite3Manager.getPath() else "sqlite3"
}
