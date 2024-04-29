package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.lvarolpezfueyo_tfm.R
import com.itextpdf.kernel.pdf.PdfWriter
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph


class AnalisisActivity : AppCompatActivity() {

    private lateinit var editTextIp: EditText
    private lateinit var resumeIP: TextView
    private lateinit var openPorts: TextView
    private lateinit var buttonScan: Button
    private lateinit var textViewResult: TextView
    private lateinit var checkBox: CheckBox
    private lateinit var progressBar: ProgressBar
    private lateinit var generatePDF: ImageView
    private lateinit var share: ImageView

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
            generatePDF.isEnabled = false
            share.isEnabled = false

            resumeIP.text = "IP: ${editTextIp.text}";
            textViewResult.text = "Escaneando, los resultados aparecerán aquí";

            val ip = editTextIp.text.toString()
            scanPorts(ip)
        }

        generatePDF = findViewById(R.id.PDF);
        share = findViewById(R.id.Share);

        generatePDF.setOnClickListener {
            generatePdf()
        }

        share.setOnClickListener {
            shareText()
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

                            this@AnalisisActivity.runOnUiThread {
                                textViewResult.text = myResponse
                                openPorts.text = "Nº puertos abiertos: ${numOpenPorts.toString()}"
                                progressBar.visibility = View.GONE
                                generatePDF.isEnabled = true
                                share.isEnabled = true
                            }
                        }
                    }
                }
            })
        }
    }

    fun generatePdf() {
        try {
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val file = File(directory, "scan.pdf")
            val outputStream = FileOutputStream(file)
            val writer = PdfWriter(outputStream)

            // Crear un objeto PdfDocument para crear el PDF
            val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)

            // Crear un objeto Document para agregar contenido al PDF
            val document = Document(pdf)


            // Agregar el contenido del TextView al PDF

            val ip = editTextIp.text.toString()
            val numOpenPorts = openPorts.text.toString()
            val text = textViewResult.text.toString()

            val ipParagraph = Paragraph("Dirección IP: $ip")
            val numOpenPortsParagraph = Paragraph(numOpenPorts)
            val paragraph = Paragraph(text)

            document.add(ipParagraph)
            document.add(numOpenPortsParagraph)
            document.add(paragraph)

            document.close()
            pdf.close()
            Toast.makeText(
                this,
                "PDF generado, guardado en Mis archivos->Descargas",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            // Mostrar un mensaje Toast indicando que se ha producido un error al generar el PDF
            Toast.makeText(this, "Error generando el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, textViewResult.text.toString())
        startActivity(Intent.createChooser(intent, "Compartir con:"))
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