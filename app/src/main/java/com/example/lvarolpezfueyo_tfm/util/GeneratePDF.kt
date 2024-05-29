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

class GeneratePDF() {

    /**
     * Crea un archivo PDF a partir de un texto dado y lo guarda en el almacenamiento temporal del dispositivo.
     *
     * @param text El texto a incluir en el archivo PDF.
     * @return El archivo PDF creado.
     * @throws IOException Si ocurre un error al escribir en el archivo.
     */
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