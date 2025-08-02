package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.ViewModel
import com.caferaquelita.restauranteapp.models.Invoice

/**
 * ViewModel para la facturación y cierre de mesas.
 */
class BillingViewModel : ViewModel() {
    // TODO: Métodos para generar factura, agregar propina, exportar PDF, etc.
    fun generateInvoice(orderId: String, tip: Double) {}
    fun exportInvoiceToPDF(invoice: Invoice) {}
} 