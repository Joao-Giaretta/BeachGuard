package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beachguard.projeto3_equipe26.databinding.ActivityLeitorQrcodeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager

class LeitorQRCodeActivity: AppCompatActivity() {

    private lateinit var binding: ActivityLeitorQrcodeBinding
    private lateinit var capture: CaptureManager
    private lateinit var firestore: FirebaseFirestore

    private var isProcessing = false

    companion object {
        const val CAMERA_REQUEST_CODE = 123 // Define o código de solicitação da câmera
        const val SCAN_DELAY = 2000L // 2 segundos de atraso entre leituras
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeitorQrcodeBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        // Inicializa o gerenciador de captura
        capture = CaptureManager(this, binding.barcodeScanner)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()

        // Inicializa o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Verifica a permissão da câmera e solicita se necessário
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            // Permissão já concedida, pode iniciar a câmera
            startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    // Chamado quando um QRCode é escaneado
    private fun onBarcodeScanned(barcodeResult: BarcodeResult) {
        val scannedText = barcodeResult.text
        if (scannedText.isNullOrEmpty()) {
            showToast("Código QR inválido ou vazio")
        } else {
            verificarID(scannedText)
        }
    }

    // Função para verificar se o ID do QRCode existe na coleção "locacoes"
    private fun verificarID(id: String) {
        firestore.collection("locacoes").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // O documento com o ID existe na coleção
                    showToast("ID encontrado na coleção 'locacoes'")
                    val intent = Intent(this, QtndPessoasActivity::class.java)
                    intent.putExtra("locacaoId", id)
                    startActivity(intent)
                    finish()
                } else {
                    // O documento com o ID não existe na coleção
                    showToast("ID não encontrado na coleção 'locacoes'")
                }
                resetProcessing()
            }
            .addOnFailureListener { exception ->
                showToast("QRCODE INVALIDO: $exception")
                resetProcessing()
            }
    }

    // Inicia a câmera quando a permissão é concedida
    private fun startCamera() {
        // Adicione o listener para os resultados do scanner de código de barras
        binding.barcodeScanner.decodeContinuous { barcodeResult ->
            if (!isProcessing) {
                isProcessing = true
                try {
                    onBarcodeScanned(barcodeResult)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Erro ao processar o código QR: ${e.message}")
                    resetProcessing()
                }
            }
        }
    }

    // Trata o resultado da solicitação de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, pode iniciar a câmera
                startCamera()
            } else {
                // Permissão negada, avise o usuário ou tome alguma outra ação apropriada
                showToast("Permissão da câmera negada")
            }
        }
    }

    // Exibe um Toast com a mensagem fornecida
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Reseta o estado de processamento após o atraso
    private fun resetProcessing() {
        Handler(Looper.getMainLooper()).postDelayed({
            isProcessing = false
        }, SCAN_DELAY)
    }
}