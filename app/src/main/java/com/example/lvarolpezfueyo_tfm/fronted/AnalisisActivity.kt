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
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit


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
            scanPorts(ip)
        }
    }

    private fun scanPorts(ip: String) {
        // Verifica si la dirección IP es válida
        if (!isValidIP(ip)) {
            textViewResult.text = "Dirección IP no válida"
            return
        } else {
            progressBar.visibility = View.VISIBLE
            val client = OkHttpClient.Builder()
                .connectTimeout(
                    10,
                    TimeUnit.SECONDS
                ) // Tiempo de espera para establecer la conexión
                .writeTimeout(10, TimeUnit.SECONDS) // Tiempo de espera para enviar la solicitud
                .readTimeout(30, TimeUnit.SECONDS) // Tiempo de espera para recibir la respuesta
                .build()

            val format = if (checkBox.isChecked) "false" else "true"
            val url = "http://192.168.0.12:3000/scan/$ip/$format"

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

                        val responseBody = response.body!!.string()
                        if (responseBody != null) {
                            val json = JSONObject(responseBody)

                            val myResponse = if (!checkBox.isChecked) {
                                json.getString("formattedOutput")
                            } else {
                                json.getString("stdout")
                            }


                            val openPortsJson = json.getJSONArray("openPorts")
                            val numOpenPorts = openPortsJson.length()


                            // Actualiza la interfaz de usuario con la respuesta formateada o sin formato
                            this@AnalisisActivity.runOnUiThread {
                                textViewResult.text = myResponse
                                openPorts.text = "Nº puertos abiertos: ${numOpenPorts.toString()}"
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