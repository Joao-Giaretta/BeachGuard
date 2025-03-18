package com.beachguard.projeto3_equipe26.cliente

import com.beachguard.projeto3_equipe26.components.Place
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.databinding.ActivitySelecaoLocacaoBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SelecaoLocacaoActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelecaoLocacaoBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var places : MutableList<Place>
    private lateinit var localizacaoAtual: Location
    private lateinit var quiosque: Place
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onStart() {
        super.onStart()
        checkLocationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = FirebaseFirestore.getInstance()
        supportActionBar?.hide()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        localizacaoAtual = Location("LocalizacaoAtual")
        setupUI()
        places = mutableListOf()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CoroutineScope(Dispatchers.IO).launch {
            try{
                // Busca os locais no Firestore
                places = getPlaces(db)
                quiosque = intent.getParcelableExtra("place") ?: throw IllegalStateException("No place found in intent")
                getLastLocation()
                // Verifica se há algum quiosque próximo
                val locQuiosque = Location("Quiosque")
                locQuiosque.latitude = quiosque.latLng.latitude
                locQuiosque.longitude = quiosque.latLng.longitude
                withContext(Dispatchers.Main){
                    // Verifica se o quiosque está próximo
                    if(localizacaoAtual.distanceTo(locQuiosque) < 500) {
                        println( "Quiosque mais próximo: ${quiosque.name}")
                        println( "ID do Quiosque mais próximo: ${quiosque.id}")
                        // Exibe as informações do quiosque
                        exibirInformacoesQuiosque()
                    }else{
                        // Não há quiosque próximo
                        println("Não há quiosque próximo")
                    }
                }
            }catch (e: Exception){
                Log.e("SelecaoLocacaoActivity", "Erro ao buscar locais: $e")
            }
        }
        binding.btnAlugar.setOnClickListener {
            // Inicia a atividade de seleção de tempo
            val intent = Intent(this, SelecionarTempoActivity::class.java)
            intent.putExtra("quiosqueId", quiosque.id)
            intent.putExtra("quiosqueName", quiosque.name)
            intent.putExtra("quiosqueAddress", quiosque.address)
            intent.putExtra("quiosqueReferencia", quiosque.referencia)
            startActivity(intent)
        }
    }

    // Função para configurar a interface do usuário
    private fun setupUI(){
        binding = ActivitySelecaoLocacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVoltar.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun exibirInformacoesQuiosque(){
        binding.tvNomeQuiosque.text = quiosque.name
        binding.tvEndereco.text = quiosque.address
        binding.tvReferencia.text = quiosque.referencia
        binding.btnAlugar.visibility = View.VISIBLE
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão de localização não concedida, solicita permissão ao usuário
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            //Toast.makeText(this, "Permissão de localização já concedida", Toast.LENGTH_SHORT).show()
            getLastLocation()
            // Permissão de localização já concedida
            // Faça algo aqui, como iniciar o serviço de localização ou carregar os dados do mapa
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de localização concedida pelo usuário
                // Faça algo aqui, como iniciar o serviço de localização ou carregar os dados do mapa
                getLastLocation()
            } else {
                // Permissão de localização não concedida pelo usuário
                Snackbar.make(
                    binding.root,
                    "A permissão de localização é necessária para usar este aplicativo",
                    Snackbar.LENGTH_SHORT
                ).show()
                kotlin.concurrent.timer(daemon = true, period = 2000, initialDelay = 2000) {
                    finish()
                }
            }
        }
    }

    // Função para buscar os locais no Firestore
    private suspend fun getPlaces(db: FirebaseFirestore): MutableList<Place> {
        val retorno: MutableList<Place> = mutableListOf()
        val querySnapshot = db.collection("locais").get().await() // Espera até que a busca seja concluída

        for (document in querySnapshot.documents) {
            val placeId = document.id
            Log.d("SelecaoLocacaoActivity", "ID do documento: $placeId")
            val place = Place(
                placeId, // ID do documento
                document.getString("nome") ?: "",
                LatLng(
                    (document.getString("latitude") ?: "0.0").toDouble(),
                    (document.getString("longitude") ?: "0.0").toDouble()
                ),
                document.getString("endereco") ?: "",
                document.getString("referencia") ?: ""
            )
            retorno.add(place)
        }

        return retorno
    }

    // Função para obter a localização atual do dispositivo
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Verifica se a localização foi encontrada com sucesso
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    localizacaoAtual.latitude = latitude
                    localizacaoAtual.longitude = longitude
                    //Toast.makeText(this, "Latitude: $latitude, Longitude: $longitude", Toast.LENGTH_LONG).show()
                } else {
                    // A localização não está disponível
                    Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Ocorreu um erro ao tentar obter a localização
                Toast.makeText(this, "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}