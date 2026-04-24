package com.example.scheduleapp

import android.app.Application
import com.example.scheduleapp.data.AuthSessionManager
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthSessionManager.initialize(this)
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}
