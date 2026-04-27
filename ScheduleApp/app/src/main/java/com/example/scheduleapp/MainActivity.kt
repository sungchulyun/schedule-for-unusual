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
import com.example.scheduleapp.data.CalendarApiConfig
import com.example.scheduleapp.data.AuthSessionManager
import com.example.scheduleapp.data.CalendarRepository
import com.example.scheduleapp.data.remote.CreateInviteResponse
import com.example.scheduleapp.data.remote.InviteLookupResponse
import com.example.scheduleapp.ui.auth.LoginScreen
import com.example.scheduleapp.ui.calendar.CalendarScreen
import com.example.scheduleapp.ui.invite.InviteAcceptScreen
import com.example.scheduleapp.ui.theme.ScheduleAppTheme
import com.example.scheduleapp.widget.ScheduleMonthWidgetContract
import com.example.scheduleapp.widget.ScheduleMonthWidgetProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val repository by lazy { CalendarRepository() }
    private var latestInviteToken by mutableStateOf<String?>(null)
    private var latestLoginCode by mutableStateOf<String?>(null)
    private var latestLoginErrorMessage by mutableStateOf<String?>(null)
    private var latestWidgetDate by mutableStateOf<LocalDate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthSessionManager.initialize(applicationContext)
        captureIncomingIntent(intent)
        enableEdgeToEdge()

        setContent {
            var session by remember { mutableStateOf(AuthSessionManager.getSession()) }
            var loginErrorMessage by remember { mutableStateOf<String?>(latestLoginErrorMessage) }
            var pendingInviteToken by remember { mutableStateOf(AuthSessionManager.getPendingInviteToken()) }
            var widgetOpenDate by remember { mutableStateOf(latestWidgetDate) }
            var partnerConnectionCheckedUserId by remember {
                mutableStateOf(session?.takeIf { it.partnerUserId != null }?.currentUserId)
            }
            var inviteDetail by remember { mutableStateOf<InviteLookupResponse?>(null) }
            var inviteLoading by remember { mutableStateOf(false) }
            var inviteSubmitting by remember { mutableStateOf(false) }
            var inviteErrorMessage by remember { mutableStateOf<String?>(null) }

            fun clearInviteUiState() {
                AuthSessionManager.clearPendingInviteToken()
                latestInviteToken = null
                pendingInviteToken = null
                inviteDetail = null
                inviteErrorMessage = null
                inviteLoading = false
                inviteSubmitting = false
            }

            ScheduleAppTheme {
                LaunchedEffect(latestInviteToken) {
                    val token = latestInviteToken?.trim().orEmpty()
                    if (token.isNotBlank()) {
                        pendingInviteToken = token
                    }
                }

                LaunchedEffect(latestLoginErrorMessage) {
                    if (!latestLoginErrorMessage.isNullOrBlank()) {
                        loginErrorMessage = latestLoginErrorMessage
                    }
                }

                LaunchedEffect(latestWidgetDate) {
                    if (latestWidgetDate != null) {
                        widgetOpenDate = latestWidgetDate
                    }
                }

                LaunchedEffect(session?.currentUserId, latestLoginCode) {
                    val loginCode = latestLoginCode?.trim().orEmpty()
                    if (session != null || loginCode.isBlank()) {
                        return@LaunchedEffect
                    }

                    loginErrorMessage = null
                    runCatching { repository.exchangeMobileLogin(loginCode) }
                        .onSuccess { newSession ->
                            AuthSessionManager.saveSession(newSession)
                            session = newSession
                            latestLoginCode = null
                            latestLoginErrorMessage = null
                            lifecycleScope.launch {
                                runCatching { repository.refreshPartnerUserId() }
                                session = AuthSessionManager.getSession()
                            }
                        }
                        .onFailure {
                            latestLoginCode = null
                            latestLoginErrorMessage = it.message ?: "브라우저 로그인 교환에 실패했습니다."
                            loginErrorMessage = latestLoginErrorMessage
                        }
                }

                LaunchedEffect(session?.currentUserId, session?.partnerUserId) {
                    val currentSession = session
                    when {
                        currentSession == null -> {
                            partnerConnectionCheckedUserId = null
                        }
                        currentSession.partnerUserId != null -> {
                            partnerConnectionCheckedUserId = currentSession.currentUserId
                        }
                        else -> {
                            runCatching { repository.refreshPartnerUserId() }
                            session = AuthSessionManager.getSession()
                            partnerConnectionCheckedUserId = currentSession.currentUserId
                        }
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
                    runCatching { repository.getInvite(token) }
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
                                            runCatching { repository.refreshPartnerUserId() }
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
                                        repository.acceptInvite(token)
                                        repository.refreshPartnerUserId()
                                    }.onSuccess {
                                        clearInviteUiState()
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
                                clearInviteUiState()
                            }
                        )
                    }

                    else -> {
                        CalendarScreen(
                            requestedOpenDate = widgetOpenDate,
                            onRequestedOpenDateConsumed = {
                                widgetOpenDate = null
                                latestWidgetDate = null
                            },
                            showPartnerInviteAction =
                                session?.partnerUserId == null &&
                                    partnerConnectionCheckedUserId == session?.currentUserId,
                            onInvitePartner = {
                                lifecycleScope.launch {
                                    runCatching {
                                        val invite = repository.createInvite()
                                        shareInvite(invite, session?.nickname)
                                    }.onFailure {
                                        Toast.makeText(
                                            this@MainActivity,
                                            it.message ?: "파트너 초대를 준비하지 못했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onCalendarDataChanged = {
                                ScheduleMonthWidgetProvider.requestRefresh(applicationContext)
                            },
                            onLogout = {
                                lifecycleScope.launch {
                                    runCatching { repository.logout() }
                                    session = AuthSessionManager.getSession()
                                    clearInviteUiState()
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
        captureIncomingIntent(intent)
    }

    private fun captureIncomingIntent(intent: Intent?) {
        captureIncomingLink(intent?.data)
        latestWidgetDate = intent?.getStringExtra(ScheduleMonthWidgetContract.ExtraDate)
            ?.trim()
            ?.ifBlank { null }
            ?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            } ?: latestWidgetDate
    }

    private fun captureIncomingLink(uri: Uri?) {
        val inviteToken = uri?.extractInviteToken()
        if (inviteToken != null) {
            AuthSessionManager.savePendingInviteToken(inviteToken)
            latestInviteToken = inviteToken
            return
        }

        when (uri?.host) {
            "auth" -> {
                latestLoginCode = uri.getQueryParameter("loginCode")
                    ?.trim()
                    ?.ifBlank { null }
                latestLoginErrorMessage = parseAuthCallbackError(uri)
            }
        }
    }

    private fun Uri.extractInviteToken(): String? {
        val isScheduleInviteLink = scheme == "scheduleapp" &&
            host == "invite" &&
            pathSegments.firstOrNull() == "accept"
        val isHttpsInviteLink = scheme == "https" &&
            host in setOf("api.link-together.site", "link-together.site") &&
            pathSegments.firstOrNull() == "invites"
        val isKakaoShareAppLink = scheme == BuildConfig.KAKAO_NATIVE_APP_KEY.takeIf { it.isNotBlank() }
            ?.let { "kakao$it" } &&
            host == "kakaolink"

        if (!isScheduleInviteLink && !isHttpsInviteLink && !isKakaoShareAppLink) {
            return null
        }

        return getQueryParameter("inviteToken")
            ?.trim()
            ?.ifBlank { null }
            ?: pathSegments.getOrNull(1)
            ?.trim()
            ?.ifBlank { null }
    }

    private fun parseAuthCallbackError(uri: Uri): String? {
        val errorCode = uri.getQueryParameter("errorCode")?.trim()?.ifBlank { null } ?: return null
        val description = uri.getQueryParameter("errorDescription")
            ?.trim()
            ?.ifBlank { null }
        return if (description != null) {
            "$errorCode: $description"
        } else {
            errorCode
        }
    }

    private fun shareInvite(
        invite: CreateInviteResponse,
        inviterNickname: String?
    ) {
        val title = "${inviterNickname?.ifBlank { null } ?: "파트너"}님이 LinkTogether 초대를 보냈어요."
        val description = "LinkTogether에서 캘린더와 근무 스케줄을 함께 쓰려면 초대를 수락해 주세요."
        val executionParams = mapOf("inviteToken" to invite.inviteToken)
        val shareUrl = invite.bestShareUrl()
        val template = TextTemplate(
            text = "$title\n$description",
            link = Link(
                webUrl = shareUrl,
                mobileWebUrl = shareUrl,
                androidExecutionParams = executionParams
            ),
            buttonTitle = "초대 수락",
            buttons = listOf(
                Button(
                    title = "초대 수락",
                    link = Link(
                        webUrl = shareUrl,
                        mobileWebUrl = shareUrl,
                        androidExecutionParams = executionParams
                    )
                )
            )
        )

        if (ShareClient.instance.isKakaoTalkSharingAvailable(this)) {
            ShareClient.instance.shareDefault(this, template) { sharingResult, error ->
                when {
                    error != null -> {
                        Toast.makeText(
                            this,
                            "카카오 링크 공유 설정을 확인해 주세요. 텍스트 링크로 공유합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        shareInviteViaText(title, description, invite)
                    }
                    sharingResult != null -> startActivity(sharingResult.intent)
                    else -> {
                        Toast.makeText(
                            this,
                            "카카오 링크 공유를 열지 못해 텍스트 링크로 공유합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        shareInviteViaText(title, description, invite)
                    }
                }
            }
            return
        }

        shareInviteViaText(title, description, invite)
    }

    private fun shareInviteViaText(
        title: String,
        description: String,
        invite: CreateInviteResponse
    ) {
        val message = buildString {
            appendLine(title)
            appendLine(description)
            appendLine("초대 링크: ${invite.bestShareUrl()}")
            appendLine()
            append("앱에서 바로 열기: ${invite.deepLink}")
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
                            repository.authenticateWithKakaoAccessToken(token.accessToken)
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

private fun CreateInviteResponse.bestShareUrl(): String {
    val candidate = shareUrl.trim()
    if (candidate.startsWith("https://") && !candidate.contains("app.example.com")) {
        return candidate
    }
    return "${CalendarApiConfig.inviteFallbackWebBaseUrl}/${inviteToken}"
}
