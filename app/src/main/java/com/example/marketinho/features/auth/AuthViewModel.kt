package com.example.marketinho.features.auth

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException // Importante para erros do Google Sign-In
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isAuthenticated = MutableStateFlow(auth.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    // Métodos de autenticação existentes (Email/Senha)
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.sendPasswordResetEmail(email).await()
                _errorMessage.value = "E-mail de redefinição de senha enviado para $email"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _isAuthenticated.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // ==============================================================
    // NOVOS MÉTODOS PARA GOOGLE SIGN-IN
    // ==============================================================

    // Função para obter o GoogleSignInClient
    // R.string.default_web_client_id é crucial e vem do google-services.json
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.marketinho.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // Função para iniciar o fluxo de login do Google
    // Esta função apenas lança a intent de login do Google
    fun startGoogleSignInFlow(signInClient: GoogleSignInClient, launcher: ActivityResultLauncher<Intent>) {
        _isLoading.value = true
        val signInIntent = signInClient.signInIntent
        launcher.launch(signInIntent)
    }

    // Função para lidar com o resultado do login do Google e autenticar no Firebase
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                // Pega o account do Google Sign-In. Se houver erro, ApiException será lançada.
                val account = task.getResult(ApiException::class.java)

                // Cria uma credencial Firebase com o ID Token do Google
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Autentica no Firebase com a credencial do Google
                auth.signInWithCredential(credential).await()

                _isAuthenticated.value = true
                _errorMessage.value = null
            } catch (e: ApiException) {
                // Erros específicos do Google Sign-In (ex: usuário cancelou, rede, erro de configuração)
                Log.e("AuthViewModel", "Google Sign-In failed: ${e.statusCode} - ${e.message}", e)
                _errorMessage.value = "Falha no login com Google: ${e.localizedMessage ?: "Erro desconhecido"}"
                _isAuthenticated.value = false
            } catch (e: Exception) {
                // Outros erros (ex: erro do Firebase após obter a credencial do Google)
                Log.e("AuthViewModel", "Firebase Google Auth failed: ${e.message}", e)
                _errorMessage.value = "Falha na autenticação Firebase: ${e.localizedMessage ?: "Erro desconhecido"}"
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}