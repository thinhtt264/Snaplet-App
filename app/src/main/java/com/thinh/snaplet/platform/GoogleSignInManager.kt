package com.thinh.snaplet.platform

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.thinh.snaplet.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInManager @Inject constructor(
    private val credentialManager: CredentialManager,
) {
    suspend fun signIn(context: Context): String {
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val result = credentialManager.getCredential(context, request)

        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }
}