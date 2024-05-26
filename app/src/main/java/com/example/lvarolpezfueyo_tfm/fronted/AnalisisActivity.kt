package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lvarolpezfueyo_tfm.R
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private lateinit var generatePDF: ImageView
    private lateinit var share: ImageView
    private val niktoResult = mutableMapOf<Int?, Pair<String?, String?>>()
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val ip = result.data?.getStringExtra("ip")
                val port = result.data?.getIntExtra("port", 0)
                val niktoScan = result.data?.getStringExtra("scan")
                val ipAndScan = Pair(ip, niktoScan)
                niktoResult[port] = ipAndScan
            }
        }

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

            if (!niktoResult.values.any { it.first == ip }) {
                niktoResult.clear()
            }

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
            openPorts.text = "Nº puertos abiertos: 0"
            return
        } else {
            progressBar.visibility = View.VISIBLE

            val client = OkHttpClient.Builder()
                .connectTimeout(
                    60,
                    TimeUnit.SECONDS
                ) // Tiempo de espera para establecer la conexión
                .writeTimeout(60, TimeUnit.SECONDS) // Tiempo de espera para enviar la solicitud
                .readTimeout(60, TimeUnit.SECONDS) // Tiempo de espera para recibir la respuesta
                .build()

            val format = if (checkBox.isChecked) "false" else "true"
            //val url = "https://tfm-fkng5higzq-ew.a.run.app/scan/$ip/$format"
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
                            val myResponse = json.getString("stdout")
                            val openPortsJson = json.getJSONArray("openPorts")
                            val numOpenPorts = openPortsJson.length()

                            this@AnalisisActivity.runOnUiThread {
                                textViewResult.text = myResponse
                                openPorts.text = "Nº puertos abiertos: ${numOpenPorts.toString()}"
                                progressBar.visibility = View.GONE
                                generatePDF.isEnabled = true
                                share.isEnabled = true

                                // Obtener la lista de puertos abiertos
                                val openPortsList = mutableListOf<Int>()
                                for (i in 0 until numOpenPorts) {
                                    val port = openPortsJson.getString(i).toInt()
                                    openPortsList.add(port)

                                }

                                // Establecer los puertos clicables
                                val spannableString = setClickablePorts(myResponse, openPortsList)
                                textViewResult.text = spannableString
                                textViewResult.movementMethod = LinkMovementMethod.getInstance()
                                textViewResult.highlightColor = Color.TRANSPARENT
                            }
                        }
                    }
                }
            })
        }
    }

    fun setClickablePorts(nmapOutput: String, clickablePorts: List<Int>): SpannableString {
        val spannableString = SpannableString(nmapOutput)

        for (clickablePort in clickablePorts) {
            val tcpPortString = "$clickablePort/tcp"
            val udpPortString = "$clickablePort/udp"
            var startIndex = 0

            while (startIndex != -1) {
                startIndex = nmapOutput.indexOf(tcpPortString, startIndex)
                if (startIndex == -1) {
                    startIndex = nmapOutput.indexOf(udpPortString, startIndex)
                }

                if (startIndex != -1) {
                    val endIndex = startIndex + if (nmapOutput.startsWith(tcpPortString, startIndex)) tcpPortString.length else udpPortString.length

                    val portClickableSpan = object : ClickableSpan() {
                        override fun onClick(view: View) {
                            startNiktoActivity(clickablePort)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                        }
                    }

                    spannableString.setSpan(
                        portClickableSpan,
                        startIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    startIndex += endIndex
                }
            }
        }

        return spannableString
    }

    fun generatePdf() {
        try {
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(directory, "scan.pdf")
            val outputStream = FileOutputStream(file)
            val writer = PdfWriter(outputStream)
            val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
            val document = Document(pdf)
            val ip = editTextIp.text.toString()
            val numOpenPorts = openPorts.text.toString()
            val text = textViewResult.text.toString()
            val ipParagraph = Paragraph("Dirección IP: $ip")
            val numOpenPortsParagraph = Paragraph(numOpenPorts)
            val paragraph = Paragraph(text)

            /*
            if (niktoResult.isNotEmpty()) {
                for ((port, pair) in niktoResult) {
                    val niktoScan = pair.second
                    paragraph.add("\n $niktoScan \n")
                }
            }

             */

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
            e.printStackTrace()
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

    private fun startNiktoActivity(clickablePort: Int) {
        val intent = Intent(this@AnalisisActivity, NiktoAcitivy::class.java)
        intent.putExtra("ip", editTextIp.text.toString())
        intent.putExtra("port", clickablePort)
        intent.putExtra("niktoScan", niktoResult[clickablePort]?.second)
        resultLauncher.launch(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Dejar en blanco para evitar que el usuario vuelva a la pantalla anterior
    }

}