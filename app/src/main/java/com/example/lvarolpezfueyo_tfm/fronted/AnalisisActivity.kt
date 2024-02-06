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
        if (!isValidIP(ip)) {

            textViewResult.text = "Direccion IP no valida"
            return

        } else {
            val client = OkHttpClient()

            val url = "http://192.168.0.12:3000/scan/$ip"

            val request = Request.Builder()
                .url(url)
                .build()

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
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val responseBody = response.body
                        if (responseBody != null) {
                            val myResponse = responseBody.string()
                            val formattedResponse = formatNmapOutput(myResponse)

                            this@AnalisisActivity.runOnUiThread {
                                textViewResult.text = if (checkBox.isChecked) myResponse else formattedResponse
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                }

            })
        }
    }

    private fun formatNmapOutput(nmapOutput: String): String {
        val lines = nmapOutput.lines()
        val stringBuilder = StringBuilder()

        // Agregamos los encabezados
        stringBuilder.append("PORT\tSTATE\tSERVICE\n")

        for (line in lines) {
            // Utilizamos una expresión regular para identificar las líneas con la información de los puertos
            if (line.matches(Regex("^\\d+/[tcpudp]+\\s+\\S+\\s+\\S+"))) {
                // Separamos la línea por espacios mientras ignoramos múltiples espacios en blanco consecutivos
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val portInfo = parts[0] // Contiene el número de puerto y el protocolo
                    val state = parts[1]    // Contiene el estado del puerto
                    val service = parts[2]  // Contiene el nombre del servicio

                    // Añadimos la información al StringBuilder
                    stringBuilder.append("$portInfo\t$state\t$service\n")
                }
            }
        }

        // Devolvemos la cadena formateada
        return stringBuilder.toString()
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