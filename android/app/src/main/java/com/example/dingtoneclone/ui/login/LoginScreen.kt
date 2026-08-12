package com.example.dingtoneclone.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon + Title
            Icon(
                Icons.Filled.Phone,
                contentDescription = null,
                tint = Color(0xFF7C5CBF),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "DingtoneClone",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Your free virtual number app",
                fontSize = 14.sp,
                color = Color(0xFFB0B0C0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))

            // Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E1B3A),
                tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        if (isSignUp) "Create Account" else "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(20.dp))

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C5CBF),
                            focusedLabelColor = Color(0xFF7C5CBF),
                            cursorColor = Color(0xFF7C5CBF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFB0B0C0),
                            unfocusedLabelColor = Color(0xFF808090),
                            unfocusedBorderColor = Color(0xFF3A3A5C)
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C5CBF),
                            focusedLabelColor = Color(0xFF7C5CBF),
                            cursorColor = Color(0xFF7C5CBF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFB0B0C0),
                            unfocusedLabelColor = Color(0xFF808090),
                            unfocusedBorderColor = Color(0xFF3A3A5C)
                        )
                    )
                    Spacer(Modifier.height(8.dp))

                    // Error message
                    AnimatedVisibility(visible = errorMsg != null) {
                        Text(
                            errorMsg ?: "",
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Login/Register button
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMsg = null
                                try {
                                    if (isSignUp) {
                                        auth.createUserWithEmailAndPassword(email.trim(), password).await()
                                    } else {
                                        auth.signInWithEmailAndPassword(email.trim(), password).await()
                                    }
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Authentication failed"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C5CBF),
                            disabledContainerColor = Color(0xFF3A3A5C)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Toggle sign-up / sign-in
                    TextButton(
                        onClick = { isSignUp = !isSignUp; errorMsg = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (isSignUp) "Already have an account? Sign In"
                            else "Don't have an account? Sign Up",
                            color = Color(0xFF9D7CE0),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
