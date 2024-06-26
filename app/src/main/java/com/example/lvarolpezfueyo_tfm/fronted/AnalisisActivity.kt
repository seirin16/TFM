package com.example.lvarolpezfueyo_tfm.fronted

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.core.content.FileProvider
import com.example.lvarolpezfueyo_tfm.R
import com.example.lvarolpezfueyo_tfm.util.GeneratePDF
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory


class AnalisisActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var portLinearLayout: LinearLayout

    private lateinit var generalInformation: TextView
    private lateinit var startTime: TextView
    private lateinit var scaninfo: TextView
    private lateinit var extraports: TextView
    private lateinit var port: TextView
    private lateinit var finished: TextView

    private lateinit var editTextIp: EditText
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


        generalInformation = findViewById(R.id.generalInformation)
        editTextIp = findViewById(R.id.editTextIP)
        buttonScan = findViewById(R.id.scanButton)
        textViewResult = findViewById(R.id.scanResults)
        checkBox = findViewById(R.id.checkBox)
        progressBar = findViewById(R.id.progressBar)

        scrollView = findViewById(R.id.scrollView)
        portLinearLayout = findViewById(R.id.portLinearLayout)

        startTime = findViewById(R.id.startTime)
        scaninfo = findViewById(R.id.scaninfo)
        extraports = findViewById(R.id.extraports)
        port = findViewById(R.id.port)
        finished = findViewById(R.id.finished)

        generatePDF = findViewById(R.id.PDF);
        share = findViewById(R.id.Share);

        generatePDF.isEnabled = false
        share.isEnabled = false

        buttonScan.setOnClickListener {
            if(editTextIp.text.toString().isNotEmpty()){
                generatePDF.isEnabled = false
                share.isEnabled = false
                buttonScan.isEnabled = false
                checkBox.isEnabled = false
                val ip = editTextIp.text.toString()
                if (!niktoResult.values.any { it.first == ip }) {
                    niktoResult.clear()
                }
                portLinearLayout.removeAllViews();
                scanPorts(ip)
            }
        }

        generatePDF.setOnClickListener {
            val generatePDF =
                GeneratePDF()
            val name = editTextIp.text.toString()
            val text = textToShare()
            val pdf = generatePDF.createPDF(this, name, text)

            sharePdf(pdf)
        }

        share.setOnClickListener {
            shareText()
        }


    }

    /**
     * Función que realiza un escaneo de puertos en una dirección IP específica.
     *
     * @param ip Dirección IP a escanear.
     */
    private fun scanPorts(ip: String) {
        // Verifica si la dirección IP es válida
        if (!isValidIP(ip)) {
            textViewResult.text = "Dirección IP no válida"
            scrollView.visibility = View.INVISIBLE
            progressBar.visibility = View.GONE
            textViewResult.visibility = View.VISIBLE
            checkBox.isEnabled = true
            buttonScan.isEnabled = true

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
                        checkBox.isEnabled = true
                        buttonScan.isEnabled = true
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
                                val numOpenPorts = getNumPorts(xml)

                                this@AnalisisActivity.runOnUiThread {
                                    generalInformation(numOpenPorts, xml)
                                    startTime(xml)
                                    scaninfo(xml)
                                    extraports(xml)
                                    information_port(xml)
                                    information_finished(xml)
                                    scrollView.visibility = View.VISIBLE
                                    progressBar.visibility = View.GONE
                                    textViewResult.visibility = View.GONE
                                    generatePDF.isEnabled = true
                                    share.isEnabled = true
                                    buttonScan.isEnabled = true
                                    checkBox.isEnabled = true
                                }

                            } else {

                            }

                        } catch (e: IOException) {
                            Log.e("OkHttp", "IOException: ${e.message}")
                        }

                    }
                }
            })
        }
    }

    /**
     * Función que comparte el texto del resumen del escaneo.
     */
    private fun shareText() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, textToShare())
        startActivity(Intent.createChooser(intent, "Compartir con:"))
    }

    /**
     * Función que comparte un archivo PDF.
     *
     * @param pdfFile El archivo PDF a compartir.
     */
    private fun sharePdf(pdfFile: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(this, "com.example.lvarolpezfueyo_tfm.fileprovider", pdfFile)
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Compartir con:"))
    }

    /**
     * Función que genera el texto del resumen del escaneo.
     *
     * @return El texto del resumen del escaneo.
     */
    fun textToShare(): String {
        val sb = StringBuilder()
        sb.append("Este es el resumen del escaneo\n\n")

        sb.append("Información general:\n")
        sb.append(generalInformation.text).append("\n\n")

        sb.append("Hora de inicio del escaneo:\n")
        sb.append(startTime.text).append("\n\n")

        sb.append("Información del escaneo:\n")
        sb.append(scaninfo.text).append("\n\n")

        sb.append("Información de los puertos no escaneados:\n")
        sb.append(extraports.text).append("\n\n")

        sb.append("Información de los puertos escaneados:\n")
        sb.append(port.text).append("\n\n")

        sb.append("Resumen ejecución:\n")
        sb.append(finished.text).append("\n\n")

        return sb.toString()
    }

    /**
     * Función que comprueba si una cadena de texto representa una dirección IP válida.
     *
     * @param ip La cadena de texto a comprobar.
     * @return `true` si la cadena de texto representa una dirección IP válida, `false` en caso contrario.
     */
    private fun isValidIP(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Función que inicia la actividad NiktoActivity con la información del puerto seleccionado.
     *
     * @param clickablePort El número de puerto seleccionado.
     */
    private fun startNiktoActivity(clickablePort: Int) {
        val intent = Intent(this@AnalisisActivity, NiktoAcitivy::class.java)
        intent.putExtra("ip", editTextIp.text.toString())
        intent.putExtra("port", clickablePort)
        intent.putExtra("niktoScan", niktoResult[clickablePort]?.second)
        resultLauncher.launch(intent)
    }

    /**
     * Función que parsea una cadena de texto XML y devuelve un objeto Document que la representa.
     *
     * @param xmlContent La cadena de texto XML a parsear.
     * @return El objeto Document que representa la cadena de texto XML.
     */
    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        return builder.parse(xmlContent.byteInputStream(StandardCharsets.UTF_8))
    }

    /**
     * Función que muestra la información general del escaneo en la interfaz de usuario.
     *
     * @param numOpenPorts El número de puertos abiertos encontrados en el escaneo.
     * @param doc El objeto Document que representa el resultado del escaneo en formato XML.
     */
    fun generalInformation(numOpenPorts: Int, doc: Document) {
        val sb = StringBuilder()

        sb.append("IP: ${editTextIp.text}\n")
        sb.append("Número de puertos abiertos: $numOpenPorts")
        if (checkBox.isChecked) {
            val os = doc.getElementsByTagName("osmatch")
                .item(0)?.attributes?.getNamedItem("name")?.nodeValue
                ?: "Unknown OS"
            val accuracy = doc.getElementsByTagName("osmatch")
                .item(0)?.attributes?.getNamedItem("accuracy")?.nodeValue
                ?.toIntOrNull()
                ?.let { if (it < 0) 0 else it } // ensure accuracy is not negative
                ?: 0

            if (os == "Unknown OS") {
                sb.append("\nSistema operativo:$os")
            } else {
                sb.append("\nSistema operativo: $os con una precisión del $accuracy%")
            }
        }

        generalInformation.text = sb.toString();
    }

    /**
     * Función que obtiene y muestra la hora de inicio de un escaneo en un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrá la hora de inicio.
     */
    fun startTime(doc: Document) {
        val sb = StringBuilder()
        val startstr = doc.getElementsByTagName("nmaprun")
            .item(0).attributes.getNamedItem("startstr").nodeValue

        sb.append(startstr)

        startTime.text = sb.toString();
    }

    /**
     * Función que obtiene y muestra la información de escaneo de un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrá la información de escaneo.
     */
    fun scaninfo(doc: Document) {
        val sb = StringBuilder()

        val type =
            doc.getElementsByTagName("scaninfo").item(0).attributes.getNamedItem("type").nodeValue
        val protocol = doc.getElementsByTagName("scaninfo")
            .item(0).attributes.getNamedItem("protocol").nodeValue
        val numservices = doc.getElementsByTagName("scaninfo")
            .item(0).attributes.getNamedItem("numservices").nodeValue
        sb.append("Tipo de escaneo que se realizó: $type\n")
        sb.append("Protocolo de red utilizado: $protocol\n")
        sb.append("Número de puertos escaneados: $numservices")

        scaninfo.text = sb.toString()
    }

    /**
     * Función que obtiene y muestra la información de puertos adicionales de un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrá la información de puertos adicionales.
     */
    fun extraports(doc: Document) {
        val sb = StringBuilder()

        val state = doc.getElementsByTagName("extraports")
            .item(0).attributes.getNamedItem("state").nodeValue
        val numPortNotScan = doc.getElementsByTagName("extraports").item(0).attributes.getNamedItem(
            "count"
        ).nodeValue
        val reason = doc.getElementsByTagName("extrareasons")
            .item(0).attributes.getNamedItem("reason").nodeValue
        sb.append("Número total de puertos: $numPortNotScan\n")
        sb.append("Estado de los puertos: $state\n")
        sb.append("Motivo: $reason")

        extraports.text = sb.toString()
    }

    /**
     * Función que obtiene y muestra la información de los puertos de un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrá la información de los puertos.
     */
    fun information_port(doc: Document) {

        portLinearLayout.orientation = LinearLayout.VERTICAL

        val sb = StringBuilder()

        sb.append("PORT STATE SERVICE\n")


        for (i in 0 until doc.getElementsByTagName("port").length) {
            val protocol = doc.getElementsByTagName("port")
                .item(i).attributes.getNamedItem("protocol").nodeValue
            val portid =
                doc.getElementsByTagName("port").item(i).attributes.getNamedItem("portid").nodeValue
            val state =
                doc.getElementsByTagName("state").item(i).attributes.getNamedItem("state").nodeValue
            val service = doc.getElementsByTagName("service")
                .item(0).attributes.getNamedItem("name").nodeValue

            sb.append("$portid/$protocol $state $service\n")

            val button = Button(this)
            button.setTextColor(Color.BLACK)
            button.text = "$portid/$protocol  $state  $service"

            button.tag = portid // Asignar el portId como tag del botón
            button.setOnClickListener {
                val portId = button.tag as String // Recuperar el portId del tag
                startNiktoActivity(portId.toInt()) // Pasar el portId a la actividad
            }

            port.text = sb.toString()
            portLinearLayout.addView(button)
        }
    }

    /**
     * Función que obtiene y muestra la información de finalización de un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrá la información de finalización.
     */
    fun information_finished(doc: Document) {
        val sb = StringBuilder()

        val summary = doc.getElementsByTagName("finished")
            .item(0).attributes.getNamedItem("summary").nodeValue
        sb.append(summary)

        finished.text = sb.toString()
    }

    /**
     * Función que obtiene el número de puertos de un documento XML.
     *
     * @param doc El documento XML a partir del cual se obtendrán los puertos.
     * @return El número de elementos "port" en el documento XML.
     */
    fun getNumPorts(doc: Document): Int {
        return doc.getElementsByTagName("port").length
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Dejar en blanco para evitar que el usuario vuelva a la pantalla anterior
    }

}