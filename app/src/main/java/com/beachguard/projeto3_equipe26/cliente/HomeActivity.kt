package com.beachguard.projeto3_equipe26.cliente


import com.beachguard.projeto3_equipe26.components.Place
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.beachguard.projeto3_equipe26.EntrarActivity
import com.beachguard.projeto3_equipe26.PagamentoActivity
import com.beachguard.projeto3_equipe26.R
import com.beachguard.projeto3_equipe26.components.MapsMarkerAdapter
import com.beachguard.projeto3_equipe26.databinding.ActivityHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    // cria um objeto do tipo GoogleMap para manipular o mapa
    private lateinit var mMap: GoogleMap
    // cria uma lista de objetos do tipo com.beachguard.projeto3_equipe26.components.Place para armazenar os locais
    private lateinit var places: MutableList<Place>
    // cria um objeto do tipo FusedLocationProviderClient para obter a localização do dispositivo
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        //cria um objeto do tipo FirebaseFirestore para acessar o banco de dados
        db = FirebaseFirestore.getInstance()
        //cria um objeto do tipo FirebaseAuth para autenticação
        auth = FirebaseAuth.getInstance()
        setupUI()

        places = mutableListOf()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                verificarLocacaoPendente()
                places = getPlaces(db)
                updateMapWithPlaces(places)
            } catch (e: Exception) {
                Log.e("HomeActivity", "Erro na thread secundária", e)
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
        //função para iniciar a interface do usuário
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAbrirMapa.setOnClickListener{
            //ao clicar no botão de mapa, a activity Maps é iniciada
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        drawerLayout = binding.drawerLayout
        navView = binding.navigationView


        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            menuItem.isChecked = true
            binding.drawerLayout.close()
            true
        }

        // Adiciona ouvintes para os itens do Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.itPerfil -> {
                    val intent = Intent(this, PerfilPessoalActivity::class.java)
                    startActivity(intent)
                }
                R.id.itCartoes -> {
                    val intent = Intent(this, CartoesActivity::class.java)
                    startActivity(intent)
                }
                R.id.itAddCartao -> {
                    val intent = Intent(this, PagamentoActivity::class.java)
                    startActivity(intent)
                }
                R.id.itMapa -> {
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                }
                R.id.itSair -> {
                    showAlertDialog()
                }
            }
            drawerLayout.closeDrawers()
            true
        }
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

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

    private fun showAlertDialog(){
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("Sim") { dialog, which ->
                auth.signOut()
                Toast.makeText(baseContext, "Deslogado com sucesso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, EntrarActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    // função para tratar a resposta da solicitação de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            Toast.makeText(baseContext, "A permissão é necessária para utilizar esse aplicativo", Toast.LENGTH_SHORT).show()
            kotlin.concurrent.timer(daemon = true, period = 2000, initialDelay = 2000) {
                finish()
            }
        }
    }

    private fun verificarLocacaoPendente(){
        val user = auth.currentUser
        val userEmail = user?.email

        if (userEmail != null){
            // consulta para verificar se há alguma locação pendente para o usuário logado
            db.collection("locacoes")
                .whereEqualTo("email", userEmail)
                .whereEqualTo("status", "Pendente")
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty){
                        // se houver locação pendente, exibe um aviso e redireciona para a tela do Qrcode
                        val quiosqueId = documents.first().getString("quiosqueId")// recupera o id da locação pendente
                        AlertDialog.Builder(this)
                            .setTitle("Locação pendente")
                            .setMessage("Você possui uma locação pendente. Deseja continuar?")
                            .setPositiveButton("Sim") { dialog, which ->
                                val intent = Intent(this, QRCodeActivity::class.java)
                                intent.putExtra("quiosqueId", quiosqueId)
                                intent.putExtra("locacaoId", documents.first().id)
                                startActivity(intent)
                                finish()
                            }
                            .setNegativeButton("Não") { dialog, which ->
                                // se o usuário não deseja continuar, muda o status da locação para cancelada
                                db.collection("locacoes").document(documents.first().id)
                                    .update("status", "Cancelada")
                                    .addOnSuccessListener {
                                        Log.d("HomeActivity", "Status da locação alterado para Cancelada")
                                    }
                                    .addOnFailureListener {
                                        Log.e("HomeActivity", "Erro ao alterar o status da locação", it)
                                    }
                            }
                            .show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("HomeActivity", "Erro ao verificar locação pendente", exception)
                }
        }
    }
}