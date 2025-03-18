package com.beachguard.projeto3_equipe26.gerente


import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivityGerenciarArmarioBinding

class GerenciarArmarioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGerenciarArmarioBinding

    private var locacaoId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setupUI()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locacaoId = intent.getStringExtra("locacaoId").toString()
    }

    private fun setupUI(){
        binding = ActivityGerenciarArmarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener {
            startActivity(Intent(this, HomeGerenteActivity::class.java))
            finish()
        }

        binding.btnAbrirArmario.setOnClickListener(){
            AlertDialog.Builder(this)
                .setTitle("Armário aberto!")
                .setMessage("Informe o cliente que o armário foi aberto e peça para que ele feche manualmente.")
                .setNeutralButton("Ok") { dialog, which ->
                    // Abrir armário
                }
                .show()
        }

        binding.btnFinalizarLocacao.setOnClickListener {
            intent = Intent(this, FinalizarLocacaoActivity::class.java)
            intent.putExtra("locacaoId", locacaoId)
            startActivity(intent)
        }
    }
}