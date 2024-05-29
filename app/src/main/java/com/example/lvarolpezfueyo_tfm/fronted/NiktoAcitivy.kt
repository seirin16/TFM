package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.lvarolpezfueyo_tfm.R
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class NiktoAcitivy : AppCompatActivity() {

    private lateinit var resumeIP: TextView
    private lateinit var portToScan: TextView
    private lateinit var scanResult: TextView
    private lateinit var progressBar: ProgressBar
    private var ip: String? = null
    private var port: Int? = 0
    private var sendInformation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nikto_acitivy)

        resumeIP = findViewById(R.id.resumeIP)
        portToScan = findViewById(R.id.portToScan)
        scanResult = findViewById(R.id.scanResults)
        progressBar = findViewById(R.id.progressBar)


        val extras = intent.extras
        ip = extras?.getString("ip")
        port = extras?.getInt("port")
        val niktoScan = extras?.getString("niktoScan")


        // Actualizar los valores de los TextViews
        resumeIP.text = "IP: $ip"
        portToScan.text = "Puerto seleccionado: $port"

        if (niktoScan != null) {
            scanResult.text = niktoScan
        } else if (ip != null && port != null) {
            progressBar.visibility = View.VISIBLE
            scanPort(ip!!, port!!)
        } else {
            scanResult.text = "Error a la hora de recibir los parametros"

        }


    }
    /**
     * Función que realiza un escaneo de puertos en una dirección IP específica.
     *
     * @param ip Dirección IP a escanear.
     */
    private fun scanPort(ip: String, port: Int) {

        val client = OkHttpClient.Builder()
            .connectTimeout(
                120,
                TimeUnit.SECONDS
            ) // Tiempo de espera para establecer la conexión
            .writeTimeout(120, TimeUnit.SECONDS) // Tiempo de espera para enviar la solicitud
            .readTimeout(120, TimeUnit.SECONDS) // Tiempo de espera para recibir la respuesta
            .build()

        val url = "http://192.168.0.12:3000/nikto/$ip/$port"


        val request = Request.Builder()
            .url(url)
            .build()

        // Realiza una llamada asíncrona para obtener la respuesta del servidor
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                this@NiktoAcitivy.runOnUiThread {
                    scanResult.text =
                        "Error: El tiempo de escaneo ha superado el tiempo máximo establecido (2 minutos) por lo que no se puede realizar"
                    progressBar.visibility = View.GONE
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Código inesperado $response")
                    val responseBody = response.body!!.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val myResponse = json.getString("niktoOutput")

                        this@NiktoAcitivy.runOnUiThread {
                            sendInformation=true;
                            scanResult.text = myResponse
                            progressBar.visibility = View.GONE

                        }

                    }
                }
            }
        })

    }

    /**
     * Este método se llama cuando el usuario presiona el botón de retroceso del dispositivo.
     * Si la variable `sendInformation` es verdadera, se envía información adicional al
     * Activity anterior a través de un Intent y se establece el resultado como RESULT_OK.
     * Luego, se llama al método super.onBackPressed() para finalizar el Activity actual.
     */
    override fun onBackPressed() {
        if (sendInformation) {
            intent.putExtra("ip", ip)
            intent.putExtra("port", port)
            intent.putExtra("scan", scanResult.text)
            setResult(RESULT_OK, intent)
        }

        super.onBackPressed()
    }

}