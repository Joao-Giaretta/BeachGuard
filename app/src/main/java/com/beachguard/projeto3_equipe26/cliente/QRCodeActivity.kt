package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityQrcodeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QRCodeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var binding: ActivityQrcodeBinding
    private var quiosqueId: String? = ""
    private var locacaoId: String? = ""
    private var fromHomeActivity: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        locacaoId = intent.getStringExtra("locacaoId")
        quiosqueId = intent.getStringExtra("quiosqueId")
        firestore = FirebaseFirestore.getInstance()

        // Passa o ID da locação para a função que gera o QRCode
        val bitmap = generateQRCode(locacaoId ?: "")
        binding.imageViewQRCode.setImageBitmap(bitmap)

        // função para pegar o gerente do quiosque
        fetchGerenteFromFirestore(quiosqueId) { gerente ->
            if (gerente.isNotEmpty()) {
                Log.d("QRCodeActivity", "Gerente do local: $gerente")
                binding.tvGerente.visibility = View.VISIBLE
                binding.tvGerente.text = "Apresente esse QRcode para o Gerente: $gerente"
                // Aqui você pode fazer o que precisa com o gerente, como exibir em um TextView, etc.
            } else {
                Log.d("QRCodeActivity", "Gerente do local não encontrado")
            }
        }

        binding.btnVoltar.setOnClickListener {       // ao clicar no botão voltar, a activity HomeActivity é iniciada
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Funcao para gerar o QRCode
    private fun generateQRCode(text: String): Bitmap {
        Log.d("QRCodeActivity", "Generating QR Code for: $text")
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            return bmp
        } catch (e: Exception) {
            Log.e("QRCodeActivity", "Error generating QR Code", e)
            throw e
        }
    }


    private fun fetchGerenteFromFirestore(quiosqueId: String?, callback: (String) -> Unit) {
        if (quiosqueId != null) {
            firestore.collection("locais")
                .document(quiosqueId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val gerente = document.getString("gerente") ?: ""
                        callback(gerente)
                    } else {
                        Log.d("QRCodeActivity", "Documento do local não encontrado")
                        callback("")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRCodeActivity", "Erro ao obter o gerente do local", e)
                    callback("")
                }
        } else {
            Log.d("QRCodeActivity", "ID do quiosque não recebido")
            callback("")
        }
    }
}