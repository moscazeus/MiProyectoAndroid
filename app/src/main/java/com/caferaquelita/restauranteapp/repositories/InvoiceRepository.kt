package com.caferaquelita.restauranteapp.repositories
import android.content.Context
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.utils.PdfGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio para la gestión de facturas.
 */
class InvoiceRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val invoicesCollection = firestore.collection("invoices")
    private val pdfGenerator = PdfGenerator(context)

    /**
     * Generar PDF de una factura individual.
     */
    suspend fun generateInvoicePdf(invoice: Invoice): String {
        return try {
            // Generar archivo local
            val pdfFile = pdfGenerator.generateInvoicePdf(invoice)
            
            // Retornar URL del archivo local
            "file://${pdfFile.absolutePath}"
        } catch (e: Exception) {
            throw Exception("Error al generar archivo: ${e.message}")
        }
    }

    /**
     * Generar reporte de facturas por fecha.
     */
    suspend fun generateInvoiceReportPdf(date: Date): String {
        return try {
            // Obtener facturas de la fecha específica
            val invoices = getInvoicesByDate(date)
            
            if (invoices.isEmpty()) {
                throw Exception("No hay facturas para la fecha seleccionada")
            }
            
            // Generar archivo del reporte
            val pdfFile = pdfGenerator.generateInvoiceReportPdf(invoices, date)
            
            // Retornar URL del archivo local
            "file://${pdfFile.absolutePath}"
        } catch (e: Exception) {
            throw Exception("Error al generar reporte: ${e.message}")
        }
    }

    /**
     * Guardar factura en Firebase.
     */
    suspend fun saveInvoice(invoice: Invoice) {
        try {
            invoicesCollection.document(invoice.id).set(invoice).await()
        } catch (e: Exception) {
            throw Exception("Error al guardar factura: ${e.message}")
        }
    }

    /**
     * Obtener factura por ID.
     */
    suspend fun getInvoiceById(invoiceId: String): Result<Invoice> {
        return try {
            val document = invoicesCollection.document(invoiceId).get().await()
            val invoice = document.toObject(Invoice::class.java)?.copy(id = document.id)
            
            if (invoice != null) {
                Result.success(invoice)
            } else {
                Result.failure(Exception("Factura no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener todas las facturas.
     */
    suspend fun getAllInvoices(): Result<List<Invoice>> {
        return try {
            val snapshot = invoicesCollection.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
            val invoices = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Invoice::class.java)?.copy(id = doc.id)
            }
            Result.success(invoices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener facturas por fecha específica.
     */
    suspend fun getInvoicesByDate(date: Date): List<Invoice> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis
            
            val snapshot = invoicesCollection
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThan("createdAt", endOfDay)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Invoice::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Obtener facturas por rango de fechas.
     */
    suspend fun getInvoicesByDateRange(startDate: Date, endDate: Date): List<Invoice> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.time = endDate
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis
            
            val snapshot = invoicesCollection
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Invoice::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
} 