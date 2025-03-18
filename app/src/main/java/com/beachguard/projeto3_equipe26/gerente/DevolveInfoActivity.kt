package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.beachguard.projeto3_equipe26.databinding.DevolveInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*


class DevolveInfoActivity : AppCompatActivity() {

    private lateinit var binding: DevolveInfoBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var locacaoId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DevolveInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        Log.i("DevolveInfoActivity", "Iniciando DevolveInfoActivity")

        // Inicializa o Firestore
        firestore = FirebaseFirestore.getInstance()
        // Inicializa o FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Obter o ID da locação
        locacaoId = intent.getStringExtra("locacaoId")

        // Verifique se locacaoId não é nulo e obtenha os dados necessários
        if (locacaoId != null) {
            obterdados(locacaoId!!)
            obterDadosLocacao(locacaoId!!)
        } else {
            Toast.makeText(this, "Erro ao obter o ID da locação", Toast.LENGTH_SHORT).show()
        }

        // Configura o botão para voltar
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomeGerenteActivity::class.java))
            finish()
        }
    }

    private fun obterdados(locacaoId: String){
        // Obter o email do cliente que fez a locação
        firestore.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    // obtem o email do cliente que fez a locação
                    val clienteEmail = document.getString("email")
                    // obtem o nome do Cliente
                    firestore.collection("users").document(clienteEmail!!).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()){
                                val nomeCliente = document.getString("nome")
                                binding.tvNome.text = "Cliente: $nomeCliente"
                            }
                        }
                }
            }
    }

    private fun obterDadosLocacao(locacaoId: String){
        // Obter os dados da locação
        firestore.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    // obtem os dados necessários da locação
                    val dataInicioTimestamp = document.getTimestamp("dataInicio")
                    val armarioId = document.getString("armarioId")
                    val plano = document.getString("plano")
                    val quiosqueId = document.getString("quiosqueId")

                    // Obter número do armário
                    if (armarioId != null) {
                        obterNumArmario(armarioId, quiosqueId!!)
                    }

                    // Converter o timestamp para uma data
                    val dataInicio = dataInicioTimestamp?.toDate()

                    // Exibir os dados da locação na tela
                    val dataInicioStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(dataInicio)
                    binding.tvDataInicio.text = "Data de início: $dataInicioStr"
                    binding.tvPlano.text = "Plano: $plano"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DevolveInfoActivity", "Erro ao obter dados da locação", exception)
            }
    }

    private fun obterNumArmario(armarioId: String, quiosqueId: String){
        // Obter o número do armário
        firestore.collection("locais").document(quiosqueId).collection("armarios").document(armarioId).get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    // Obter o número do armário
                    val numArmario = document.getString("numero")
                    binding.tvNumArmario.text = "Armário: $numArmario"
                }
            }
    }
}

