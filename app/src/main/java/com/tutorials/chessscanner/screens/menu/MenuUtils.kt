package com.tutorials.chessscanner.screens.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.navigation.NavController
import com.tutorials.chessscanner.BuildConfig
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

fun uriToFile(uri: Uri, context: Context): File {
    val input = context.contentResolver.openInputStream(uri)
    val file = File.createTempFile("imported_", ".png", context.cacheDir)
    FileOutputStream(file).use { out ->
        input?.copyTo(out)
        out.flush()
    }
    return file
}

fun sendToBackend(
    file: File,
    context: Context,
    navController: NavController,
    onDone: () -> Unit,
    token: String?
) {
    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", file.name, file.asRequestBody("image/png".toMediaTypeOrNull()))
        .build()

    val request = Request.Builder()
        .url("${BuildConfig.API_BASE_URL}/detect_fen")
        .addHeader("Authorization", "Bearer $token")
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val json = JSONObject(body ?: "{}")
            val fen = json.getString("fen")

            withContext(Dispatchers.Main) {
                onDone()
                val encodedFen = Uri.encode(fen)
                navController.navigate("fen_editor/$encodedFen")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onDone()
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun launchCrop(
    sourceUri: Uri,
    context: Context,
    cropLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped.jpg"))
    val options = UCrop.Options().apply {
        setToolbarTitle("Crop Image")
        setToolbarColor(android.graphics.Color.parseColor("#6200EE"))
        setStatusBarColor(android.graphics.Color.parseColor("#3700B3"))
        setToolbarWidgetColor(android.graphics.Color.WHITE)
        setActiveControlsWidgetColor(android.graphics.Color.parseColor("#6200EE"))
        setFreeStyleCropEnabled(false)
        setHideBottomControls(false)
        setShowCropFrame(true)
        setShowCropGrid(true)
        setCircleDimmedLayer(false)
        setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
    }

    val cropIntent = UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(1f, 1f)
        .withMaxResultSize(1024, 1024)
        .withOptions(options)
        .getIntent(context)

    cropLauncher.launch(cropIntent)
}
