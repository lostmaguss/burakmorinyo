package com.burakmorinyo.gelirgider.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.burakmorinyo.gelirgider.domain.ReceiptInfo
import com.burakmorinyo.gelirgider.domain.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Fiş fotoğrafını cihaz üzerinde (internetsiz) okur ve [ReceiptParser] ile çözümler */
object ReceiptScanner {

    /** Kamera uygulamasının fotoğrafı yazacağı geçici dosya */
    fun newPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // önceki fotoğrafları sakla(ma)
        val file = File(dir, "fis-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun recognize(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        // Satırları yukarıdan aşağıya sırala: fişlerde sütunlar ayrı bloklara düşebiliyor
                        val parts = result.textBlocks.flatMap { it.lines }.map { l ->
                            val b = l.boundingBox
                            TextPart(l.text, b?.centerY() ?: 0, b?.height() ?: 0, b?.left ?: 0)
                        }
                        cont.resume(mergeRows(parts))
                    }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            recognizer.close()
        }
    }

    data class TextPart(val text: String, val centerY: Int, val height: Int, val left: Int)

    /** Aynı yükseklikteki satır parçalarını ("TOPLAM" ... "*245,90") soldan sağa tek satırda birleştirir */
    fun mergeRows(parts: List<TextPart>): String {
        val rows = mutableListOf<MutableList<TextPart>>()
        for (p in parts.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            val tolerance = maxOf(p.height, row?.first()?.height ?: 0) / 2
            if (row != null && kotlin.math.abs(row.first().centerY - p.centerY) <= tolerance) row += p else rows += mutableListOf(p)
        }
        return rows.joinToString("\n") { r -> r.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    suspend fun scan(context: Context, uri: Uri): ReceiptInfo = ReceiptParser.parse(recognize(context, uri))
}
