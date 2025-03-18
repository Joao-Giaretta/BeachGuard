package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.beachguard.projeto3_equipe26.PagamentoActivity
import com.beachguard.projeto3_equipe26.components.Cartao
import com.beachguard.projeto3_equipe26.components.CartoesListAdapter
import com.beachguard.projeto3_equipe26.components.OnDeleteCard
import com.beachguard.projeto3_equipe26.databinding.ActivityCartoesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CartoesActivity : AppCompatActivity(), OnDeleteCard {

    private lateinit var binding: ActivityCartoesBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: CartoesListAdapter
    private var fromProfileScreen: Boolean = false

    override fun onResume() {
        super.onResume()
        // Carrega a lista de cartões ao retomar a activity
        obterCartoes()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartoesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Verifica se a activity foi acessada a partir da tela de perfil pessoal
        fromProfileScreen = intent.getBooleanExtra("from_profile_screen", false)

        // Obtem instancias do Firestore
        firestore = FirebaseFirestore.getInstance()

        // Configurando o RecyclerView
        adapter = CartoesListAdapter(listOf(), this) // Começa como um lista vazia
        binding.rvCartoes.apply {
            layoutManager = LinearLayoutManager(this@CartoesActivity)
            this.adapter = adapter
        }

        // Carrega a lista de cartões ao criar a activity
        obterCartoes()

        binding.btnAddCartao.setOnClickListener {
            // Ao clicar no botão "Adicionar cartão", a activity PagamentoActivity é iniciada
            val intent = Intent(this, PagamentoActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão "Voltar"
        binding.btnVoltar.setOnClickListener {
            if (fromProfileScreen) {
                // Se a activity foi acessada a partir da tela de perfil pessoal, encerra a activity
                finish()
            } else {
                finish()
            }
        }
    }

    private fun obterCartoes(){
        // obtém o usuário autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        // obtém o email do usuário autenticado
        val userEmail = currentUser?.email
        if (userEmail != null){
            // Recuperando os dados do firestore
            CoroutineScope(Dispatchers.IO).launch {
                firestore.collection("users").document(userEmail).collection("cartoes").get()
                    .addOnSuccessListener { result ->
                        val listCartoes = mutableListOf<Cartao>()
                        for (document in result) {
                            val cartao = document.toObject(Cartao::class.java)
                            listCartoes.add(cartao)
                        }
                        // Atualiza o RecyclerView com a lista de cartões
                        val itemAdapter = CartoesListAdapter(listCartoes, this@CartoesActivity)
                        binding.rvCartoes.adapter = itemAdapter
                    }
                    .addOnFailureListener { _ ->
                        Toast.makeText(this@CartoesActivity, "Erro ao recuperar os cartões", Toast.LENGTH_SHORT).show()
                    }
            }

        } else{
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun delete(card: Cartao) {
        // Exibe um AlertDialog para confirmar a exclusão do cartão
        AlertDialog.Builder(this)
            .setTitle("Excluir cartão")
            .setMessage("Deseja excluir o cartão ?")
            .setPositiveButton("Sim") { _, _ ->
                // Remove o cartão do Firestore
                Log.d("CartoesActivity", "Removendo cartão: $card")
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userEmail = currentUser?.email
                if (userEmail != null) {
                    firestore.collection("users").document(userEmail)
                        .collection("cartoes")
                        .whereEqualTo("cardName", card.cardName)
                        .whereEqualTo("cardNumber", card.cardNumber)
                        .whereEqualTo("dataValidade", card.dataValidade)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                // Exclui o documento do cartão
                                document.reference.delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this@CartoesActivity, "Cartão excluído com sucesso", Toast.LENGTH_SHORT).show()
                                        obterCartoes() // Atualiza a lista após a exclusão
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@CartoesActivity, "Erro ao excluir o cartão", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@CartoesActivity, "Erro ao excluir o cartão", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Não"){ dialog, _ ->
                // Fecha o AlertDialog se o usuário clicar em "Não"
                dialog.dismiss()
            }
            .create()
            .show()
    }
}