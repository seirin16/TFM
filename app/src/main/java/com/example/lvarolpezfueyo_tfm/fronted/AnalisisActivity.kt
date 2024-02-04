package com.example.lvarolpezfueyo_tfm.fronted

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.lvarolpezfueyo_tfm.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class AnalisisActivity : AppCompatActivity() {

    private lateinit var editTextIp: EditText
    private lateinit var resumeIP: EditText
    private lateinit var buttonScan: Button
    private lateinit var textViewResult: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis)

        editTextIp = findViewById(R.id.editTextIP)
        resumeIP = findViewById(R.id.resumeIP)
        buttonScan = findViewById(R.id.scanButton)
        textViewResult = findViewById(R.id.scanResults)
        progressBar = findViewById(R.id.progressBar)

        buttonScan.setOnClickListener {

            resumeIP.text = editTextIp.text;
            textViewResult.text ="";

            val ip = editTextIp.text.toString()
            scanPorts(ip)
        }
    }

    private fun scanPorts(ip: String) {
        println("Entro2")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                progressBar.visibility = View.VISIBLE
                val openPorts = scanPortsConcurrently(ip)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    textViewResult.text = openPorts.joinToString(", ")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Manejar error (mostrar mensaje de error al usuario)
                }
            }
        }
    }

    private suspend fun scanPortsConcurrently(ip: String): List<Int> {
        println("Entro3")
        val openPorts = mutableListOf<Int>()
        val job = Job()
        val executor = Executors.newFixedThreadPool(10)
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher + job)
        //1..65535
        val ports = (1..100).toList()
        val jobs = ports.map { port ->
            scope.async {
                if (isPortOpen(ip, port)) {
                    openPorts.add(port)
                }
            }
        }

        jobs.forEach { it.await() } // Esperar a que todas las corrutinas finalicen
        job.complete() // Marcar el job como completado para evitar cancelaciones
        executor.shutdown() // Liberar recursos
        return openPorts
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        println("Entro4")
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 3000)
                true
            }
        } catch (e: Exception) {
            println(e.message)
            false
        }
    }
}