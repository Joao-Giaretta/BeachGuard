package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityConfirmarLocacaoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConfirmarLocacaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmarLocacaoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Variaveis para armazenar informações do quiosque
    private var quiosqueId: String? = ""
    private var quiosqueName: String? = ""
    private var quiosqueAddress: String? = ""
    private var quiosqueReference: String? = ""
    private var selectedText: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmarLocacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o Firestore e Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Recuperar informações do quiosque mais próximo
        quiosqueId = intent.getStringExtra("quiosqueId")
        quiosqueName = intent.getStringExtra("quiosqueName")
        quiosqueAddress = intent.getStringExtra("quiosqueAddress")
        quiosqueReference = intent.getStringExtra("quiosqueReference")
        selectedText = intent.getStringExtra("selectedText")

        // Exibir tempo de locação selecionado
        binding.tvTempoLocacao.text = selectedText

        // Exibir informações do quiosque
        exibirInformaçõesQuiosque()

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnAlugar.setOnClickListener {
            // Direciona para a tela de Selecionar Cartão
            val intent = Intent(this, SelecionarCartaoActivity::class.java)
            intent.putExtra("quiosqueId", quiosqueId)
            intent.putExtra("selectedText", selectedText)
            startActivity(intent)
        }
    }
    private fun exibirInformaçõesQuiosque(){
        // Exibir informações do quiosque
        binding.tvNomeQuiosque.text = quiosqueName
        binding.tvEndereco.text = quiosqueAddress
        binding.tvReferencia.text = quiosqueReference
    }

}