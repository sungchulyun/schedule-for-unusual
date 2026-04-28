package com.example.scheduleapp.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object FcmTokenSync {
    suspend fun registerCurrentToken(repository: CalendarRepository = CalendarRepository()) {
        val token = currentTokenOrNull() ?: return
        repository.registerFcmToken(token)
    }

    suspend fun unregisterCurrentToken(repository: CalendarRepository = CalendarRepository()) {
        val token = currentTokenOrNull() ?: return
        repository.unregisterFcmToken(token)
    }

    private suspend fun currentTokenOrNull(): String? {
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    val token = if (task.isSuccessful) {
                        task.result?.trim()?.ifBlank { null }
                    } else {
                        null
                    }
                    if (continuation.isActive) {
                        continuation.resume(token)
                    }
                }
        }
    }
}
