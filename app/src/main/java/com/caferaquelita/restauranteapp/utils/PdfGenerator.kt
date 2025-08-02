package com.caferaquelita.restauranteapp.utils

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.caferaquelita.restauranteapp.models.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para generar archivos HTML de facturas y reportes.
 * Versión simplificada que genera HTML con estilo PDF.
 */
class PdfGenerator(private val context: Context) {
    
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    
    /**
     * Generar HTML de una factura individual.
     */
    fun generateInvoicePdf(invoice: Invoice): File {
        val fileName = "factura_${invoice.invoiceNumber}.html"
        val file = File(context.filesDir, fileName)
        
        try {
            val htmlContent = buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html>")
                appendLine("<head>")
                appendLine("<meta charset='UTF-8'>")
                appendLine("<title>Factura ${invoice.invoiceNumber}</title>")
                appendLine("<style>")
                appendLine("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f9f9f9; }")
                appendLine(".header { text-align: center; color: #2d5016; margin-bottom: 30px; }")
                appendLine(".header h1 { font-size: 28px; margin: 0; color: #2d5016; }")
                appendLine(".header h2 { font-size: 16px; margin: 5px 0; color: #666; }")
                appendLine(".separator { border-top: 2px solid #2d5016; margin: 20px 0; }")
                appendLine(".invoice-info { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 20px 0; }")
                appendLine(".info-item { padding: 8px; background-color: #f0f0f0; border-radius: 5px; }")
                appendLine(".info-label { font-weight: bold; color: #2d5016; }")
                appendLine(".products-table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                appendLine(".products-table th { background-color: #2d5016; color: white; padding: 12px; text-align: left; }")
                appendLine(".products-table td { padding: 10px; border-bottom: 1px solid #ddd; }")
                appendLine(".products-table tr:nth-child(even) { background-color: #f9f9f9; }")
                appendLine(".totals { margin-top: 30px; }")
                appendLine(".total-item { display: flex; justify-content: space-between; padding: 8px 0; }")
                appendLine(".total-final { font-size: 18px; font-weight: bold; color: #2d5016; border-top: 2px solid #2d5016; padding-top: 10px; }")
                appendLine(".footer { text-align: center; margin-top: 40px; color: #2d5016; font-style: italic; }")
                appendLine("</style>")
                appendLine("</head>")
                appendLine("<body>")
                
                // Header
                appendLine("<div class='header'>")
                appendLine("<h1>CAFÉ RAQUELITA</h1>")
                appendLine("<h2>Sistema de Gestión</h2>")
                appendLine("</div>")
                appendLine("<div class='separator'></div>")
                
                // Información de la factura
                appendLine("<div class='invoice-info'>")
                appendLine("<div class='info-item'><span class='info-label'>Número de Factura:</span> ${invoice.invoiceNumber}</div>")
                appendLine("<div class='info-item'><span class='info-label'>Fecha:</span> ${dateFormat.format(Date(invoice.createdAt))}</div>")
                appendLine("<div class='info-item'><span class='info-label'>Mesa:</span> ${invoice.tableNumber}</div>")
                appendLine("<div class='info-item'><span class='info-label'>Mesero:</span> ${invoice.waiterName}</div>")
                appendLine("</div>")
                
                // Tabla de productos
                appendLine("<table class='products-table'>")
                appendLine("<thead>")
                appendLine("<tr>")
                appendLine("<th>Producto</th>")
                appendLine("<th>Cantidad</th>")
                appendLine("<th>Precio Unit.</th>")
                appendLine("<th>Subtotal</th>")
                appendLine("</tr>")
                appendLine("</thead>")
                appendLine("<tbody>")
                
                invoice.items.forEach { item ->
                    appendLine("<tr>")
                    appendLine("<td>${item.productName}</td>")
                    appendLine("<td>${item.quantity}</td>")
                    appendLine("<td>${numberFormat.format(item.productPrice)}</td>")
                    appendLine("<td>${numberFormat.format(item.subtotal)}</td>")
                    appendLine("</tr>")
                }
                
                appendLine("</tbody>")
                appendLine("</table>")
                
                // Totales
                appendLine("<div class='totals'>")
                appendLine("<div class='total-item'><span>Subtotal (IVA incluido):</span> <span>${numberFormat.format(invoice.subtotal)}</span></div>")
                if (invoice.tip > 0) {
                    val tipPercentage = ((invoice.tip / invoice.subtotal) * 100).toInt()
                    appendLine("<div class='total-item'><span>Propina (${tipPercentage}%):</span> <span>${numberFormat.format(invoice.tip)}</span></div>")
                }
                appendLine("<div class='total-item total-final'><span>TOTAL:</span> <span>${numberFormat.format(invoice.total)}</span></div>")
                appendLine("</div>")
                
                // Footer
                appendLine("<div class='footer'>")
                appendLine("<p>¡Gracias por su visita!</p>")
                appendLine("<p>Café Raquelita - Sistema de Gestión</p>")
                appendLine("</div>")
                
                appendLine("</body>")
                appendLine("</html>")
            }
            
            file.writeText(htmlContent)
            return file
        } catch (e: Exception) {
            throw Exception("Error al generar factura: ${e.message}")
        }
    }
    
    /**
     * Generar HTML con reporte de facturas por fecha.
     */
    fun generateInvoiceReportPdf(invoices: List<Invoice>, date: Date): File {
        val fileName = "reporte_facturas_${dateOnlyFormat.format(date)}.html"
        val file = File(context.filesDir, fileName)
        
        try {
            val htmlContent = buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html>")
                appendLine("<head>")
                appendLine("<meta charset='UTF-8'>")
                appendLine("<title>Reporte Facturas ${dateOnlyFormat.format(date)}</title>")
                appendLine("<style>")
                appendLine("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f9f9f9; }")
                appendLine(".header { text-align: center; color: #2d5016; margin-bottom: 30px; }")
                appendLine(".header h1 { font-size: 24px; margin: 0; color: #2d5016; }")
                appendLine(".header h2 { font-size: 16px; margin: 5px 0; color: #666; }")
                appendLine(".separator { border-top: 2px solid #2d5016; margin: 20px 0; }")
                appendLine(".invoices-table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                appendLine(".invoices-table th { background-color: #2d5016; color: white; padding: 12px; text-align: left; }")
                appendLine(".invoices-table td { padding: 10px; border-bottom: 1px solid #ddd; }")
                appendLine(".invoices-table tr:nth-child(even) { background-color: #f9f9f9; }")
                appendLine(".summary { margin-top: 30px; }")
                appendLine(".summary-item { display: flex; justify-content: space-between; padding: 8px 0; }")
                appendLine(".summary-final { font-size: 18px; font-weight: bold; color: #2d5016; border-top: 2px solid #2d5016; padding-top: 10px; }")
                appendLine(".footer { text-align: center; margin-top: 40px; color: #2d5016; font-style: italic; }")
                appendLine("</style>")
                appendLine("</head>")
                appendLine("<body>")
                
                // Header
                appendLine("<div class='header'>")
                appendLine("<h1>REPORTE DE FACTURAS</h1>")
                appendLine("<h2>Fecha: ${dateOnlyFormat.format(date)}</h2>")
                appendLine("</div>")
                appendLine("<div class='separator'></div>")
                
                // Tabla de facturas
                appendLine("<table class='invoices-table'>")
                appendLine("<thead>")
                appendLine("<tr>")
                appendLine("<th>Factura</th>")
                appendLine("<th>Mesa</th>")
                appendLine("<th>Mesero</th>")
                appendLine("<th>Productos</th>")
                appendLine("<th>Total</th>")
                appendLine("</tr>")
                appendLine("</thead>")
                appendLine("<tbody>")
                
                invoices.forEach { invoice ->
                    appendLine("<tr>")
                    appendLine("<td>${invoice.invoiceNumber}</td>")
                    appendLine("<td>${invoice.tableNumber}</td>")
                    appendLine("<td>${invoice.waiterName}</td>")
                    appendLine("<td>${invoice.items.size}</td>")
                    appendLine("<td>${numberFormat.format(invoice.total)}</td>")
                    appendLine("</tr>")
                }
                
                appendLine("</tbody>")
                appendLine("</table>")
                
                // Resumen
                val totalInvoices = invoices.size
                val totalAmount = invoices.sumOf { it.total }
                val totalTax = invoices.sumOf { it.tax }
                val totalTip = invoices.sumOf { it.tip }
                
                appendLine("<div class='summary'>")
                appendLine("<div class='summary-item'><span>Total Facturas:</span> <span>$totalInvoices</span></div>")
                appendLine("<div class='summary-item'><span>Total Ventas:</span> <span>${numberFormat.format(totalAmount)}</span></div>")
                appendLine("<div class='summary-item'><span>Total IVA:</span> <span>${numberFormat.format(totalTax)}</span></div>")
                appendLine("<div class='summary-item summary-final'><span>Total Propinas:</span> <span>${numberFormat.format(totalTip)}</span></div>")
                appendLine("</div>")
                
                // Footer
                appendLine("<div class='footer'>")
                appendLine("<p>Café Raquelita - Sistema de Gestión</p>")
                appendLine("</div>")
                
                appendLine("</body>")
                appendLine("</html>")
            }
            
            file.writeText(htmlContent)
            return file
        } catch (e: Exception) {
            throw Exception("Error al generar reporte: ${e.message}")
        }
    }
    
    /**
     * Convertir HTML a PDF usando WebView (opcional).
     * Esta función puede ser usada para generar PDF real si es necesario.
     */
    fun convertHtmlToPdf(htmlFile: File): File {
        val pdfFile = File(context.filesDir, htmlFile.nameWithoutExtension + ".pdf")
        
        // Por ahora, simplemente copiamos el HTML como PDF
        // En una implementación real, usarías WebView para renderizar HTML a PDF
        htmlFile.copyTo(pdfFile, overwrite = true)
        
        return pdfFile
    }
} 