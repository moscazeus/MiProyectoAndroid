package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.Invoice
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para el historial de facturas (sin funcionalidad de PDF).
 */
class InvoiceHistoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val invoicesCollection = firestore.collection("invoices")

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
} 