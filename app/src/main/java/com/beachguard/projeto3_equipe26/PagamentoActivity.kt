package com.beachguard.projeto3_equipe26

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import com.beachguard.projeto3_equipe26.databinding.ActivityPagamentoBinding
import com.braintreepayments.cardform.view.CardForm
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


private lateinit var binding: ActivityPagamentoBinding
private lateinit var auth: FirebaseAuth
private lateinit var firestore: FirebaseFirestore


class PagamentoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPagamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o Firestore e Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configura o formulário de cartão
        binding.cardForm.cardRequired(true)
            .expirationRequired(true)
            .cvvRequired(true)
            .cardholderName(CardForm.FIELD_REQUIRED)
            .actionLabel("Purchase")
            .setup(this@PagamentoActivity)

        // Configura o campo de CVV para aceitar apenas números
        binding.cardForm.cvvEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        binding.btnAdd.setOnClickListener {
            addCartao()
        }
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }
    // Função para adicionar cartão
    private fun addCartao(){
        // Recupera o usuário logado
        val user = auth.currentUser
        if (user != null){
            // Recupera o email do usuário
            val userEmail = user.email
            if (userEmail != null){
                // Recupera os dados do cartão
                val cardNumber = binding.cardForm.cardNumber
                val dataValidade = binding.cardForm.expirationMonth + "/" + binding.cardForm.expirationYear
                val cardName = binding.cardForm.cardholderName
                // Cria um HashMap com os dados do cartão
                val cardDetails = hashMapOf(
                    "cardNumber" to cardNumber,
                    "dataValidade" to dataValidade,
                    "cardName" to cardName
                )
                // Adiciona o cartão ao Firestore
                firestore.collection("users").document(userEmail).collection("cartoes").add(cardDetails)
                    .addOnSuccessListener {
                        Toast.makeText(this@PagamentoActivity, "Cartão adicionado com sucesso", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this@PagamentoActivity, "Erro ao adicionar o cartão", Toast.LENGTH_SHORT).show()
                    }
            }
            // Fecha a tela
            finish()
        }
    }
}