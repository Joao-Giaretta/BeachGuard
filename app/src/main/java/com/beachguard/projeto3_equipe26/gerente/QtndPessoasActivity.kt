package com.beachguard.projeto3_equipe26.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityQtndPessoasBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class QtndPessoasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQtndPessoasBinding
    private lateinit var db: FirebaseFirestore
    private var locacaoId: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQtndPessoasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        locacaoId = intent.getStringExtra("locacaoId")

        db = FirebaseFirestore.getInstance()

        // Verifica se há armários disponíveis para a locação
        locacaoId?.let {
            verificarArmarioDisponivel(it) { armarioId ->
                if (armarioId != null) {
                    // Há armários disponíveis
                    Log.d("QtndPessoasActivity", "Armário disponível: $armarioId")
                    Toast.makeText(this, "ID do armário disponível: $armarioId", Toast.LENGTH_SHORT).show()

                    binding.btnUmaPessoa.setOnClickListener {
                        // Solicitar permissão para acessar a câmera
                        requestCameraPermission("Uma Pessoa", armarioId)
                    }

                    binding.btnDuasPessoas.setOnClickListener {
                        // Solicitar permissão para acessar a câmera
                        requestCameraPermission("Duas Pessoas", armarioId)
                    }
                } else {
                    // Não há armários disponíveis
                    cancelarLocacao(it)
                    Toast.makeText(this, "Nenhum armário disponível", Toast.LENGTH_SHORT).show()
                    val i = Intent(this, HomeGerenteActivity::class.java)
                    startActivity(i)
                    finish()
                }

            }
        }

        binding.btnVoltar.setOnClickListener {
            // Cancelar a locação
            val i = Intent(this, HomeGerenteActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    private fun requestCameraPermission(option: String, armarioId: String?) {
        // Solicitar permissão para acessar a câmera
        cameraProviderResult.launch(android.Manifest.permission.CAMERA)
        // Salva a opção selecionada para uso posterior
        selectedOption = option
        selectedArmarioId = armarioId
    }

    private val cameraProviderResult =
        // Resultado da solicitação de permissão
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Verifica a opção selecionada
                when (selectedOption) {
                    "Uma Pessoa" -> abrirTelaDePreviewForOne(selectedArmarioId)
                    "Duas Pessoas" -> abrirTelaDePreviewForTwo(selectedArmarioId)
                }
            } else {
                // Mostrar mensagem de erro
                Snackbar.make(binding.root, "Você não concedeu permissões para usar a câmera.", Snackbar.LENGTH_SHORT).show()
            }
        }

    private fun abrirTelaDePreviewForOne(armarioId: String?) {
        // Abre a tela de preview da câmera para uma pessoa
        val intent = Intent(this, CameraPreviewActivity::class.java)
        intent.putExtra("locacaoId", locacaoId)
        intent.putExtra("armarioId", armarioId)
        intent.putExtra("quantidadeFotos", 1)
        startActivity(intent)
    }

    private fun abrirTelaDePreviewForTwo(armarioId: String?) {
        // Abre a tela de preview da câmera para duas pessoas
        val intent = Intent(this, CameraPreviewActivity::class.java)
        intent.putExtra("locacaoId", locacaoId)
        intent.putExtra("armarioId", armarioId)
        intent.putExtra("quantidadeFotos", 2)
        startActivity(intent)
    }

    private fun verificarArmarioDisponivel(locacaoId: String, callback: (String?) -> Unit) {
        // Verifica se há armários disponíveis para a locação
        // Se houver, chama o callback com true, senão, chama com false
        // Recupera o id do quiosque da locação
        Log.i("QtndPessoasActivity", "Verificando armários disponíveis para a locação $locacaoId")
        db.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                val quiosqueId = document.getString("quiosqueId")
                Log.i("QtndPessoasActivity", "QuiosqueId: $quiosqueId")
                if (quiosqueId != null) {
                    // Verificar se há armários disponíveis no quiosque
                    Log.i("QtndPessoasActivity", "Verificando armários disponíveis no quiosque $quiosqueId")
                    db.collection("locais").document(quiosqueId).collection("armarios")
                        .whereEqualTo("disponivel", true)
                        .get()
                        .addOnSuccessListener { armarios ->
                            if (!armarios.isEmpty) {
                                // Não há armários disponíveis
                                val armarioId = armarios.documents[0].id
                                callback(armarioId)
                            } else {
                                // Há armários disponíveis
                                callback(null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("QntdPessoasActivity", "Erro ao verificar armários disponíveis", exception)
                            callback(null)
                        }
                } else {
                    Log.w("QntdPessoasActivity", "QuiosqueId não encontrado na locação")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("QntdPessoasActivity", "Erro ao recuperar locação", exception)
                callback(null)
            }
    }

    private fun cancelarLocacao(locacaoId: String) {
        // Cancela a locação
        db.collection("locacoes").document(locacaoId)
            .update("status", "Cancelada")
            .addOnSuccessListener {
                Log.i("QtndPessoasActivity", "Locação cancelada com sucesso")
            }
            .addOnFailureListener { exception ->
                Log.w("QtndPessoasActivity", "Erro ao cancelar locação", exception)
            }
    }

    companion object {
        // Variáveis estáticas para armazenar a opção selecionada e o ID do armário
        var selectedOption: String? = null
        var selectedArmarioId: String? = null
    }
}