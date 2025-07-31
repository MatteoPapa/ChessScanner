package com.tutorials.chessscanner.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tutorials.chessscanner.BuildConfig
import com.tutorials.chessscanner.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(context as Activity) { authResult ->
                    if (authResult.isSuccessful) {
                        onLoginSuccess()
                    } else {
                        errorMessage = authResult.exception?.message
                    }
                }
        } catch (e: ApiException) {
            errorMessage = e.localizedMessage
        }
    }

    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            //.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, signInOptions)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "ChessScanner logo",
                modifier = Modifier
                    .height(160.dp)
                    .padding(bottom = 10.dp)
            )

            // Google Sign-In
            Button(onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Email/Password Auth
            val focusRequester = remember { FocusRequester() }
            val passwordFocusRequester = remember { FocusRequester() }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    }
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(passwordFocusRequester),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Email and password must not be empty"
                        } else {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(context as Activity) { task ->
                                    if (task.isSuccessful) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = task.exception?.message
                                    }
                                }
                        }
                    }
                )
            )


            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password must not be empty"
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(context as Activity) { task ->
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = task.exception?.message
                                }
                            }
                    }
                }) {
                    Text("Register")
                }

                Button(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password must not be empty"
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(context as Activity) { task ->
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = task.exception?.message
                                }
                            }
                    }
                }) {
                    Text("Sign In")
                }
            }

            // Show error if any
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
