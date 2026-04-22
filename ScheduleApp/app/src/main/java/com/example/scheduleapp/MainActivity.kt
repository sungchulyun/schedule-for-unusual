package com.example.scheduleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import com.example.scheduleapp.data.remote.CreateInviteResponse
import com.example.scheduleapp.data.remote.InviteLookupResponse
import com.example.scheduleapp.ui.auth.LoginScreen
import com.example.scheduleapp.ui.calendar.CalendarScreen
import com.example.scheduleapp.ui.invite.InviteAcceptScreen
import com.example.scheduleapp.ui.theme.ScheduleAppTheme
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var latestInviteToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthSessionManager.initialize(applicationContext)
        captureInviteToken(intent?.data)
        enableEdgeToEdge()

        setContent {
            var session by remember { mutableStateOf(AuthSessionManager.getSession()) }
            var loginErrorMessage by remember { mutableStateOf<String?>(null) }
            var pendingInviteToken by remember { mutableStateOf(AuthSessionManager.getPendingInviteToken()) }
            var inviteDetail by remember { mutableStateOf<InviteLookupResponse?>(null) }
            var inviteLoading by remember { mutableStateOf(false) }
            var inviteSubmitting by remember { mutableStateOf(false) }
            var inviteErrorMessage by remember { mutableStateOf<String?>(null) }

            ScheduleAppTheme {
                LaunchedEffect(latestInviteToken) {
                    val token = latestInviteToken?.trim().orEmpty()
                    if (token.isNotBlank()) {
                        pendingInviteToken = token
                    }
                }

                LaunchedEffect(session?.currentUserId, session?.partnerUserId) {
                    if (session != null && session?.partnerUserId == null) {
                        runCatching { CalendarRepository().refreshPartnerUserId() }
                        session = AuthSessionManager.getSession()
                    }
                }

                LaunchedEffect(session?.currentUserId, pendingInviteToken) {
                    val token = pendingInviteToken?.trim().orEmpty()
                    if (session == null || token.isBlank()) {
                        inviteDetail = null
                        inviteLoading = false
                        inviteSubmitting = false
                        inviteErrorMessage = null
                        return@LaunchedEffect
                    }

                    inviteLoading = true
                    inviteErrorMessage = null
                    runCatching { CalendarRepository().getInvite(token) }
                        .onSuccess { inviteDetail = it }
                        .onFailure {
                            inviteDetail = null
                            inviteErrorMessage = it.message ?: "초대 정보를 불러오지 못했습니다."
                        }
                    inviteLoading = false
                }

                when {
                    session == null -> {
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
                    }

                    !pendingInviteToken.isNullOrBlank() -> {
                        InviteAcceptScreen(
                            invite = inviteDetail,
                            isLoading = inviteLoading,
                            isSubmitting = inviteSubmitting,
                            errorMessage = inviteErrorMessage,
                            onAccept = {
                                val token = pendingInviteToken ?: return@InviteAcceptScreen
                                inviteSubmitting = true
                                inviteErrorMessage = null
                                lifecycleScope.launch {
                                    runCatching {
                                        CalendarRepository().acceptInvite(token)
                                        CalendarRepository().refreshPartnerUserId()
                                    }.onSuccess {
                                        AuthSessionManager.clearPendingInviteToken()
                                        latestInviteToken = null
                                        pendingInviteToken = null
                                        inviteDetail = null
                                        session = AuthSessionManager.getSession()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "파트너 연결이 완료되었습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure {
                                        inviteErrorMessage = it.message ?: "초대 수락에 실패했습니다."
                                    }
                                    inviteSubmitting = false
                                }
                            },
                            onDismiss = {
                                AuthSessionManager.clearPendingInviteToken()
                                latestInviteToken = null
                                pendingInviteToken = null
                                inviteDetail = null
                                inviteErrorMessage = null
                            }
                        )
                    }

                    else -> {
                        CalendarScreen(
                            showPartnerInviteAction = session?.partnerUserId == null,
                            onInvitePartner = {
                                lifecycleScope.launch {
                                    runCatching {
                                        val invite = CalendarRepository().createInvite()
                                        shareInvite(invite, session?.nickname)
                                    }.onFailure {
                                        Toast.makeText(
                                            this@MainActivity,
                                            it.message ?: "파트너 초대를 준비하지 못했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureInviteToken(intent.data)
    }

    private fun captureInviteToken(uri: Uri?) {
        val inviteToken = uri?.getQueryParameter("inviteToken")
            ?.trim()
            ?.ifBlank { null }
            ?: return

        AuthSessionManager.savePendingInviteToken(inviteToken)
        latestInviteToken = inviteToken
    }

    private fun shareInvite(
        invite: CreateInviteResponse,
        inviterNickname: String?
    ) {
        val title = "${inviterNickname?.ifBlank { null } ?: "파트너"}님이 ScheduleApp 초대를 보냈어요."
        val message = buildString {
            appendLine(title)
            appendLine("아래 링크에서 초대를 수락하면 일정과 근무 스케줄을 함께 관리할 수 있습니다.")
            appendLine(invite.shareUrl)
            appendLine()
            append("앱에서 바로 열기: ${invite.deepLink}")
        }

        val kakaoTalkIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.kakao.talk"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        if (kakaoTalkIntent.resolveActivity(packageManager) != null) {
            startActivity(kakaoTalkIntent)
            return
        }

        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "초대 링크 공유"
        )
        startActivity(chooserIntent)
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
