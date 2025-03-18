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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.components.MapsMarkerAdapter
import com.beachguard.projeto3_equipe26.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    // cria um objeto do tipo GoogleMap para manipular o mapa
    private lateinit var mMap: GoogleMap
    // cria uma lista de objetos do tipo com.beachguard.projeto3_equipe26.components.Place para armazenar os locais
    private lateinit var places: MutableList<Place>
    // cria um objeto do tipo FusedLocationProviderClient para obter a localização do dispositivo
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // cria um objeto do tipo Location para armazenar a localização atual do dispositivo
    // sem uso, apenas para teste
    private lateinit var localizacaoAtual: Location
    // cria um objeto do tipo com.beachguard.projeto3_equipe26.components.Place para armazenar o local selecionado
    private var selectedPlace: Place? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

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

        // inicia as variáveis predefinidas
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        places = mutableListOf()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        localizacaoAtual = Location("LocalizacaoAtual")

        // cria uma thread separada para coletar os locais no db e atualizar o mapa
        CoroutineScope(Dispatchers.Main).launch {
            try {
                getLastLocation()
                places = getPlaces(db)
                //places.add(com.beachguard.projeto3_equipe26.components.Place("you", "Você está aqui", LatLng(localizacaoAtual.latitude, localizacaoAtual.longitude), "", ""))
                updateMapWithPlaces(places)
            } catch (e: Exception) {
                Log.e("Maps", "Erro ao carregar locais", e)
            }
        }

        // cria um objeto do tipo SupportMapFragment para manipular o mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync{ googleMap ->
            googleMap.setInfoWindowAdapter(MapsMarkerAdapter(this))
            onMapReady(googleMap)
        }
    }

    //função para iniciar a interface do usuário
    private fun setupUI() {
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener(){
            finish()
        }

        binding.btnAlugar.setOnClickListener(){
            selectedPlace?.let {
                val intent = Intent(this, SelecaoLocacaoActivity::class.java)
                intent.putExtra("place", it)
                startActivity(intent)
            }
        }
    }

    //função para manipular o mapa
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMarkerClickListener { marker ->
            val place = marker.tag as? Place
            place?.let {
                selectedPlace = it
                binding.btnAlugar.visibility = View.VISIBLE
            }
            false
        }
        mMap.setOnMapClickListener {
            binding.btnAlugar.visibility = View.GONE
            selectedPlace = null
        }

        // Verifica se a permissão foi concedida
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Habilita a camada de localização
            mMap.isMyLocationEnabled = true
        } else {
            // Solicita as permissões de localização
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    //função para obter os locais do banco de dados
    private suspend fun getPlaces(db: FirebaseFirestore): MutableList<Place> {
        val retorno: MutableList<Place> = mutableListOf()
        val querySnapshot = db.collection("locais").get().await()

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

    //função para atualizar o mapa com os locais
    private fun updateMapWithPlaces(places: MutableList<Place>) {
        // limpa o mapa
        mMap.clear()
        // adiciona os locais ao mapa
        places.forEach { place ->
            val snippet = "${place.address}\nReferência:\n${place.referencia}"
            // adiciona um marcador ao mapa para cada local
            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .snippet(snippet)
                    .position(place.latLng)
            )
            marker?.tag = place
        }
        // move a camera para posição do usuário
        moveCameraToUserLocation()
    }

    // verifica e solicita as permissões ao iniciar a activity
    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // pega a última localização conhecida do dispositivo
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    localizacaoAtual.latitude = latitude
                    localizacaoAtual.longitude = longitude
                } else {
                    Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // move a câmera para a localização do usuário
    @SuppressLint("MissingPermission")
    private fun moveCameraToUserLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
    }


    // função para tratar a resposta da solicitação de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            Toast.makeText(baseContext, "A permissão é necessária para utilizar esse aplicativo", Toast.LENGTH_SHORT).show()
            kotlin.concurrent.timer(daemon = true, period = 2000, initialDelay = 2000) {
                finish()
            }
        }
    }
}
