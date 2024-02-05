package com.example.lvarolpezfueyo_tfm.fronted

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.lvarolpezfueyo_tfm.R
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback


class AnalisisActivity : AppCompatActivity() {

    private lateinit var editTextIp: EditText
    private lateinit var resumeIP: TextView
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
            progressBar.visibility = View.VISIBLE
            scanPorts(ip)
        }
    }

    private fun scanPorts(ip: String) {
        val client = OkHttpClient()

        val url = "http://localhost:3000/scan/$ip"

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