package com.burakmorinyo.gelirgider.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.burakmorinyo.gelirgider.domain.ReceiptInfo
import com.burakmorinyo.gelirgider.ocr.ReceiptScanner
import kotlinx.coroutines.launch

/**
 * Fişi fotoğraftan okur: kamerayla çek ya da galeriden seç. Okuma cihaz üzerinde yapılır.
 * [onResult] okunamazsa sebebiyle birlikte hata alır.
 */
@Composable
fun ReceiptScanMenu(expanded: Boolean, onDismiss: () -> Unit, onStart: () -> Unit, onResult: (Result<ReceiptInfo>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun process(uri: Uri) {
        onStart()
        scope.launch {
            onResult(ReceiptScanner.scan(context, uri))
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) process(Uri.parse(uri))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) process(uri)
    }

    DropdownMenu(expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Fişin fotoğrafını çek") },
            leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
            onClick = {
                onDismiss()
                val uri = ReceiptScanner.newPhotoUri(context)
                photoUri = uri.toString()
                runCatching { camera.launch(uri) }.onFailure { onResult(Result.failure(IllegalStateException("Kamera uygulaması açılamadı."))) }
            }
        )
        DropdownMenuItem(
            text = { Text("Galeriden seç") },
            leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
            onClick = {
                onDismiss()
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}
