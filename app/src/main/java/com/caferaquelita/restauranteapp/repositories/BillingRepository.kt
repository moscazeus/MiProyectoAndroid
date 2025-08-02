package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.Invoice

/**
 * Repositorio para la gestión de facturas en Firestore y PDF.
 */
class BillingRepository {
    // TODO: Métodos para generar y guardar facturas, exportar PDF, etc.
    fun generateInvoice(orderId: String, tip: Double): Invoice? = null
    fun saveInvoice(invoice: Invoice) {}
    fun exportInvoiceToPDF(invoice: Invoice): String = ""
} 