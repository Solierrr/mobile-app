package com.project.solaria_mobile.core.images

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun rememberCameraCapture(onResult: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pending?.let(Uri::parse)
        pending = null
        onResult(if (saved) uri else null)
    }
    return {
        val directory = File(context.cacheDir, "captures").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, "photo-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pending = uri.toString()
        launcher.launch(uri)
    }
}
