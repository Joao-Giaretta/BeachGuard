package com.beachguard.projeto3_equipe26

import com.beachguard.projeto3_equipe26.components.Place
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.cliente.HomeActivity
import com.beachguard.projeto3_equipe26.components.MapsMarkerAdapter
import com.beachguard.projeto3_equipe26.databinding.ActivityEntrarBinding
import com.beachguard.projeto3_equipe26.gerente.HomeGerenteActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EntrarActivity : AppCompatActivity(), OnMapReadyCallback {
    //cria uma variável do tipo FirebaseAuth para autenticação
    private lateinit var auth: FirebaseAuth
    //cria uma variável do tipo ActivityEntrarBinding para acessar os elementos da interface
    private lateinit var binding: ActivityEntrarBinding
    //cria uma variável do tipo MutableList para armazenar os lugares/armários
    private lateinit var places: MutableList<Place>
    //cria uma variável do tipo FirebaseFirestore para acessar o banco de dados
    private lateinit var db: FirebaseFirestore
    //cria uma variável do tipo GoogleMap para acessar o mapa
    private lateinit var mMap: GoogleMap

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

    // onStart é chamado quando a atividade esta prestes a se tornar visível
    override fun onStart() {
        //verifica se as permissões de localização foram concedidas. Se nao, solicita as permissões.
        super.onStart()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // As permissões ja foram concedidas
        }
    }

    // trata o resultado do pedido de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissão concedida
        }else{
            // Permissão negada
            Toast.makeText(this, "A permissão é necessária para utlizar esse aplicativo", Toast.LENGTH_SHORT).show()
            kotlin.concurrent.timer(daemon = true, period = 2000, initialDelay = 2000) {
                finish()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //infla o layout da interface e o exibe na tela
        setupUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        db = FirebaseFirestore.getInstance()
        supportActionBar?.hide()
        places = mutableListOf()
        auth = FirebaseAuth.getInstance()
        //cria um mapa com os marcadores dos armários
        CoroutineScope(Dispatchers.Main).launch {
            try {
                places = getPlaces(db)
                updateMapWithPlaces(places)
                // Faça algo com os locais aqui, como exibir no mapa
            } catch (e: Exception) {
                Log.e("EntrarActivity", "Erro ao carregar locais", e)
            }
        }


        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync{ googleMap ->
            googleMap.setInfoWindowAdapter(MapsMarkerAdapter(this))
            onMapReady(googleMap)
        }
    }

    private fun updateMapWithPlaces(places: MutableList<Place>) {
        mMap.clear() // Limpa todos os marcadores do mapa

        // Adiciona marcadores para cada local na lista de lugares
        places.forEach { place ->
            val snippet = "${place.address}\n Referência: \n ${place.referencia}"
            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .snippet(snippet)
                    .position(place.latLng)
            )
            marker?.tag = place
        }

        // Mover a câmera para a posição do marcador
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(places[0].latLng, 15f))
    }

    private fun setupUI(){
        binding = ActivityEntrarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEntrar.setOnClickListener{
            //executa a função de login ao clicar no botão e verifica se os campos estão corretos
            if(checkAllFields()){
                auth.signInWithEmailAndPassword(binding.etEmail.text.toString(), binding.etSenha.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            //verifica se o email foi verificado, caso não, impede o login
                            val user = auth.currentUser
                            if(user?.isEmailVerified == false){
                                Toast.makeText(baseContext, "Você precisa verificar o seu email antes de fazer login", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                return@addOnCompleteListener
                            }else{
                                //verifica a role para redirecionar para a tela correta
                                db.collection("users").document(user?.email.toString()).get().addOnSuccessListener { document ->
                                    Toast.makeText(baseContext, "Login realizado com sucesso", Toast.LENGTH_SHORT).show()
                                    if(document.getString("role") == "gerente"){
                                        val intent = Intent(this, HomeGerenteActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }else{
                                        //se o login for bem sucedido, inicia a activity HomeActivity
                                        val intent = Intent(this, HomeActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                        } else {
                            //se o login falhar, exibe uma mensagem de erro
                            Log.e("EntrarActivity", "Erro ao logar", task.exception)
                            binding.tiEmail.error = "E-mail ou senha incorretos"
                        }
                    }
            }
        }

        binding.btnSignUp.setOnClickListener {
            //ao clicar no botão de cadastro, inicia a activity CadastroActivity
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnEsqueceuSenha.setOnClickListener {
            //ao clicar no botão de esqueci minha senha, inicia a activity EsqueciMinhaSenhaActivity
            val intent = Intent(this, EsqueciMinhaSenhaActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Função chamada quando o mapa está pronto para ser usado
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurar o mapa conforme necessário
        mMap.uiSettings.isZoomControlsEnabled = true
    }

    //função que busca os lugares no banco de dados
    private suspend fun getPlaces(db: FirebaseFirestore): MutableList<Place> {
        val retorno: MutableList<Place> = mutableListOf()
        val querySnapshot = db.collection("locais").get().await() // Espera até que a busca seja concluída

        for (document in querySnapshot.documents) {
            val place = Place(
                document.id,
                document.getString("nome") ?: "",
                LatLng((document.getString("latitude") ?: "0.0").toDouble(), (document.getString("longitude") ?: "0.0").toDouble()),
                document.getString("endereco") ?: "",
                document.getString("referencia") ?: ""
            )
            retorno.add(place)
        }

        return retorno
    }


    private fun checkAllFields() : Boolean{
        //verifica se os campos de e-mail e senha estão preenchidos e preenchidos corretamente
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
        return true
    }
}