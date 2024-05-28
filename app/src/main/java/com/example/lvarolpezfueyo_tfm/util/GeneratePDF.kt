package com.example.lvarolpezfueyo_tfm.util


import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GeneratePDF(val context: Context) {

    @Throws(IOException::class)
    fun createPDF(text: String?): File {
        val pdfFile = File.createTempFile("scan", ".pdf")
        FileOutputStream(pdfFile).use { fileOutputStream ->
            PdfWriter(fileOutputStream).use { pdfWriter ->
                PdfDocument(pdfWriter).use { pdfDocument ->
                    Document(pdfDocument).use { document ->
                        pdfDocument.defaultPageSize = PageSize.A4
                        document.add(Paragraph(text))
                    }
                }
            }
        }
        return pdfFile
    }



}