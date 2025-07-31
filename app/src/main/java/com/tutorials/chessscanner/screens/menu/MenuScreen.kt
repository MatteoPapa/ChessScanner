package com.tutorials.chessscanner.screens.menu

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.tutorials.chessscanner.R
import java.io.File

@Composable
fun MenuScreen(navController: NavController) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val photoUri = remember { mutableStateOf<Uri?>(null) }


    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = result.data?.let { com.yalantis.ucrop.UCrop.getOutput(it) }
            if (resultUri != null) {
                val file = uriToFile(resultUri, context)
                isLoading = true

                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(true)
                    ?.addOnSuccessListener { result ->
                        val token = result.token
                        sendToBackend(file, context, navController, { isLoading = false }, token)
                    }
                    ?.addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_LONG).show()
                    }
            }

        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri.value != null) {
            launchCrop(photoUri.value!!, context, cropLauncher)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && photoUri.value != null) {
            cameraLauncher.launch(photoUri.value!!)
        }
        else Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            launchCrop(it, context, cropLauncher)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "ChessScanner logo",
                modifier = Modifier.height(160.dp)
            )

            // New Analysis Section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Analysis", style = MaterialTheme.typography.titleLarge,    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        val imageFile = File.createTempFile("captured_", ".jpg", context.cacheDir)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            imageFile
                        )
                        photoUri.value = uri
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Take Photo") }

                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import Photo") }

                if (isLoading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            HorizontalDivider()

            // Your Analyses Section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Saved Analyses", style = MaterialTheme.typography.titleLarge,modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
                Button(
                    onClick = { navController.navigate("analysis") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("View Analyses") }
            }

            HorizontalDivider()

            // Profile
            Button(
                onClick = { navController.navigate("profile") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Go to Profile") }
        }
    }
}

