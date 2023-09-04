package com.nibav.dialer

import android.app.Application
import com.nibav.commons.extensions.checkUseEnglish

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}
