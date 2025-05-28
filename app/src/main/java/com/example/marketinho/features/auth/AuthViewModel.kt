// marketinho/features/auth/AuthViewModel.kt

package com.example.marketinho.features.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    init {
        // Observa mudanças no estado de autenticação do Firebase
        firebaseAuth.addAuthStateListener { auth ->
            _isAuthenticated.value = auth.currentUser != null
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Por favor, preencha todos os campos."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Limpa qualquer erro anterior

        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                        if (task.isSuccessful) {
                            // Login bem-sucedido, o listener do authState já atualizou _isAuthenticated
                            // Não precisamos fazer mais nada aqui além de garantir que isLoading seja false
                        } else {
                            _errorMessage.value = handleFirebaseException(task.exception)
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Ocorreu um erro inesperado: ${e.message}"
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Por favor, preencha todos os campos."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "A senha deve ter no mínimo 6 caracteres."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                        if (task.isSuccessful) {
                            // Cadastro bem-sucedido, o listener do authState já atualizou _isAuthenticated
                            // Opcional: Enviar e-mail de verificação
                            // firebaseAuth.currentUser?.sendEmailVerification()
                            // _errorMessage.value = "Cadastro realizado! Verifique seu e-mail para ativar a conta."
                        } else {
                            _errorMessage.value = handleFirebaseException(task.exception)
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Ocorreu um erro inesperado: ${e.message}"
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        // O listener do authStateListener vai capturar essa mudança e atualizar _isAuthenticated para false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    // Adicione esta função dentro do AuthViewModel.kt
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Por favor, digite seu e-mail para enviar o link de redefinição."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                        if (task.isSuccessful) {
                            // Não exiba Toast aqui, o LoginScreen já o fará
                        } else {
                            _errorMessage.value = handleFirebaseException(task.exception)
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Ocorreu um erro inesperado ao enviar o e-mail: ${e.message}"
            }
        }
    }

    private fun handleFirebaseException(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Usuário não encontrado ou desabilitado."
            is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas. Verifique seu e-mail e senha."
            is FirebaseAuthWeakPasswordException -> "Senha muito fraca. Use pelo menos 6 caracteres."
            is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
            else -> "Erro de autenticação: ${exception?.localizedMessage ?: "Ocorreu um erro desconhecido."}"
        }
    }
}