package com.example.lvarolpezfueyo_tfm.fronted

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.lvarolpezfueyo_tfm.R
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import java.net.InetAddress


class AnalisisActivity : AppCompatActivity() {

    private lateinit var editTextIp: EditText
    private lateinit var resumeIP: TextView
    private lateinit var openPorts: TextView
    private lateinit var buttonScan: Button
    private lateinit var textViewResult: TextView
    private lateinit var checkBox: CheckBox
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis)

        editTextIp = findViewById(R.id.editTextIP)
        resumeIP = findViewById(R.id.resumeIP)
        openPorts = findViewById(R.id.openPorts)

        buttonScan = findViewById(R.id.scanButton)
        textViewResult = findViewById(R.id.scanResults)
        checkBox = findViewById(R.id.checkBox)
        progressBar = findViewById(R.id.progressBar)

        buttonScan.setOnClickListener {

            resumeIP.text = editTextIp.text;
            textViewResult.text = "";

            val ip = editTextIp.text.toString()
            progressBar.visibility = View.VISIBLE
            scanPorts(ip)
        }
    }

    private fun scanPorts(ip: String) {
        // Verifica si la dirección IP es válida
        if (!isValidIP(ip)) {
            textViewResult.text = "Dirección IP no válida"
            return
        } else {
            val client = OkHttpClient()

            // Construye la URL para escanear los puertos en la dirección IP especificada
            val url = "http://192.168.0.12:3000/scan/$ip?format=${checkBox.isChecked}"

            val request = Request.Builder()
                .url(url)
                .build()

            // Realiza una llamada asíncrona para obtener la respuesta del servidor
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    this@AnalisisActivity.runOnUiThread {
                        progressBar.visibility = View.GONE
                        textViewResult.text = "Error: ${e.message}"
                    }
                }

                @Throws(IOException::class)
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        // Si la respuesta no es exitosa, lanza una excepción
                        if (!response.isSuccessful) throw IOException("Código inesperado $response")

                        val responseBody = response.body
                        if (responseBody != null) {
                            val myResponse = responseBody.string()

                            // Actualiza la interfaz de usuario con la respuesta formateada o sin formato
                            this@AnalisisActivity.runOnUiThread {
                                textViewResult.text = myResponse
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            })
        }
    }

    private fun isValidIP(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

}