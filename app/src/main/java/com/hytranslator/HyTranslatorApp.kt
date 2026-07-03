package com.hytranslator

import android.app.Application

class HyTranslatorApp : Application() {
    lateinit var engine: com.hytranslator.translator.HyMTEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        engine = com.hytranslator.translator.HyMTEngine(this)
    }

    companion object {
        lateinit var instance: HyTranslatorApp
            private set
    }
}
