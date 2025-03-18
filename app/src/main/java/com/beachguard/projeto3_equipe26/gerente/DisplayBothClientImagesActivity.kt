package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityDisplayBothClientImagesBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DisplayBothClientImagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayBothClientImagesBinding
    private lateinit var db: FirebaseFirestore
    private var locacaoId: String? = ""
    private var armarioId: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayBothClientImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // Pega os IDs da locação e do armário
        locacaoId = intent.getStringExtra("locacaoId")
        armarioId = intent.getStringExtra("armarioId")

        // Get image URLs from intent
        val imageUrl1 = intent.getStringExtra("imageUrl1")
        Log.d("DisplayBothClientImagesActivity", "imageUrl1: $imageUrl1")
        val imageUrl2 = intent.getStringExtra("imageUrl2")
        Log.d("DisplayBothClientImagesActivity", "imageUrl2: $imageUrl2")

        // Carregar imagens dos clientes nos ImageViews
        Glide.with(this)
            .load(imageUrl1)
            .into(binding.imageView1)

        Glide.with(this)
            .load(imageUrl2)
            .into(binding.imageView2)


        binding.btnAlugar.setOnClickListener {
            // Verifica se os IDs da locação e do armário não são nulos
            if (locacaoId != null && armarioId != null && imageUrl1 != null && imageUrl2 != null) {
                // Atualiza a locação com as URLs das imagens e o ID do armário
                atualizarLocacao(locacaoId!!, imageUrl1, imageUrl2, armarioId!!) { sucesso ->
                    if (sucesso) {
                        val i = Intent(this, VincularPulseiraActivity::class.java).apply {
                            putExtra("locacaoId", locacaoId)
                            putExtra("qtdPessoas", 2)
                        }
                        Log.i("DisplayBothClientImageActivity", "Indo para DevolveInfoActivity")
                        startActivity(i)
                        finish()
                    } else {
                        Toast.makeText(this, "Falha ao atualizar a locação", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Dados insuficientes", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnVoltar.setOnClickListener {
            // Deleta as imagens do Firebase Storage
            imageUrl1?.let { url ->
                deleteImageFromFirebase(url)
            }
            imageUrl2?.let { url ->
                deleteImageFromFirebase(url)
            }
            finish()
        }
    }

    private fun atualizarLocacao(locacaoId: String, imageUrl1: String?, imageUrl2: String?, armarioId: String, callback: (Boolean) -> Unit) {
        // Referência para o documento da locação
        val locacaoRef = db.collection("locacoes").document(locacaoId)

        // Obtem a data e hora atual
        val dataHoraAtual = FieldValue.serverTimestamp()

        // Prepara os dados para a atualização
        val dados = hashMapOf(
            "imageUrl1" to imageUrl1,
            "imageUrl2" to imageUrl2,
            "dataInicio" to dataHoraAtual,
            "status" to "Em uso",
            "armarioId" to armarioId
        )

        // Atualiza o documento da locação
        locacaoRef.update(dados)
            .addOnSuccessListener {
                Log.d("DisplayBothClientImagesActivity", "Documento da locação atualizado com sucesso")
                obterIdQuiosque(locacaoId, armarioId)
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("DisplayBothClientImagesActivity", "Erro ao atualizar documento da locação", e)
                callback(false)
            }
    }

    private fun atualizarStatusArmario(armarioId: String, quiosqueId: String){
        // Atualiza o status do armário para indisponível
        db.collection("locais").document(quiosqueId).collection("armarios").document(armarioId)
            .update("disponivel", false)
            .addOnSuccessListener {
                Log.i("DisplayBothClientImagesActivity", "Status do armário atualizado com sucesso")
            }
            .addOnFailureListener { e ->
                Log.w("DisplayBothClientImagesActivity", "Erro ao atualizar status do armário", e)
            }
    }
    private fun obterIdQuiosque(locacaoId: String, armarioId: String){
        // Obtem o ID do quiosque a partir do ID da locação
        db.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    val quiosqueId = document.getString("quiosqueId")
                    if (quiosqueId != null) {
                        atualizarStatusArmario(armarioId, quiosqueId)
                    }
                }
            }
    }

    private fun deleteImageFromFirebase(imageUrl: String){
        // Deleta a imagem do Firebase Storage
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageRef.delete()
            .addOnSuccessListener {
                Log.i("DisplayOneClientImagesActivity", "Imagem deletada com sucesso")
            }
            .addOnFailureListener { e ->
                Log.w("DisplayOneClientImagesActivity", "Erro ao deletar imagem", e)
            }
    }
}