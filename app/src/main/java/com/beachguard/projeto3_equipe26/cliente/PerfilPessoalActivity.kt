package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.EntrarActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityPerfilPessoalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilPessoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilPessoalBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilPessoalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Obtem instancias do Firestore e do FirebaseAuth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Obtem dados do usuário
        obterDadosUsuario()

        // ir para tela de cartões
        binding.btnCartoes.setOnClickListener {
            val intent = Intent(this@PerfilPessoalActivity, CartoesActivity::class.java)
            intent.putExtra("from_profile_screen", true)
            startActivity(intent)

        }

        binding.btnSair.setOnClickListener {
            // Mostra dialogo de confirmação
            showAlertDialog()}

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }
    private fun showAlertDialog(){
        // Cria um dialogo de Alerta para confirmar se o usuário deseja sair
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("Sim") { dialog, which ->
                auth.signOut()
                Toast.makeText(baseContext, "Deslogado com sucesso", Toast.LENGTH_SHORT).show()
                // Redireciona para a tela de Login
                val intent = Intent(this, EntrarActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    private fun obterDadosUsuario(){
        // Obter o ID do usuário logado
        val user = auth.currentUser
        // Verificar se o usuário está logado
        if (user != null){
            // Obtém o email do usuário logado
            val userEmail = user.email
            if (userEmail != null){
                // consultar o Firestore para obter os dados do usuário logado
                firestore.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()){
                            // Obter os dados do usuário
                            val nome = document.getString("nome")
                            val email = document.getString("email")
                            val cpf = document.getString("cpf")
                            Log.d("TAG", "Dados do usuário: nome=$nome, email=$email, cpf=$cpf")

                            // Exibir os dados do usuário na tela
                            binding.tvNome.text = "Nome: $nome"
                            binding.tvEmail.text = "E-mail: $email"
                            binding.tvCpf.text = "CPF: $cpf"
                        } else {
                            // Se o documento não existir, exibir uma mensagem de erro
                            Log.d("TAG", "Documento não encontrado para o usuário $userEmail")
                            Toast.makeText(this@PerfilPessoalActivity, "Dados não encontrados", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Se ocorrer um erro, exibir uma mensagem de erro
                        Log.e("TAG", "Erro ao obter os dados do usuário", exception)
                        Toast.makeText(this@PerfilPessoalActivity, "Erro ao obter os dados do usuário", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Se o email do usuário for nulo, exibir uma mensagem de erro
                Log.d("TAG", "Email do usuário nulo")
                Toast.makeText(this@PerfilPessoalActivity, "Email do usuário nulo", Toast.LENGTH_SHORT).show()
            }
        }else {
            // Se o usuário não estiver logado, exibir uma mensagem de erro
            Log.d("TAG", "Usuário não logado")
            Toast.makeText(this@PerfilPessoalActivity, "Usuário não logado", Toast.LENGTH_SHORT).show()
        }
    }
}