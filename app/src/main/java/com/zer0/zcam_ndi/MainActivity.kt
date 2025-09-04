package com.zer0.zcam_ndi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview as PreviewCompose
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zer0.zcam_ndi.ui.theme.ZCamNDITheme
import com.ndi.android.NDIlib

class MainActivity : ComponentActivity() {

    private var ndiSender: NDISender? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa a biblioteca NDI antes de criar qualquer sender
        if (!NDIlib.initialize()) {
            throw RuntimeException("Falha ao inicializar a biblioteca NDI SDK")
        }

        // Cria o sender NDI
        ndiSender = NDISender("ZCam-NDI")

        setContent {
            ZCamNDITheme {
                val context = LocalContext.current
                val previewView = remember { PreviewView(context) }

                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[Manifest.permission.CAMERA] == true &&
                            permissions[Manifest.permission.RECORD_AUDIO] == true
                    if (granted) startCamera(previewView)
                    else Toast.makeText(context, "Permissões negadas!", Toast.LENGTH_SHORT).show()
                }

                LaunchedEffect(previewView) {
                    if (cameraPermission == PackageManager.PERMISSION_GRANTED &&
                        audioPermission == PackageManager.PERMISSION_GRANTED) {
                        startCamera(previewView)
                    } else {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    }
                }

                // Exibe a prévia da câmera
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(text = "ZCam-NDI ativo!", modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(1280, 720))
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                // Converte YUV -> NV21 -> RGBA bytes direto
                val frameBytes = imageProxyToRGBABytes(imageProxy)
                frameBytes?.let {
                    ndiSender?.sendFrame(frameBytes, imageProxy.width, imageProxy.height)
                }
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao iniciar câmera: $e", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun imageProxyToRGBABytes(image: ImageProxy): ByteArray? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // NV21: Y + V + U
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Converte NV21 -> RGBA diretamente
        val width = image.width
        val height = image.height
        val out = ByteArray(width * height * 4) // RGBA
        var i = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yp = y * width + x
                val Y = nv21[yp].toInt() and 0xFF
                val uvIndex = y / 2 * width + (x / 2) * 2 + ySize
                val V = nv21[uvIndex].toInt() and 0xFF
                val U = nv21[uvIndex + 1].toInt() and 0xFF

                val r = (Y + 1.370705f * (V - 128)).toInt().coerceIn(0, 255)
                val g = (Y - 0.337633f * (U - 128) - 0.698001f * (V - 128)).toInt().coerceIn(0, 255)
                val b = (Y + 1.732446f * (U - 128)).toInt().coerceIn(0, 255)

                out[i++] = r.toByte()
                out[i++] = g.toByte()
                out[i++] = b.toByte()
                out[i++] = 0xFF.toByte() // Alpha
            }
        }
        return out
    }


    override fun onDestroy() {
        super.onDestroy()
        // Fecha o sender individual
        ndiSender?.close()
        // Finaliza a biblioteca NDI
        NDIlib.destroy()
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@PreviewCompose(showBackground = true)
@Composable
fun GreetingPreview() {
    ZCamNDITheme {
        Greeting("Android")
    }
}
