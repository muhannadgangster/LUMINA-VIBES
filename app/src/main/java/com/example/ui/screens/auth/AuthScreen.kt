package com.example.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Attempt silent auto-sign in on initial launch
  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return@LaunchedEffect
    }
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = { /* Wait for user to tap sign in */ },
      scope = scope
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    // Ambient glowing liquid glass orbs in background
    Box(
      modifier = Modifier
        .size(320.dp)
        .align(Alignment.TopEnd)
        .background(
          Brush.radialGradient(
            listOf(LuminaViolet.copy(alpha = 0.15f), Color.Transparent)
          )
        )
    )
    Box(
      modifier = Modifier
        .size(300.dp)
        .align(Alignment.BottomStart)
        .background(
          Brush.radialGradient(
            listOf(LuminaCyan.copy(alpha = 0.15f), Color.Transparent)
          )
        )
    )

    GlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      shape = RoundedCornerShape(28.dp),
      elevation = 12.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Icon & Hologram Brand Badge
        Box(
          modifier = Modifier
            .size(72.dp)
            .liquidGlassSurface(shape = CircleShape, elevation = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Lumina",
            tint = LuminaCyan,
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Lumina",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 1.sp
          )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Next-Generation Video Streaming & Social Platform",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
          )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Features Highlights
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          FeatureRow(
            icon = Icons.Default.AutoAwesome,
            tint = LuminaCyan,
            title = "Liquid Glass Interface",
            subtitle = "Translucent aesthetics with fluid controls"
          )
          FeatureRow(
            icon = Icons.Default.CheckCircle,
            tint = LuminaPink,
            title = "Creator Studio & Analytics",
            subtitle = "Publish content with genuine real-time metrics"
          )
          FeatureRow(
            icon = Icons.Default.Security,
            tint = LuminaViolet,
            title = "Cloud Persistence",
            subtitle = "Protected social interactions and sync"
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedVisibility(
          visible = errorMessage != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFFF87171),
              textAlign = TextAlign.Center
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          )
        }

        // GOOGLE SIGN IN BUTTON
        Button(
          onClick = {
            isLoading = true
            errorMessage = null
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                // Sync user profile to Firestore
                scope.launch {
                  syncUserProfileToFirestore(context)
                }
                onAuthSuccess()
              },
              onAuthError = { errorMsg ->
                isLoading = false
                errorMessage = errorMsg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
              }
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_btn"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1E293B)
          ),
          enabled = !isLoading
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = Color(0xFF0F172A),
              strokeWidth = 2.5.dp
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
                    )
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "G",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color(0xFF0F172A)
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  title: String,
  subtitle: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .liquidGlassSurface(shape = CircleShape, elevation = 2.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
      )
    }
  }
}

private fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }

  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        syncUserProfileToFirestore(context)
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activity = context as? Activity ?: run {
        onAuthError("Context is not an Activity")
        return@launch
      }
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type returned")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

suspend fun syncUserProfileToFirestore(context: Context) {
  val user = Firebase.auth.currentUser ?: return
  try {
    val databaseId = context.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(databaseId)
    val profileData = mapOf(
      "userId" to user.uid,
      "displayName" to (user.displayName ?: "Creator"),
      "email" to (user.email ?: ""),
      "avatarUrl" to (user.photoUrl?.toString() ?: ""),
      "createdAt" to FieldValue.serverTimestamp()
    )
    db.collection("users").document(user.uid).set(profileData).await()
    Log.i(TAG, "Synced user profile for ${user.uid} to Firestore")
  } catch (e: Exception) {
    Log.w(TAG, "Could not sync user profile to Firestore: ${e.message}")
  }
}

fun signOut(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}
