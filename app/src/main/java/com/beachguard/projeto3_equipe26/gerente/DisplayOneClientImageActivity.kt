package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityDisplayOneClientImageBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DisplayOneClientImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayOneClientImageBinding
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayOneClientImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // Receber o ID da locação e do armário
        val locacaoId = intent.getStringExtra("locacaoId")
        val armarioId = intent.getStringExtra("armarioId")

        // Receber a imagem do cliente
        val imageUrl = intent.getStringExtra("image_url")

        // Carregar a imagem do cliente
        Glide.with(this)
            .load(imageUrl)
            .into(binding.imageView)

        // Atualizar a locação com a imagem do cliente e o armário
        binding.btnAlugar.setOnClickListener {
            if (locacaoId != null && armarioId != null && imageUrl != null) {
                atualizarLocacao(locacaoId, imageUrl, armarioId) { sucesso ->
                    if (sucesso) {
                        // Ir para a tela de vincular pulseira com a locação
                        val i = Intent(this, VincularPulseiraActivity::class.java).apply {
                            putExtra("locacaoId", locacaoId)
                            putExtra("qtdPessoas", 1)
                        }
                        Log.i("DisplayOneClientImageActivity", "Indo para DevolveInfoActivity")
                        startActivity(i)
                        finish()
                    } else {
                        Toast.makeText(this, "Erro ao atualizar locação", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Dados insuficientes", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVoltar.setOnClickListener {
            // Deletar a imagem do cliente
            imageUrl?.let { url ->
                deleteImageFromFirebase(url)
            }
            finish()
        }
    }
    private fun atualizarLocacao(locacaoId: String, imageUrl: String?, armarioId: String, callback: (Boolean) -> Unit){
        // Referência para o documento da locação
        val locacaoRef = db.collection("locacoes").document(locacaoId)

        // Obtem a data e hora atual
        val dataHoraAtual = FieldValue.serverTimestamp()

        // Prepara os dados para a atualização
        val dados = hashMapOf(
            "imageUrl" to imageUrl,
            "dataInicio" to dataHoraAtual,
            "status" to "Em uso",
            "armarioId" to armarioId
        )

        // Atualiza o documento da locação
        locacaoRef.update(dados)
            .addOnSuccessListener {
                Log.i("DisplayOneClientImagesActivity", "Documento da locação atualizado com sucesso")
                obterIdQuiosque(locacaoId, armarioId)
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("DisplayOneClientImagesActivity", "Erro ao atualizar documento da locação", e)
                callback(false)
            }
    }

    private fun atualizarStatusArmario(armarioId: String, quiosqueId: String){
        // Atualiza o status do armário para indisponível
        db.collection("locais").document(quiosqueId).collection("armarios").document(armarioId)
            .update("disponivel", false)
            .addOnSuccessListener {
                Log.i("DisplayOneClientImagesActivity", "Status do armário atualizado com sucesso")
            }
            .addOnFailureListener { e ->
                Log.w("DisplayOneClientImagesActivity", "Erro ao atualizar status do armário", e)
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