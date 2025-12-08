// features/camera/CameraSection.kt
package com.example.marketinho.features.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // MANTENHA ESTE IMPORT
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun CameraSection(
    onImageCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // MANTENHA: Usando rememberSaveable para persistir a Uri da imagem
    var capturedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedUri?.let { uri ->
                onImageCaptured(uri) // Chama o callback com a Uri da foto
                Toast.makeText(context, "Foto capturada!", Toast.LENGTH_SHORT).show()
                // NÃO LIMPAMOS capturedUri AQUI, POIS ELE SERÁ USADO NO ProductViewModel
            }
        } else {
            // Apenas mostra o toast se a captura falhou ou foi cancelada
            Toast.makeText(context, "Captura de foto cancelada ou falhou.", Toast.LENGTH_SHORT).show()
        }
        // IMPORTANTE: Limpamos o capturedUri APENAS se a captura não foi bem-sucedida
        // ou se o ProductViewModel indicar que ele não é mais necessário.
        // No seu fluxo atual, o ProductViewModel gerencia o _imageUri.
        // Para evitar erros de "falha", vamos remover o capturedUri = null daqui por enquanto.
        // Se a tela de marcação aparecer, significa que o Uri chegou no VM.
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            val uri = createImageUri(context)
            uri?.let {
                capturedUri = it // Salva a Uri antes de lançar a câmera
                cameraLauncher.launch(it)
            }
        } else {
            Toast.makeText(context, "Permissão da câmera negada", Toast.LENGTH_LONG).show()
        }
    }

    Button(
        onClick = {
            if (hasCameraPermission) {
                val uri = createImageUri(context)
                uri?.let {
                    capturedUri = it // Salva a Uri antes de lançar a câmera
                    cameraLauncher.launch(it)
                }
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = modifier
    ) {
        Text("Tirar Foto do Produto")
    }
}

// MANTENHA: Fun createImageUri original, sem RELATIVE_PATH
private fun createImageUri(context: Context): Uri? {
    val contentResolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "produto_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // REMOVIDO: put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Marketinho")
    }
    return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}