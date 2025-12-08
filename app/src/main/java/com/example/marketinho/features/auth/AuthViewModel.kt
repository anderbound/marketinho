package com.example.marketinho.features.auth

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

    // ========== VALIDAÇÕES ==========

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email não pode estar vazio"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email inválido"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Senha não pode estar vazia"
            password.length < 6 -> "Senha deve ter no mínimo 6 caracteres"
            else -> null
        }
    }

    fun validatePasswordMatch(password: String, confirmPassword: String): String? {
        return when {
            password != confirmPassword -> "As senhas não coincidem"
            else -> null
        }
    }

    // ========== LOGIN ==========

    fun login(email: String, password: String) {
        // Validações
        val emailError = validateEmail(email)
        if (emailError != null) {
            _errorMessage.value = emailError
            return
        }

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            _errorMessage.value = passwordError
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _errorMessage.value = null
                Log.d("AuthViewModel", "Login bem-sucedido: ${auth.currentUser?.email}")
            } catch (e: FirebaseAuthInvalidUserException) {
                _errorMessage.value = "Usuário não encontrado. Verifique o email."
                _isAuthenticated.value = false
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _errorMessage.value = "Email ou senha incorretos."
                _isAuthenticated.value = false
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro no login: ${e.message}", e)
                _errorMessage.value = "Erro ao fazer login: ${e.localizedMessage}"
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== CADASTRO ==========

    fun register(email: String, password: String, confirmPassword: String) {
        // Validações
        val emailError = validateEmail(email)
        if (emailError != null) {
            _errorMessage.value = emailError
            return
        }

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            _errorMessage.value = passwordError
            return
        }

        val passwordMatchError = validatePasswordMatch(password, confirmPassword)
        if (passwordMatchError != null) {
            _errorMessage.value = passwordMatchError
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _errorMessage.value = null
                Log.d("AuthViewModel", "Cadastro bem-sucedido: ${auth.currentUser?.email}")
            } catch (e: FirebaseAuthWeakPasswordException) {
                _errorMessage.value = "Senha muito fraca. Use no mínimo 6 caracteres."
                _isAuthenticated.value = false
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _errorMessage.value = "Email inválido."
                _isAuthenticated.value = false
            } catch (e: FirebaseAuthUserCollisionException) {
                _errorMessage.value = "Este email já está cadastrado. Faça login."
                _isAuthenticated.value = false
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro no cadastro: ${e.message}", e)
                _errorMessage.value = "Erro ao cadastrar: ${e.localizedMessage}"
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== RECUPERAÇÃO DE SENHA ==========

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit = {}) {
        val emailError = validateEmail(email)
        if (emailError != null) {
            _errorMessage.value = emailError
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.sendPasswordResetEmail(email).await()
                _errorMessage.value = "Email de redefinição enviado para $email"
                Log.d("AuthViewModel", "Email de redefinição enviado para: $email")
                onSuccess()
            } catch (e: FirebaseAuthInvalidUserException) {
                _errorMessage.value = "Email não encontrado."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro ao enviar email: ${e.message}", e)
                _errorMessage.value = "Erro ao enviar email: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== GOOGLE SIGN-IN ==========

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.marketinho.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun startGoogleSignInFlow(signInClient: GoogleSignInClient, launcher: ActivityResultLauncher<Intent>) {
        _isLoading.value = true
        val signInIntent = signInClient.signInIntent
        launcher.launch(signInIntent)
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).await()
                _isAuthenticated.value = true
                _errorMessage.value = null
                Log.d("AuthViewModel", "Login com Google bem-sucedido: ${auth.currentUser?.email}")
            } catch (e: ApiException) {
                Log.e("AuthViewModel", "Google Sign-In falhou: ${e.statusCode} - ${e.message}", e)
                _errorMessage.value = when (e.statusCode) {
                    12501 -> "Login cancelado pelo usuário"
                    12500 -> "Erro de configuração. Verifique o google-services.json"
                    else -> "Erro no login com Google: ${e.localizedMessage}"
                }
                _isAuthenticated.value = false
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase Google Auth falhou: ${e.message}", e)
                _errorMessage.value = "Erro na autenticação: ${e.localizedMessage}"
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== LOGOUT ==========

    fun signOut() {
        auth.signOut()
        _isAuthenticated.value = false
        _errorMessage.value = null
        Log.d("AuthViewModel", "Usuário deslogado")
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}