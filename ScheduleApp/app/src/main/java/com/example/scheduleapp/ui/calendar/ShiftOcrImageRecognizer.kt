package com.example.scheduleapp.ui.calendar

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

object ShiftOcrImageRecognizer {
    fun recognize(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: IOException) {
            onFailure("이미지를 읽지 못했습니다.")
            onComplete()
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { recognizedText ->
                onSuccess(recognizedText.text)
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "이미지 OCR 인식에 실패했습니다.")
            }
            .addOnCompleteListener {
                recognizer.close()
                onComplete()
            }
    }
}
