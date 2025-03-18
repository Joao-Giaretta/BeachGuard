package com.beachguard.projeto3_equipe26

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.beachguard.projeto3_equipe26.cliente.HomeActivity
import com.beachguard.projeto3_equipe26.databinding.ActivityLauncherBinding
import com.beachguard.projeto3_equipe26.gerente.HomeGerenteActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LauncherActivity : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 3000
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityLauncherBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            val user = auth.currentUser
            if(user != null){
                //se o usuário já estiver logado e for gerente, inicia a activity HomeGerenteActivity
                db.collection("users").document(user.email.toString()).get().addOnSuccessListener { document ->
                    if(document.getString("role") == "gerente"){
                        val intent = Intent(this, HomeGerenteActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        //se o usuário já estiver logado e for cliente, inicia a activity HomeActivity
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }else{
                //se o usuário não estiver logado, inicia a activity LoginActivity
                val intent = Intent(this, EntrarActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, SPLASH_DELAY)
    }
}