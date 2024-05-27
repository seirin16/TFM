package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lvarolpezfueyo_tfm.R
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Document
import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory


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
                        try {
                            if (!response.isSuccessful) throw IOException("Código inesperado $response")
                            val responseBody = response.body!!.string()
                            if (responseBody != null) {
                                val xml = parseXml(responseBody)
                                val scanResult= parseNmap(xml)
                                val numOpenPorts = getNumPorts(xml)

                                this@AnalisisActivity.runOnUiThread {
                                    textViewResult.text = scanResult
                                    openPorts.text = "Nº puertos abiertos: ${numOpenPorts.toString()}"
                                    progressBar.visibility = View.GONE
                                    generatePDF.isEnabled = true
                                    share.isEnabled = true
                                }

                            } else {

                            }

                        } catch (e: IOException) {
                            Log.e("OkHttp", "IOException: ${e.message}")
                        }


                        /*
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

                                                */
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
                    val endIndex = startIndex + if (nmapOutput.startsWith(
                            tcpPortString,
                            startIndex
                        )
                    ) tcpPortString.length else udpPortString.length

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


    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        return builder.parse(xmlContent.byteInputStream(StandardCharsets.UTF_8))
    }

    fun parseNmap(doc: Document): String {
        val sb = StringBuilder()
        sb.append("Este es el resumen del escaneo\n\n")

        val startTime = doc.getElementsByTagName("nmaprun")
            .item(0).attributes.getNamedItem("startstr").nodeValue
        sb.append("Hora de inicio del escaneo: $startTime\n\n")

        val type =
            doc.getElementsByTagName("scaninfo").item(0).attributes.getNamedItem("type").nodeValue
        val protocol = doc.getElementsByTagName("scaninfo")
            .item(0).attributes.getNamedItem("protocol").nodeValue
        val numservices = doc.getElementsByTagName("scaninfo")
            .item(0).attributes.getNamedItem("numservices").nodeValue
        sb.append("Informacion del escaneo:\n")
        sb.append("Tipo de escaneo que se realizó: $type\n")
        sb.append("Protocolo de red utilizado: $protocol\n")
        sb.append("Número de puertos escaneados: $numservices\n\n")

        val state = doc.getElementsByTagName("extraports")
            .item(0).attributes.getNamedItem("state").nodeValue
        val numPortNotScan = doc.getElementsByTagName("extraports").item(0).attributes.getNamedItem(
            "count"
        ).nodeValue
        val reason = doc.getElementsByTagName("extrareasons")
            .item(0).attributes.getNamedItem("reason").nodeValue
        sb.append("Informacion de los puertos no escaneados:\n")
        sb.append("Número total de puertos: $numPortNotScan\n")
        sb.append("Estado de los puertos: $state\n")
        sb.append("Motivo: $reason\n\n")

        sb.append("Informacion de los puertos escaneados:\n")
        sb.append("PORT    STATE SERVICE\n")
        for (i in 0 until doc.getElementsByTagName("port").length) {
            val protocol = doc.getElementsByTagName("port")
                .item(i).attributes.getNamedItem("protocol").nodeValue
            val portid =
                doc.getElementsByTagName("port").item(i).attributes.getNamedItem("portid").nodeValue
            val state =
                doc.getElementsByTagName("state").item(i).attributes.getNamedItem("state").nodeValue
            val service = doc.getElementsByTagName("service")
                .item(0).attributes.getNamedItem("name").nodeValue

            sb.append("$portid/$protocol  $state  $service\n")
        }
        sb.append("\n")
        val summary = doc.getElementsByTagName("finished")
            .item(0).attributes.getNamedItem("summary").nodeValue
        sb.append("Resumen de la ejecución: $summary\n")

        return sb.toString()
    }

    fun getNumPorts(doc: Document): Int {

        return doc.getElementsByTagName("port").length
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Dejar en blanco para evitar que el usuario vuelva a la pantalla anterior
    }

}