// marketinho/features/auth/LoginScreen.kt

package com.example.marketinho.features.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.* // Certifique-se de que este import está correto para Material3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth // Não é estritamente necessário importar aqui, mas ajuda na compreensão

// Remover ou comentar @OptIn(ExperimentalMaterial3Api::class) se não for mais necessário
// a partir de Compose Material3 1.0.0 estável. Se a IDE reclamar, descomente.

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

        // Removidos os textos de teste e fundos temporários
        // O `OutlinedTextField` agora usará as cores definidas no seu `Theme.kt`

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") }, // Label padrão, usa a cor do tema
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            // Usa as cores do tema que você definiu no `Theme.kt`
            textStyle = TextStyle(color = Color.Black) // FORÇA UMA COR EXTREMAMENTE VISÍVEL (verde fluorescente)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") }, // Label padrão, usa a cor do tema
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
            // Usa as cores do tema que você definiu no `Theme.kt`
            textStyle = TextStyle(color = Color.Black) // FORÇA A MESMA COR
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

        // Você pode adicionar botões para login social aqui (Google, Facebook)
    }
}