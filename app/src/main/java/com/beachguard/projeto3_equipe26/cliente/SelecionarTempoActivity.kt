package com.beachguard.projeto3_equipe26.cliente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.databinding.ActivitySelecionarTempoBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SelecionarTempoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelecionarTempoBinding
    private lateinit var firestore: FirebaseFirestore

    // Variaveis para armazenar informações do quiosque
    private var quiosqueId: String? = ""
    private var quiosqueName: String? = ""
    private var quiosqueAddress: String? = ""
    private var quiosqueReference: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecionarTempoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Recuperar informações do quiosque mais próximo
        infoQuiosque()

        firestore = FirebaseFirestore.getInstance()

        // Obter os dados para os RadioButtons
        obterDadosRadioButton()

        // Exibe as informações do quiosque
        exibirInformacoesQuiosque()

        binding.btnConfirmar.setOnClickListener {
            btnConfirmar()
        }
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }
    private fun isWithinTimeRange(hour: Int, minute: Int, startTime: Double, endTime:Double): Boolean {
        val currentHourWithMinutes = hour + minute / 60.0
        return currentHourWithMinutes >= startTime && currentHourWithMinutes < endTime
    }
    private fun exibirInformacoesQuiosque(){
        binding.tvNomeQuiosque.text = quiosqueName
        binding.tvEndereco.text = quiosqueAddress
        binding.tvReferencia.text = quiosqueReference
    }
    private fun infoQuiosque(){
        quiosqueId = intent.getStringExtra("quiosqueId")
        quiosqueName = intent.getStringExtra("quiosqueName")
        quiosqueAddress = intent.getStringExtra("quiosqueAddress")
        quiosqueReference = intent.getStringExtra("quiosqueReference")
    }
    private fun obterDadosRadioButton(){
        // Consultar o Firestore para obter os dados para os RadioButtons
        firestore.collection("locais").document(quiosqueId!!).collection("planos").get()
            .addOnSuccessListener { result ->

                // Obter a hora atual
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                for (document in result) {
                    // Para cada documento, irá criar um RadioButton
                    val descricao = document.data["descricao"].toString()
                    val preco = document.data["preco"].toString()
                    val radioButton = RadioButton(this)
                    radioButton.text = "$descricao - R$ $preco"

                    // Definir os horários de acesso para cada plano
                    val startTime: Double
                    val endTime: Double
                    when (descricao) {
                        "30 Minutos" -> {
                            startTime = 7.0
                            endTime = 17.5
                        }
                        "1 Hora" -> {
                            startTime = 7.0
                            endTime = 17.0
                        }
                        "3 Horas" -> {
                            startTime = 7.0
                            endTime = 15.0
                        }
                        "Diária" -> {
                            startTime = 7.0
                            endTime = 8.0
                        }
                        else -> {
                            startTime = 0.0
                            endTime = 0.0
                        }
                    }

                    if (!isWithinTimeRange(hour, minute, startTime, endTime)){
                        radioButton.isEnabled = false
                        radioButton.text = "$descricao - R$ $preco - Somente acessível das: ${startTime.toInt()}:00 às ${
                            if (descricao == "30 Minutos") "17:30" else endTime.toInt().toString() + ":00"
                        }"
                    }

                    binding.radioGroup.addView(radioButton)
                    Log.d("TAG", "RadioButton adicionado: $descricao - R$ $preco")
                }
            }
            .addOnFailureListener { exception ->
                // Exibir mensagem de erro se a consulta falhar
                Toast.makeText(this@SelecionarTempoActivity, "Erro ao consultar o Firestore", Toast.LENGTH_SHORT).show()
                Log.e("TAG", "Erro ao consultar o Firestore", exception)
            }
    }
    private fun btnConfirmar(){
        // Obter o ID do Radio Button
        val selectedRadioButtonID = binding.radioGroup.checkedRadioButtonId

        // Verificar se algum Radio Button foi selecionado
        if (selectedRadioButtonID != -1) {
            // Obtem o radio selecionado
            val radioButton = findViewById<RadioButton>(selectedRadioButtonID)
            val selectedText = radioButton.text.toString()

            // Configura o Intent para passar a informação selecionada para a próxima Activity
            val intent = Intent(this@SelecionarTempoActivity, ConfirmarLocacaoActivity::class.java)
            intent.putExtra("selectedText", selectedText)
            intent.putExtra("quiosqueId", quiosqueId)
            intent.putExtra("quiosqueName", quiosqueName)
            intent.putExtra("quiosqueAddress", quiosqueAddress)
            intent.putExtra("quiosqueReference", quiosqueReference)
            startActivity(intent)
        } else
        // Exibir mensagem de erro se nenhum RadioButton for selecionado
            Toast.makeText(this@SelecionarTempoActivity, "Selecione um tempo de locação", Toast.LENGTH_SHORT).show()
    }
}