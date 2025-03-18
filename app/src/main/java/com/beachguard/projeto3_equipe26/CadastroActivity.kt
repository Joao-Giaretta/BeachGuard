package com.beachguard.projeto3_equipe26

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.components.Account
import com.beachguard.projeto3_equipe26.databinding.ActivityCadastroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityCadastroBinding
    private lateinit var account: Account
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        setupUI()
        masksApply()
    }

    private fun setupUI(){
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //executa a função de cadastro ao clicar no botão
        binding.btnCadastrar.setOnClickListener {
            if(checkAllFields()){
                //cria uma instância do Firebase Auth e tenta criar um usuário com o e-mail e senha informados
                auth.createUserWithEmailAndPassword(binding.etEmail.text.toString(), binding.etSenha.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            //salva as informações do usuário na variável user
                            val user = auth.currentUser
                            //envia um e-mail de verificação para o usuário
                            user?.sendEmailVerification()?.addOnCompleteListener(this) {
                                if (it.isSuccessful) {
                                    Log.d("CadastroActivity", "E-mail de verificação enviado")
                                    Toast.makeText(baseContext, "E-mail de verificação enviado", Toast.LENGTH_SHORT).show()
                                    println("entrou")
                                } else {
                                    Log.e("CadastroActivity", "Erro ao enviar e-mail de verificação", task.exception)
                                }
                            }
                            try{
                                println("entrou no try")
                                account = Account(
                                    binding.etNome.text.toString(),
                                    binding.etEmail.text.toString(),
                                    binding.etCpf.text.toString(),
                                    binding.etDataNasc.text.toString(),
                                    binding.etCelular.text.toString(),
                                    "cliente"
                                )
                                val collection = firestore.collection("users")
                                println("entrou no collection")
                                //adiciona o usuário ao Firestore
                                collection.document(account.email).set(accountMap(account))
                                    .addOnCompleteListener { taskAdd ->
                                        if(taskAdd.isSuccessful){
                                            println("Usuário adicionado com sucesso")
                                            Log.d("CadastroActivity", "Usuário adicionado com sucesso")
                                        }else{
                                            println("erro ao adicionar usuário \n" + taskAdd.exception)
                                            Log.e("CadastroActivity", "Erro ao adicionar usuário", taskAdd.exception)
                                        }
                                    }
                            }catch(e: Exception) {
                                println("erro ao salvar no bando de dados" + task.exception)
                                Log.e("CadastroActivity", "Erro ao salvar no banco de dados", e)
                            }
                            //se o cadastro for bem sucedido, exibe uma mensagem de sucesso
                            //tenta criar uma instância de Account com os dados informados
                            Toast.makeText(baseContext, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show()
                            //desloga o usuário e leva para adtivity entrar
                            auth.signOut()
                            val intent = Intent(this, EntrarActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            //se o cadastro falhar, exibe uma mensagem de erro e destaca o campo de e-mail como suposição de erro
                            Log.e("CadastroActivity", "Erro ao cadastrar", task.exception)
                            binding.tiEmail.error = "E-mail já cadastrado"
                        }
                    }
            }
        }

        binding.btnLogin.setOnClickListener {
            //ao clicar no botão de login, inicia a activity EntrarActivity
            val intent = Intent(this, EntrarActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun masksApply(){
        //aplica máscaras nos campos de CPF, data de nascimento e celular
        binding.etCpf.addTextChangedListener(MaskEditUtil.mask(binding.etCpf, MaskEditUtil.FORMAT_CPF))
        binding.etDataNasc.addTextChangedListener(MaskEditUtil.mask(binding.etDataNasc, MaskEditUtil.FORMAT_DATE))
        binding.etCelular.addTextChangedListener(MaskEditUtil.mask(binding.etCelular, MaskEditUtil.FORMAT_FONE))
    }

    class MaskEditUtil {
        companion object {
            const val FORMAT_CPF = "###.###.###-##"
            const val FORMAT_DATE = "##/##/####"
            const val FORMAT_FONE = "(##) #####-####"

            fun mask(editText: EditText, mask: String): TextWatcher {
                return object : TextWatcher {
                    var isUpdating: Boolean = false
                    var old = ""
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val str = unmask(s.toString())
                        var maskCurrent = ""
                        if (isUpdating) {
                            old = str
                            isUpdating = false
                            return
                        }
                        var i = 0
                        for (m: Char in mask.toCharArray()) {
                            if (m != '#' && str.length > old.length) {
                                maskCurrent += m
                                continue
                            }
                            try {
                                maskCurrent += str[i]
                            } catch (e: Exception) {
                                break
                            }
                            i++
                        }
                        isUpdating = true
                        editText.setText(maskCurrent)
                        editText.setSelection(maskCurrent.length)
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: Editable?) {}
                }
            }

            fun unmask(s: String): String {
                return s.replace("[.]".toRegex(), "").replace("[-]".toRegex(), "").replace("[/]".toRegex(), "").replace("[(]".toRegex(), "").replace("[ ]".toRegex(), "").replace("[:]".toRegex(), "").replace("[)]".toRegex(), "")
            }
        }
    }

    private fun accountMap(account: Account): Map<String, Any> {
        //retorna um mapa com os dados da conta
        return mapOf(
            "nome" to account.nome,
            "email" to account.email,
            "cpf" to account.cpf,
            "dataNascimento" to account.dataNascimento,
            "celular" to account.celular,
            "role" to account.role
        )
    }

    private fun checkAllFields(): Boolean {
        //verifica todos os campos do cadastro com suas regras
        val email = binding.etEmail.text.toString()
        if(binding.etEmail.text.toString() == ""){
            binding.tiEmail.error = "Campo obrigatório"
            return false
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.tiEmail.error = "E-mail inválido"
            return false
        }
        if(binding.etSenha.text.toString() == ""){
            binding.tiSenha.error = "Campo obrigatório"
            binding.tiSenha.errorIconDrawable = null
            return false
        }
        if(binding.etSenha.text.toString().length < 6){
            binding.tiSenha.error = "Senha deve ter no mínimo 6 caracteres"
            binding.tiSenha.errorIconDrawable = null
            return false
        }
        if(binding.etConfirmarSenha.text.toString() == ""){
            binding.tiConfirmarSenha.error = "Campo obrigatório"
            binding.tiConfirmarSenha.errorIconDrawable = null
            return false
        }
        if(binding.etSenha.text.toString() != binding.etConfirmarSenha.text.toString()){
            binding.tiConfirmarSenha.error = "Senhas não conferem"
            return false
        }
        if(binding.etNome.text.toString() == ""){
            binding.tiNome.error = "Campo obrigatório"
            return false
        }
        if(binding.etCpf.text.toString() == ""){
            binding.tiCpf.error = "Campo obrigatório"
            return false
        }
        if(binding.etCpf.text.toString().length < 14){
            binding.tiCpf.error = "CPF inválido"
            return false
        }
        if(binding.etDataNasc.text.toString() == ""){
            binding.tiDataNasc.error = "Campo obrigatório"
            return false
        }
        if(!binding.etDataNasc.text.toString().matches(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}"))){
            binding.tiDataNasc.error = "Data de nascimento inválida"
            return false
        }
        if(binding.etCelular.text.toString() == ""){
            binding.tiCelular.error = "Campo obrigatório"
            return false
        }
        if(!binding.etCelular.text.toString().matches(Regex("\\([0-9]{2}\\) [0-9]{5}-[0-9]{4}"))){
            binding.tiCelular.error = "Celular inválido"
            return false
        }
        return true
    }
}