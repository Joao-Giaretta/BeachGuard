package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.EntrarActivity
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivityHomeGerenteBinding
import com.google.firebase.auth.FirebaseAuth

class HomeGerenteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeGerenteBinding
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

    private fun setupUI() {
        // Inicializa a ViewBinding
        binding = ActivityHomeGerenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSair.setOnClickListener {
            // Mostra um AlertDialog para confirmar a saída
            showAlertDialog()
        }
        binding.btnLiberarLoc.setOnClickListener {
            // Redireciona para a tela de leitura do QR Code
            val intent = Intent(this, LeitorQRCodeActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnAbrirArmario.setOnClickListener {
            // Redireciona para a tela de leitura da pulseira
            val intent = Intent(this, LerPulseiraActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun showAlertDialog(){
        // Mostra um AlertDialog para confirmar a saída
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("Sim") { _, _->
                auth.signOut()
                Toast.makeText(baseContext, "Deslogado com sucesso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, EntrarActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Não", null)
            .show()
    }
}