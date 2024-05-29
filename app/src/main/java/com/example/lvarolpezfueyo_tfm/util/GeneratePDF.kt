package com.example.lvarolpezfueyo_tfm.util


import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.AccessController.getContext


class GeneratePDF() {

    /**
     * Crea un archivo PDF a partir de un texto dado y lo guarda en el almacenamiento temporal del dispositivo.
     *
     * @param text El texto a incluir en el archivo PDF.
     * @return El archivo PDF creado.
     * @throws IOException Si ocurre un error al escribir en el archivo.
     */
    @Throws(IOException::class)
    fun createPDF(context: Context, name: String, text: String?): File {
        val imagePath: File = File(context.filesDir, "pdfs")
        imagePath.mkdir()
        val pdfFile = File(imagePath, "$name.pdf")

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