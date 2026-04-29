package com.example.scheduleapp.ui.calendar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

object ShiftOcrImageRecognizer {
    private const val MinimumMlKitImageDimension = 32
    private const val TargetSmallOcrImageDimension = 128

    fun recognize(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val image = try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw IOException()
            InputImage.fromBitmap(bitmap.preparedForOcr(), 0)
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

    private fun Bitmap.preparedForOcr(): Bitmap {
        val scaled = scaledForOcr()
        val targetWidth = maxOf(MinimumMlKitImageDimension, scaled.width)
        val targetHeight = maxOf(MinimumMlKitImageDimension, scaled.height)
        if (targetWidth == scaled.width && targetHeight == scaled.height) return scaled

        return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { padded ->
            Canvas(padded).apply {
                drawColor(Color.WHITE)
                drawBitmap(scaled, 0f, 0f, null)
            }
        }
    }

    private fun Bitmap.scaledForOcr(): Bitmap {
        val scale = maxOf(
            1f,
            TargetSmallOcrImageDimension.toFloat() / width,
            TargetSmallOcrImageDimension.toFloat() / height
        )
        if (scale <= 1f) return this

        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }
}
