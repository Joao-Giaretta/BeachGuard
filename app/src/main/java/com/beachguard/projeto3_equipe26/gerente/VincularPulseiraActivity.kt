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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivityVincularPulseiraBinding
import java.nio.charset.Charset

class VincularPulseiraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVincularPulseiraBinding
    // cria uma variável para o adaptador NFC
    private lateinit var nfcAdapter: NfcAdapter
    private var locacaoId = ""
    private var qtdPessoas = 1
    private var cont = 0
    private lateinit var Handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupUI()
        supportActionBar?.hide()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        locacaoId = intent.getStringExtra("locacaoId").toString()
        qtdPessoas = intent.getIntExtra("qtdPessoas", 1)
        // inicializa o adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Handler = Handler(Looper.myLooper()!!)
    }

    //função para configurar a interface do usuário
    private fun setupUI() {
        binding = ActivityVincularPulseiraBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    // função chamada quando uma nova intent é recebida
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
                // converte o ID do tag para uma string
                val data = locacaoId
                // escreve a string no tag
                writeDataToTag(tag, data)
                // exibe a string na tela
                binding.tvTag.text = "Tag NFC gravada com sucesso!"
                if(qtdPessoas == 2 && cont < 1){
                    alertaNumeroDePessoas()
                }else{
                    Toast.makeText(this, "Registro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    Handler.postDelayed({
                        val intent = Intent(this, DevolveInfoActivity::class.java)
                        intent.putExtra("locacaoId", locacaoId)
                        startActivity(intent)
                        finish()
                    }, 3000)
                }
            }
        }
    }

    // função para escrever dados em um tag NFC
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
                Toast.makeText(this, "Dados gravados com sucesso!", Toast.LENGTH_SHORT).show()
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

    private fun alertaNumeroDePessoas(){
        AlertDialog.Builder(this)
            .setTitle("Registro realizado com sucesso!")
            .setMessage("Resgistre a segunda pulseira!")
            .setPositiveButton("ok") { dialog, _ ->
                dialog.dismiss()
                binding.tvTag.text = "Aguardando a segunda pulseira..."
                cont++
            }
            .show()
    }

}