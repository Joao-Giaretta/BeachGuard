package com.beachguard.projeto3_equipe26.gerente

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivityLerPulseiraBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.nio.charset.Charset
import java.util.Arrays


class LerPulseiraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLerPulseiraBinding
    // Cria uma instância do adaptador NFC
    private lateinit var nfcAdapter: NfcAdapter
    // Cria uma variável para o ID da locação
    private var locacaoId = "vazio"
    // Cria uma instância do Firebase Functions
    private lateinit var functions: FirebaseFunctions
    // Cria uma instância do Firebase Firestore
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupUI()
        supportActionBar?.hide()
        functions = FirebaseFunctions.getInstance()
        db = FirebaseFirestore.getInstance()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // da get no adaptador nfc padrão
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    //função para configurar a interface do usuário
    private fun setupUI() {
        binding = ActivityLerPulseiraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //função para quando a activity estiver em foco
    override fun onResume() {
        super.onResume()
        // Cria um intent pendente para a activity atual
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        // Cria um array de filtros de intent para a activity atual
        val intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
        // Cria um array de tecnologias para a activity atual
        val techListArray = arrayOf(arrayOf(android.nfc.tech.Ndef::class.java.name))

        // Habilita o despacho em primeiro plano
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListArray)
    }

    //função para quando a activity não estiver em foco
    override fun onPause() {
        super.onPause()
        // Desabilita o despacho em primeiro plano
        nfcAdapter.disableForegroundDispatch(this)
    }

    //função para quando um novo intent for recebido
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("Novo intent recebido: ${intent.action}")
        // Verifica se o intent é de descoberta de tag
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            println("Tag descoberta")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                // se for diferente de nulo, lê a tag
                readFromTag(tag)
            }
        }
    }

    //função para ler a tag
    private fun readFromTag(tag: Tag) {
        // Cria uma instância Ndef
        val ndef = Ndef.get(tag)
        // verifica se a instância é diferente de nulo
        if (ndef != null) {
            // conecta a instância
            ndef.connect()
            // lê a mensagem Ndef
            val ndefMessage = ndef.ndefMessage
            if (ndefMessage != null) {
                // se a mensagem for diferente de nulo, lê a mensagem
                val records = ndefMessage.records
                for (record in records) {
                    // verifica se o registro é do tipo texto
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                        try {
                            // lê o payload do registro
                            val payload = record.payload
                            val textEncoding = if (payload[0].toInt() and 128 == 0) Charset.forName("UTF-8") else Charset.forName("UTF-16")
                            val languageCodeLength = payload[0].toInt() and 63
                            val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
                            // exibe o texto na tela
                            binding.tvLerPulseira.text = text
                            if(text == "vazio" || text == "" || text.length != 20){
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Erro")
                                builder.setMessage("Pulseira não cadastrada")
                                builder.setPositiveButton("OK"){dialog, which ->
                                    dialog.dismiss()
                                    startActivity(Intent(this, HomeGerenteActivity::class.java))
                                    finish()
                                }
                                val dialog: AlertDialog = builder.create()
                                dialog.show()
                            }else{
                                locacaoId = text
                                val intent = Intent(this, DirecionarParaUmOuDoisClientesActivity::class.java)
                                intent.putExtra("locacaoId", locacaoId)
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            Log.e("NFC", "Erro ao ler tag NFC", e)
                        }
                    }
                }
            }
            ndef.close()
        }
    }
}