package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.lvarolpezfueyo_tfm.R

class LoginActivity : AppCompatActivity() {

    private lateinit var signUpText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //El usuario pasa a la pantalla donde se registrará
        signUpText = findViewById(R.id.signUpText)
        signUpText.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        // Dejar en blanco para evitar que el usuario vuelva a la pantalla anterior
    }

}