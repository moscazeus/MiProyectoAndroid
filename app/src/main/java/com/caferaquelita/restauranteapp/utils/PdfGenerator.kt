package com.caferaquelita.restauranteapp.utils

import android.os.Environment
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.caferaquelita.restauranteapp.models.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

    // === Utilidad para escribir texto con salto de línea automático ===
    private fun drawTextBlock(
        page: PdfDocument.Page,
        text: String,
        startX: Float,
        startY: Float,
        paint: Paint,
        lineSpacing: Float = 6f,
        maxWidth: Float = 540f
    ): Float {
        val canvas = page.canvas
        val words = text.split(" ")
        var x = startX
        var y = startY

        for (word in words) {
            val w = paint.measureText("$word ")
            if (x + w > startX + maxWidth) {
                x = startX
                y += paint.textSize + lineSpacing
            }
            canvas.drawText("$word ", x, y, paint)
            x += w
        }
        return y + paint.textSize // devuelve la siguiente Y disponible
    }

    /** Generar **PDF real** de una factura individual. */
    fun generateInvoicePdf(invoice: Invoice): File {
        // 1) Prepara documento
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 en puntos (72dpi aprox)
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // 2) Pinceles
        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply { strokeWidth = 1f }

        var y = 40f

        // 3) Encabezado
        canvas.drawText("CAFÉ RAQUELITA", 40f, y, titlePaint); y += 10f
        y = drawTextBlock(page, "Sistema de Gestión", 40f, y + 10f, subPaint)
        canvas.drawLine(40f, y + 10f, 555f, y + 10f, linePaint)
        y += 30f

        // 4) Datos de factura
        canvas.drawText("Número de Factura:", 40f, y, boldPaint)
        canvas.drawText(invoice.invoiceNumber, 190f, y, subPaint); y += 18f

        canvas.drawText("Fecha:", 40f, y, boldPaint)
        canvas.drawText(dateFormat.format(invoice.createdAt.toDate()), 190f, y, subPaint); y += 18f

        canvas.drawText("Mesa:", 40f, y, boldPaint)
        canvas.drawText("${invoice.tableNumber}", 190f, y, subPaint); y += 18f

        canvas.drawText("Mesero:", 40f, y, boldPaint)
        canvas.drawText(invoice.waiterName, 190f, y, subPaint); y += 26f

        canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f

        // 5) Items
        canvas.drawText("PRODUCTOS", 40f, y, boldPaint); y += 14f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f

        for (it in invoice.items) {
            val left = "${it.productName} x${it.quantity}"
            val right = numberFormat.format(it.subtotal)

            // texto izquierdo
            canvas.drawText(left, 40f, y, subPaint)
            // texto derecho (alineado a la derecha)
            val w = subPaint.measureText(right)
            canvas.drawText(right, 555f - w, y, subPaint)
            y += 16f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 18f

        // 6) Totales
        fun drawLabelValue(label: String, value: String) {
            canvas.drawText(label, 320f, y, boldPaint)
            val w = subPaint.measureText(value)
            canvas.drawText(value, 555f - w, y, subPaint)
            y += 18f
        }

        drawLabelValue("SUBTOTAL:", numberFormat.format(invoice.subtotal))
        drawLabelValue("IVA (19%):", numberFormat.format(invoice.tax))
        if (invoice.tip > 0) {
            drawLabelValue("Propina:", numberFormat.format(invoice.tip))
        }
        drawLabelValue("TOTAL:", numberFormat.format(invoice.total))

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 18f

        y = drawTextBlock(page, "¡Gracias por su visita!", 40f, y, subPaint)
        y = drawTextBlock(page, "Café Raquelita - Sistema de Gestión", 40f, y + 6f, subPaint)

        // 7) Cierra página y escribe archivo
        pdf.finishPage(page)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir ?: context.filesDir, "factura_${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out -> pdf.writeTo(out) }

        pdf.close()
        return file
    }

    /** Generar **PDF real** de reporte por fecha (listado simple). */
    fun generateInvoiceReportPdf(invoices: List<Invoice>, date: java.util.Date): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val boldPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply { strokeWidth = 1f }

        var y = 40f
        canvas.drawText("REPORTE DE FACTURAS", 40f, y, titlePaint); y += 10f
        canvas.drawText("Fecha: ${dateOnlyFormat.format(date)}", 40f, y + 20f, subPaint)
        y += 40f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f

        for (inv in invoices) {
            val row = "${inv.invoiceNumber} | Mesa ${inv.tableNumber} | ${inv.waiterName} | ${inv.items.size} ítems"
            val total = numberFormat.format(inv.total)
            canvas.drawText(row, 40f, y, subPaint)
            val w = subPaint.measureText(total)
            canvas.drawText(total, 555f - w, y, subPaint)
            y += 16f
            if (y > 790f) break // (simple: cortar si se llena la hoja)
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint); y += 16f

        val totalFact = "Total Facturas: ${invoices.size}"
        val totalVentas = "Total Ventas: ${numberFormat.format(invoices.sumOf { it.total })}"
        canvas.drawText(totalFact, 40f, y, boldPaint); y += 16f
        canvas.drawText(totalVentas, 40f, y, boldPaint); y += 16f

        pdf.finishPage(page)

        val safeName = SimpleDateFormat("yyyyMMdd", Locale("es", "CO")).format(date) // ← sin “/”
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir ?: context.filesDir, "reporte_facturas_$safeName.pdf")
        FileOutputStream(file).use { out -> pdf.writeTo(out) }


        pdf.close()
        return file
    }
}
