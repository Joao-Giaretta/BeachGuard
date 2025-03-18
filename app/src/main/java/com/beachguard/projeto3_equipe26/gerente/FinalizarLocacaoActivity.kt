package com.beachguard.projeto3_equipe26.gerente

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivityFinalizarLocacaoBinding
import com.google.firebase.functions.FirebaseFunctions
import java.nio.charset.Charset

class FinalizarLocacaoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFinalizarLocacaoBinding
    // Cria uma instância do Firebase Functions
    private lateinit var functions: FirebaseFunctions
    // Cria uma variável para o ID da locação
    private var locacaoId = ""
    // Cria uma instância do adaptador NFC
    private lateinit var nfcAdapter: NfcAdapter
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

        functions = FirebaseFunctions.getInstance("southamerica-east1")
        locacaoId = intent.getStringExtra("locacaoId").toString()
        // inicializa o adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    private fun setupUI() {
        binding = ActivityFinalizarLocacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVoltar.setOnClickListener {
            startActivity(Intent(this, HomeGerenteActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // cria um PendingIntent para a atividade atual
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        // cria um array de IntentFilter para interceptar as intents de NFC
        val intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
        val techListArray = arrayOf(arrayOf(android.nfc.tech.Ndef::class.java.name))
        // habilita o modo de despacho em primeiro plano
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListArray)
    }

    override fun onPause() {
        super.onPause()
        // desabilita o modo de despacho em primeiro plano
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // verifica se a intent recebida é uma intent de NFC
        println("Novo intent recebido: ${intent.action}")
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            println("Tag descoberta")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val data = "vazio"
                // limpa a tag colocando "vazio"
                writeDataToTag(tag, data)
                // finaliza a locação
                finalizarLocacao(locacaoId)
                binding.tvTitulo.text = "Locação finalizada! Pulseira limpa com sucesso!"
                binding.tvInfo.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun writeDataToTag(tag: Tag, data: String) {
        // cria uma mensagem NDEF com os dados
        val ndefMessage = createNdefMessage(data)
        // obtém uma instância de Ndef para o tag
        val ndef = Ndef.get(tag)
        try {
            // conecta ao tag
            ndef.connect()
            if (ndef.isWritable) {
                // escreve a mensagem no tag
                ndef.writeNdefMessage(ndefMessage)
                Toast.makeText(this, "Pulseira limpa com sucesso!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "TAG não pode ser gravada!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NFC", "Erro ao gravar dados!", e)
            Toast.makeText(this, "Erro ao gravar dados na TAG", Toast.LENGTH_SHORT).show()
        } finally {
            // fecha a conexão com o tag
            ndef.close()
        }
    }

    // função para criar uma mensagem NDEF com os dados
    private fun createNdefMessage(data: String): NdefMessage {
        // cria um array de bytes com a linguagem do texto
        val lang = "pt-br"
        val langBytes = lang.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = data.toByteArray(Charset.forName("UTF-8"))
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)

        // preenche o payload com os bytes da linguagem e do texto
        payload[0] = langLength.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

        // cria um NdefRecord com o payload
        val ndefRecord = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
        // retorna uma mensagem NDEF com o NdefRecord
        return NdefMessage(arrayOf(ndefRecord))
    }

    //função para finalizar locação. Recebe o ID da locação e chama a função finalizarLocacao do Firebase Functions
    private fun finalizarLocacao(locacaoId: String) {
        val data = hashMapOf("locacaoId" to locacaoId)

        functions
            .getHttpsCallable("finalizarLocacao")
            .call(data)
            .continueWith { task ->
                if (!task.isSuccessful) {
                    println("Erro ao finalizar locação!")
                    task.exception?.let {
                        throw it
                    }
                }
                println("Locação finalizada com sucesso!")

                try {
                    val result = task.result?.data as Map<String, Any>

                    val valorTotal = (result["valorTotal"] as? Number)?.toDouble() ?: 0.0
                    val multa = (result["multa"] as? Number)?.toDouble() ?: 0.0

                    println("Valor total: $valorTotal")
                    println("Multa: $multa")
                    binding.tvInfo.text = "Valor total: R$ $valorTotal"
                    binding.tvComplemento.visibility = android.view.View.VISIBLE
                    mostrarAlerta(valorTotal, multa)
                } catch (e: Exception) {
                    println("Erro ao processar resultado da função: ${e.message}")
                }
            }
    }

    //função para mostrar alerta
    private fun mostrarAlerta(valorTotal: Double, multa: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Locação finalizada!")
        builder.setMessage("Valor total: R$ $valorTotal\nMulta: R$ $multa \nO valor total será descontado do caução e o restante estornado devidamente!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }
}