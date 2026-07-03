package com.hytranslator

import android.app.Application

class HyTranslatorApp : Application() {
    lateinit var engine: translator.HyMTEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        engine = translator.HyMTEngine(this)
    }

    companion object {
        lateinit var instance: HyTranslatorApp
            private set
    }
}
