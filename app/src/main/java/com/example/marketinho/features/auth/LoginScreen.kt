// marketinho/features/auth/LoginScreen.kt

package com.example.marketinho.features.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Certifique-se de que este import está presente
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle // Certifique-se de que este import está presente
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Remova @OptIn(ExperimentalMaterial3Api::class) se não for mais necessário
// Se a IDE reclamar, adicione de volta.

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    // Inicializa o GoogleSignInClient
    val googleSignInClient = remember { authViewModel.getGoogleSignInClient(context) }

    // Launcher para a tela de login do Google
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Passa o resultado para o ViewModel para ser processado
        authViewModel.handleGoogleSignInResult(result.data)
    }

    // Observa o estado de autenticação do AuthViewModel para navegar
    LaunchedEffect(authViewModel.isAuthenticated.collectAsState().value) {
        if (authViewModel.isAuthenticated.value) {
            onLoginSuccess() // Chama o callback se o usuário estiver autenticado
        }
    }

    // Exibe Toast de erro
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearErrorMessage() // Limpa a mensagem após exibir
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bem-vindo ao Marketinho!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.Black) // Mantém a cor preta para visibilidade
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.Black) // Mantém a cor preta para visibilidade
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botão de Login com Google
        Button(
            onClick = {
                authViewModel.startGoogleSignInFlow(googleSignInClient, googleAuthLauncher)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4), // Cor azul do Google
                contentColor = Color.White // Texto branco
            )
        ) {
            // Você pode adicionar um ícone do Google aqui se tiver (ex: com.google.accompanist:accompanist-drawablepainter)
            Text("Entrar com Google")
        }
        Spacer(modifier = Modifier.height(16.dp)) // Espaço após o botão do Google

        TextButton(
            onClick = {
                authViewModel.clearErrorMessage()
                if (email.isNotBlank()) {
                    authViewModel.sendPasswordResetEmail(email)
                    Toast.makeText(context, "Um e-mail para redefinir a senha pode ter sido enviado para $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Por favor, digite seu e-mail para redefinir a senha.", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading
        ) {
            Text("Esqueceu a senha?")
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { authViewModel.register(email, password) },
            enabled = !isLoading
        ) {
            Text("Não tem conta? Cadastre-se")
        }
    }
}