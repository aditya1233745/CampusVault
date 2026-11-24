package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "AuthManager"

class AuthManager(private val db: FirebaseFirestore) {

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val cleanEmail = email.trim()
            require(cleanEmail.contains("@")) { "Enter a valid email address." }
            require(password.length >= 6) { "Password must be at least 6 characters." }
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Email sign-in failed", e)
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
        branch: String,
        gradYear: String,
        phoneNumber: String = ""
    ): Result<Unit> {
        return try {
            val cleanName = fullName.trim().take(80)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phoneNumber.trim().take(20)
            require(cleanName.isNotEmpty()) { "Enter your full name." }
            require(cleanEmail.contains("@")) { "Enter a valid email address." }
            require(password.length >= 6) { "Password must be at least 6 characters." }

            val authResult = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: throw IllegalStateException("Account created but user session is unavailable.")

            runCatching {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            val payload = mutableMapOf<String, Any>(
                "userId" to user.uid,
                "fullName" to cleanName,
                "collegeEmail" to cleanEmail,
                "branch" to branch,
                "gradYear" to gradYear,
                "verified" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhone.isNotEmpty()) {
                payload["phoneNumber"] = cleanPhone
            }
            db.collection("users").document(user.uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Email sign-up failed", e)
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val cleanEmail = email.trim()
            require(cleanEmail.contains("@")) { "Enter your college email first to reset password." }
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Password reset failed", e)
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    fun signOut() {
        Firebase.auth.signOut()
    }

    private fun friendlyAuthError(e: Exception): String {
        if (e is IllegalArgumentException) return e.message ?: "Invalid input."
        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
                "ERROR_USER_NOT_FOUND" -> "No account found for that email. Try Sign Up."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
                "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Enable Email/Password sign-in in Firebase Console -> Authentication."
                else -> e.localizedMessage ?: "Authentication failed."
            }
        }
        return e.localizedMessage ?: "Authentication failed."
    }
}
