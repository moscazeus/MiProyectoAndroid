package com.caferaquelita.restauranteapp.repositories
import android.content.Context
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.utils.PdfGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import com.caferaquelita.restauranteapp.repositories.CashRegisterRepository
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FieldValue





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

            // Generar archivo del reporte (aunque esté vacío)
            val pdfFile = pdfGenerator.generateInvoiceReportPdf(invoices, date)


            // Retornar URL del archivo local
            "file://${pdfFile.absolutePath}"
        } catch (e: Exception) {
            throw Exception("Error al generar reporte: ${e.message}")
        }
    }

    /**
     * Guardar factura en Firebase y actualizar caja.
     */
    suspend fun saveInvoice(invoice: Invoice) {
        try {
            // 1) sellamos con Timestamp
            val toSave = invoice.copy(createdAt = Timestamp.now())

            // 2) guardamos en Firestore
            invoicesCollection.document(toSave.id).set(toSave).await()

            // 3) sumamos a la caja en el doc "cash/status" (crea el campo si existe)
            firestore.collection("cash")
                .document("status")
                .update("current", FieldValue.increment(toSave.total))
                .await()

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
            val snapshot = invoicesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

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
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = Timestamp(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val end = Timestamp(cal.time)

            val snapshot = invoicesCollection
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThan("createdAt", end)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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
            val calStart = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                time = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val startTs = Timestamp(calStart.time)
            val endTs = Timestamp(calEnd.time)

            val snapshot = invoicesCollection
                .whereGreaterThanOrEqualTo("createdAt", startTs)
                .whereLessThanOrEqualTo("createdAt", endTs)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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