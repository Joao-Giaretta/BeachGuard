package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DirecionarParaUmOuDoisClientesActivity : AppCompatActivity() {
    private lateinit var db : FirebaseFirestore
    private var locacaoId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()
        // Receber o ID da locação (você pode passar isso através de Intent)
        locacaoId = intent.getStringExtra("locacaoId").toString()

        // Verificar a quantidade de imagens e redireciona para a tela correta
        verificarQuantidadeImagens(locacaoId)
    }
    private fun verificarQuantidadeImagens(locacaoId: String) {
        // Verificar se a locação possui uma ou duas imagens
        db.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val image = document.getString("imageUrl")
                    if (image != null) {
                        // Redirecionar para ConfirmarClienteActivity
                        val intent = Intent(this, ConfirmarClienteActivity::class.java)
                        intent.putExtra("locacaoId", locacaoId)
                        startActivity(intent)
                        finish()
                    } else {
                        // Redirecionar para ConfirmarBothClientesActivity
                        val intent = Intent(this, ConfirmarBothClientesActivity::class.java)
                        intent.putExtra("locacaoId", locacaoId)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.w("DecisaoConfirmacaoActivity", "Erro ao encontrar locação")
                    // Finaliza a atividade atual, pois não será mais necessária.
                    finish()
                }
                // Finaliza a atividade atual, pois não será mais necessária.
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("DecisaoConfirmacaoActivity", "Erro ao encontrar locação", e)
                // Finaliza a atividade atual, pois não será mais necessária.
                finish()
            }
    }
}