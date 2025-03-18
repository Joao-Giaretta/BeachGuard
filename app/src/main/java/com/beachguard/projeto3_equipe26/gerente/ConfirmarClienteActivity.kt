package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityConfirmarClienteBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ConfirmarClienteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmarClienteBinding
    private lateinit var db: FirebaseFirestore

    private var locacaoId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConfirmarClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // Receber o ID da locação
        //locacaoId = "BGbv4qxhWuXliuSfHBdY" foi enviado dessa forma na entrega 2
        locacaoId = intent.getStringExtra("locacaoId").toString()

        // Obter dados do cliente
        obterDadosCliente(locacaoId)

        locacaoId = intent.getStringExtra("locacaoId").toString()

        binding.btnProsseguir.setOnClickListener(){
            // Ir para a tela de gerenciar armário
            val intent = Intent(this, GerenciarArmarioActivity::class.java)
            intent.putExtra("locacaoId", locacaoId)
            startActivity(intent)
            finish()
        }

        binding.btnVoltar.setOnClickListener {
            // Voltar para a tela inicial
            val intent = Intent(this, HomeGerenteActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun obterDadosCliente(locacaoId: String) {
        // Obter dados do cliente
        db.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val clientId = document.getString("email")
                    if (clientId != null) {
                        db.collection("users").document(clientId).get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val nome = document.getString("nome")
                                    if (nome != null) {
                                        binding.tvNome.text = "Nome do Cliente: $nome"
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.w("ConfirmarClienteActivity", "Erro ao encontrar cliente", it)
                            }
                    }
                    val foto = document.getString("imageUrl")
                    if (foto != null) {
                        // Carregar a imagem do cliente
                        Glide.with(this)
                            .load(foto)
                            .into(binding.imageView)
                    }
                    // obter número do armário
                    val quiosqueId = document.getString("quiosqueId")
                    val armarioId = document.getString("armarioId")
                    if (armarioId != null){
                        if (quiosqueId != null) {
                            db.collection("locais").document(quiosqueId).collection("armarios").document(armarioId).get()
                                .addOnSuccessListener { document ->
                                    if (document != null) {
                                        val numero = document.getString("numero")
                                        if (numero != null) {
                                            binding.tvNumeroArmario.text = "Armário Número: $numero"
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Log.w("ConfirmarClienteActivity", "Erro ao encontrar armário", it)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("ConfirmarClienteActivity", "Erro ao encontrar locação", e)
            }
    }
}