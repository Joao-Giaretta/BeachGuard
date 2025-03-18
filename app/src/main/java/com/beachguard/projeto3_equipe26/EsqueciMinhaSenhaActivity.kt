package com.beachguard.projeto3_equipe26

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.databinding.ActivityEsqueciMinhaSenhaBinding
import com.google.firebase.auth.FirebaseAuth

class EsqueciMinhaSenhaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEsqueciMinhaSenhaBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        setupUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //função para configurar a interface do usuário
    private fun setupUI(){
        binding = ActivityEsqueciMinhaSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConfirmar.setOnClickListener{
            if(checkAllFields()){
                auth.sendPasswordResetEmail(binding.etEmail.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            //se o envio do e-mail for bem sucedido, exibe uma mensagem de sucesso
                            binding.tiEmail.error = null
                            binding.tiEmail.helperText = "E-mail enviado com sucesso"
                            //exibe um AlertDialog para informar o usuário que o e-mail foi enviado com sucesso
                            AlertDialog.Builder(this)
                                .setTitle("E-mail enviado com sucesso!")
                                .setMessage("Cheque sua caixa de entrada para redefinir sua senha.")
                                .setNeutralButton("Ok") { _, _ ->
                                    val intent = Intent(this, EntrarActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        } else {
                            //se o envio do e-mail falhar, exibe uma mensagem de erro
                            binding.tiEmail.error = "E-mail não cadastrado"
                        }
                    }
            }
        }
        binding.tvVoltar.setOnClickListener {
            val intent = Intent(this, EntrarActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    //função para verificar se todos os campos estão preenchidos corretamente
    private fun checkAllFields(): Boolean {
        val email = binding.etEmail.text.toString()
        if(binding.etEmail.text.toString().isEmpty()){
            binding.tiEmail.error = "Campo obrigatório"
            return false
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.tiEmail.error = "E-mail inválido"
            return false
        }
        return true
    }
}