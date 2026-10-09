package com.consay.app

import android.app.Application
import com.consay.app.data.SessionRepository

class ConSayApplication : Application() {
    lateinit var sessionRepository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionRepository = SessionRepository(this)
    }

    companion object {
        lateinit var instance: ConSayApplication
            private set
    }
}
