package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.example.lvarolpezfueyo_tfm.R

class SplashScreen : AppCompatActivity() {

    private lateinit var splashImg: ImageView
    private lateinit var lottieAnimationView: LottieAnimationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        splashImg = findViewById(R.id.img);
        lottieAnimationView = findViewById(R.id.lottie);

        splashImg.animate().translationY(0F).setDuration(0).setStartDelay(2000);
        lottieAnimationView.animate().translationY(0F).setDuration(0).setStartDelay(2000);

        //Despues de 2 segundos se muestra la pantalla de Login
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val intent = Intent(this@SplashScreen, LoginActivity::class.java)
            startActivity(intent)

        }, 2000)

    }
}