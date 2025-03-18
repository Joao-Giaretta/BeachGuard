package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityConfirmarBothClientesBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ConfirmarBothClientesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmarBothClientesBinding
    private lateinit var db: FirebaseFirestore
    private var locacaoId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupUI()
        supportActionBar?.hide()

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // Receber o ID da locação
        locacaoId = intent.getStringExtra("locacaoId").toString()

        // Obter dados do cliente
        obterDadosCliente(locacaoId)
    }

    private fun setupUI(){
        // Inflar o layout
        binding = ActivityConfirmarBothClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener {
            // Voltar para a tela inicial
            val intent = Intent(this, HomeGerenteActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnProsseguir.setOnClickListener(){
            // Ir para a tela de gerenciar armário
            val intent = Intent(this, GerenciarArmarioActivity::class.java)
            intent.putExtra("locacaoId", locacaoId)
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
                                        binding.tvNome.text = nome
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.w("ConfirmarClienteActivity", "Erro ao encontrar cliente", it)
                            }
                    }
                    val image1 = document.getString("imageUrl1")
                    val image2 = document.getString("imageUrl2")
                    if (image1 != null && image2 != null) {
                        // Carregar a imagem do cliente
                        Glide.with(this)
                            .load(image1)
                            .into(binding.imageView1)

                        Glide.with(this)
                            .load(image2)
                            .into(binding.imageView2)
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