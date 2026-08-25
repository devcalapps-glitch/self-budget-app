package com.selfbudget.app.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.selfbudget.app.data.local.UserDao
import com.selfbudget.app.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthResult {
    data class Success(val user: UserEntity) : AuthResult
    data class Error(val message: String) : AuthResult
}

@Singleton
class AuthManager @Inject constructor(
    private val userDao: UserDao
) {
    val currentUser: Flow<UserEntity?> = userDao.getCurrentUser()

    suspend fun signInWithGoogle(context: Context, webClientId: String): AuthResult {
        val credentialManager = CredentialManager.create(context)

        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val newUser = UserEntity(
                    id = googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: "User",
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
                val existingUser = userDao.getUserById(newUser.id)
                val userToSave = if (existingUser != null) {
                    existingUser.copy(
                        displayName = newUser.displayName ?: existingUser.displayName,
                        photoUrl = newUser.photoUrl ?: existingUser.photoUrl
                    )
                } else {
                    newUser
                }
                userDao.insertUser(userToSave)
                AuthResult.Success(userToSave)
            } else {
                AuthResult.Error("Unsupported credential type returned: ${credential.type}")
            }
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Google Sign-In failed", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In cancelled or failed.")
        } catch (e: Exception) {
            Log.e("AuthManager", "Unexpected Auth Exception", e)
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun signOut() {
        userDao.clearUsers()
    }
}
