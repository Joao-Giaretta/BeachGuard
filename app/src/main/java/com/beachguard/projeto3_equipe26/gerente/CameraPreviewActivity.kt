package com.beachguard.projeto3_equipe26.gerente

import CameraViewModel
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.beachguard.projeto3_equipe26.databinding.ActivityCameraPreviewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraPreviewBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService

    private var locacaoId: String? = ""
    private var armarioId: String? = ""

    private var quantidadeFotos: Int = 1
    private var isFirstPhotoTaken = false
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicialização de variáveis da camera
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newCachedThreadPool()

        // Recebendo dados da tela anterior
        quantidadeFotos = intent.getIntExtra("quantidadeFotos", 1)
        locacaoId = intent.getStringExtra("locacaoId")
        armarioId = intent.getStringExtra("armarioId")

        // Inicialização da câmera
        startCamera()

        binding.btnTakePhoto.setOnClickListener {
            // Se for para tirar apenas uma foto
            if (quantidadeFotos == 1){
                takePhotoForOne()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    blinkPreview()
                }
            } else {
                // Se for para tirar duas fotos
                if (!isFirstPhotoTaken) {
                    // Se for a primeira foto
                    takePhotoForTwo { imageUrl1 ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            blinkPreview()
                        }
                        Toast.makeText(this, "Primeira foto salva. Agora, tire a segunda foto.", Toast.LENGTH_SHORT).show()
                        viewModel.imageUrl1 = imageUrl1
                        isFirstPhotoTaken = true
                    }
                } else {
                    // Se for a segunda foto
                    takePhotoForTwo { imageUrl2 ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            blinkPreview()
                        }
                        Toast.makeText(this, "Segunda foto salva.", Toast.LENGTH_SHORT).show()
                        viewModel.imageUrl2 = imageUrl2
                        navigateToDisplayBothImages(viewModel.imageUrl1!!, viewModel.imageUrl2!!)
                    }
                }
            }
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        // Inicialização da câmera
        cameraProviderFuture.addListener({
            // Quando a câmera estiver pronta
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraPreviewActivity", "Erro ao abrir a câmera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoForOne() {
        // Tirar foto para apenas uma imagem
        imageCapture?.let {
            // Nome do arquivo
            val fileName = "FOTO_JPEG_${System.currentTimeMillis()}.jpg"
            // Arquivo
            val file = File(externalMediaDirs[0], fileName)
            // Opções de saída
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            // Tirar a foto
            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("CameraPreviewActivity", "Imagem salva com sucesso em ${file.absolutePath}")
                        compressAndUploadImage(file, ::uploadImageToFirebaseForOne)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(binding.root.context, "Erro ao salvar a imagem", Toast.LENGTH_SHORT).show()
                        Log.e("CameraPreviewActivity", "Erro ao salvar a imagem", exception)
                    }
                }
            )
        }
    }

    private fun takePhotoForTwo(callback: (String) -> Unit) {
        // Tirar foto para duas imagens
        imageCapture?.let {
            val fileName = "FOTO_JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("CameraPreviewActivity", "Image saved successfully at ${file.absolutePath}")
                        // Compress and upload image
                        compressAndUploadImage(file) { uploadImageToFirebaseForTwo(file, callback) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(this@CameraPreviewActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                        Log.e("CameraPreviewActivity", "Error saving image", exception)
                    }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun blinkPreview() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun compressAndUploadImage(file: File, uploadFunction: (File) -> Unit) {
        // Comprimir e fazer upload da imagem
        uploadFunction(file)
    }

    private fun uploadImageToFirebaseForOne(file: File) {
        // Fazer upload da imagem para o Firebase
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${file.name}")

        val fileUri = file.toUri()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Upload da imagem
                val uri = imageRef.putFile(fileUri).await().storage.downloadUrl.await()
                withContext(Dispatchers.Main) {
                    navigateToDisplayImage(uri.toString())
                }
            } catch (e: Exception) {
                Log.e("CameraPreviewActivity", "Erro ao fazer upload da imagem", e)
            }
        }
    }

    private fun uploadImageToFirebaseForTwo(file: File, callback: (String) -> Unit) {
        // Fazer upload da imagem para o Firebase
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${file.name}")

        val fileUri = file.toUri()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Upload da imagem
                val uri = imageRef.putFile(fileUri).await().storage.downloadUrl.await()
                withContext(Dispatchers.Main) {
                    callback(uri.toString())
                }
            } catch (e: Exception) {
                Log.e("CameraPreviewActivity", "Error uploading image", e)
            }
        }
    }

    private fun navigateToDisplayImage(imageUrl: String) {
        // Navegar para a tela de exibição da imagem
        val intent = Intent(this, DisplayOneClientImageActivity::class.java).apply {
            putExtra("image_url", imageUrl)
            putExtra("locacaoId", locacaoId)
            putExtra("armarioId", armarioId)
        }
        startActivity(intent)
    }

    private fun navigateToDisplayBothImages(imageUrl1: String, imageUrl2: String) {
        // Navegar para a tela de exibição das duas imagens
        val intent = Intent(this, DisplayBothClientImagesActivity::class.java).apply {
            putExtra("imageUrl1", imageUrl1)
            putExtra("imageUrl2", imageUrl2)
            putExtra("locacaoId", locacaoId)
            putExtra("armarioId", armarioId)
        }
        Log.i("CameraPreviewActivity", "Navigating to DisplayBothClientImagesActivity")
        startActivity(intent)
    }
}
