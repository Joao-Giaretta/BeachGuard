package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.PagamentoActivity
import com.beachguard.projeto3_equipe26.components.Cartao
import com.beachguard.projeto3_equipe26.components.CartoesRadioAdapter
import com.beachguard.projeto3_equipe26.databinding.ActivitySelecionarCartaoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelecionarCartaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelecionarCartaoBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: CartoesRadioAdapter
    private lateinit var auth: FirebaseAuth

    // Variaveis para armazenar informações do quiosque
    private var quiosqueId: String? = ""
    private var selectedText: String? = ""
    private var valorDiaria: String? = ""

    override fun onResume() {
        super.onResume()
        obterCartoes()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecionarCartaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        quiosqueId = intent.getStringExtra("quiosqueId")
        selectedText = intent.getStringExtra("selectedText")

        // Carrega os cartões
        obterCartoes()

        // Configura o botão para adicionar cartão
        binding.btnAddCartao.setOnClickListener {
            val intent = Intent(this, PagamentoActivity::class.java)
            startActivity(intent)
        }
        binding.btnAlugar.setOnClickListener(){
            salvarLocacao()
        }
        // Configura o botão para voltar
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun obterCartoes(){
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        if (userEmail != null){
            // limpar a lista de radiobuttons
            binding.radioGroupCartoes.removeAllViews()
            // Recuperando os dados do firestore
            CoroutineScope(Dispatchers.IO).launch {
                firestore.collection("users").document(userEmail).collection("cartoes").get()
                    .addOnSuccessListener { result ->
                        for (document in result){
                            val cartao = document.toObject(Cartao::class.java)
                            adicionarCartaoAoRadioGroup(cartao)
                        }

                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this@SelecionarCartaoActivity, "Erro ao recuperar os cartões", Toast.LENGTH_SHORT).show()
                    }
            }

        } else{
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun adicionarCartaoAoRadioGroup(cartao: Cartao) {
        // Verifica se o cartão já foi adicionado ao RadioGroup
        var cartaoJaAdicionado = false
        for (i in 0 until binding.radioGroupCartoes.childCount) {
            val radioButton = binding.radioGroupCartoes.getChildAt(i) as? RadioButton
            val cartaoExistente = radioButton?.tag as? Cartao
            if (cartaoExistente != null && cartaoExistente == cartao) {
                cartaoJaAdicionado = true
                break
            }
        }
        // Se o cartão não foi adicionado, adiciona-o ao RadioGroup
        if (!cartaoJaAdicionado) {
            val radioButton = RadioButton(this)
            radioButton.text = "${cartao.cardNumber} - ${cartao.cardName}"
            radioButton.tag = cartao
            binding.radioGroupCartoes.addView(radioButton)
        }
    }
    private fun salvarLocacao(){
        val user = auth.currentUser
        val userEmail = user?.email

        if (userEmail != null){
            // Verifica se algum Radio Button foi selecionado
            val selectedRadioButtonId = binding.radioGroupCartoes.checkedRadioButtonId
            if (selectedRadioButtonId != -1){
                // recupera o RadioButton selecionado
                val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
                // recupera o Cartao associado ao RadioButton
                val selectedCartao = selectedRadioButton.tag as Cartao
                // Obtém o número do cartão
                val numeroCartao = selectedCartao?.cardNumber ?: ""
                // obter o valor da diária
                quiosqueId?.let {
                    firestore.collection("locais").document(it).collection("planos").document("4").get()
                        .addOnSuccessListener { document ->
                            valorDiaria = document.getString("preco")
                            val locacao = hashMapOf(
                                "email" to userEmail,
                                "quiosqueId" to quiosqueId,
                                "status" to "Pendente",
                                "plano" to selectedText,
                                "cartao" to numeroCartao,
                                "dataInicio" to null,
                                "dataTermino" to null,
                                "valorDiaria" to valorDiaria
                            )
                            firestore.collection("locacoes")
                                .add(locacao)
                                .addOnSuccessListener { documentReference ->
                                    // Sucesso ao adicionar a locação
                                    val locacaoId = documentReference.id
                                    val intent = Intent(this, QRCodeActivity::class.java)
                                    intent.putExtra("locacaoId", locacaoId)
                                    intent.putExtra("quiosqueId", quiosqueId)
                                    Log.i("SelecionarCartaoActivity", "Locação adicionada com sucesso: $locacaoId")
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    // Falha ao adicionar a locação
                                    Toast.makeText(this, "Erro ao adicionar a locação", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao obter o valor da diária", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                // Exibir mensagem de erro se nenhum RadioButton for selecionado
                Toast.makeText(this, "Selecione um cartão", Toast.LENGTH_SHORT).show()
            }
        }
    }
}