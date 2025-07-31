package com.tutorials.chessscanner.screens.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    if (user == null) {
        // Defensive fallback — shouldn't happen if auth is gated properly
        Toast.makeText(context, "No user signed in", Toast.LENGTH_SHORT).show()
        onSignOut()
        return
    }

    val name = user.displayName ?: "Anonymous"
    val email = user.email ?: "No email"
    val photoUrl = user.photoUrl?.toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (photoUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(photoUrl),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            )
        } else {
            // Placeholder avatar
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Default avatar",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )
        }


        Text("Name: $name", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Email: $email", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {

            // Sign out from Google
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)

            googleSignInClient.signOut().addOnCompleteListener {
                FirebaseAuth.getInstance().signOut()
                onSignOut()
            }
        }) {
            Text("Sign Out")
        }
    }
}
