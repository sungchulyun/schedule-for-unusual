package com.example.scheduleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.scheduleapp.data.AuthSession
import com.example.scheduleapp.data.AuthSessionManager
import com.example.scheduleapp.data.CalendarRepository
import com.example.scheduleapp.ui.auth.LoginScreen
import com.example.scheduleapp.ui.calendar.CalendarScreen
import com.example.scheduleapp.ui.theme.ScheduleAppTheme
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthSessionManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            var session by remember { mutableStateOf(AuthSessionManager.getSession()) }
            var loginErrorMessage by remember { mutableStateOf<String?>(null) }

            ScheduleAppTheme {
                LaunchedEffect(session?.currentUserId, session?.partnerUserId) {
                    if (session != null && session?.partnerUserId == null) {
                        runCatching { CalendarRepository().refreshPartnerUserId() }
                        session = AuthSessionManager.getSession()
                    }
                }

                if (session == null) {
                    LoginScreen(
                        errorMessage = loginErrorMessage,
                        onLoginClick = {
                            loginErrorMessage = null
                            startNativeKakaoLogin(
                                onSuccess = { newSession ->
                                    AuthSessionManager.saveSession(newSession)
                                    session = newSession
                                    loginErrorMessage = null
                                    lifecycleScope.launch {
                                        runCatching { CalendarRepository().refreshPartnerUserId() }
                                        session = AuthSessionManager.getSession()
                                    }
                                },
                                onFailure = { message ->
                                    loginErrorMessage = message
                                }
                            )
                        }
                    )
                } else {
                    CalendarScreen()
                }
            }
        }
    }

    private fun startNativeKakaoLogin(
        onSuccess: (AuthSession) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            onFailure("`local.properties`에 `kakao.native.app.key`를 설정해 주세요.")
            return
        }

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> onFailure(error.message ?: "카카오 로그인에 실패했습니다.")
                token != null -> {
                    lifecycleScope.launch {
                        runCatching {
                            CalendarRepository().authenticateWithKakaoAccessToken(token.accessToken)
                        }.onSuccess {
                            onSuccess(it)
                        }.onFailure {
                            onFailure(it.message ?: "서비스 로그인에 실패했습니다.")
                        }
                    }
                }
                else -> onFailure("카카오 로그인 결과가 비어 있습니다.")
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        onFailure("카카오 로그인이 취소되었습니다.")
                        return@loginWithKakaoTalk
                    }
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    callback(token, null)
                } else {
                    onFailure("카카오 로그인 결과가 비어 있습니다.")
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }
}
