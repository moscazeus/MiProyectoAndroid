package com.caferaquelita.restauranteapp.models

/**
 * Modelo de factura generada al cerrar una mesa.
 */
data class Invoice(
    val id: String = "",
    val invoiceNumber: String = "",
    val tableId: String = "",
    val tableNumber: Int = 0,
    val waiterId: String = "",
    val waiterName: String = "",
    val items: List<TableItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val tip: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "Efectivo",
    val customerName: String = "",
    val customerDocument: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val pdfUrl: String? = null,
    val status: InvoiceStatus = InvoiceStatus.PAID
)

enum class InvoiceStatus {
    PAID,
    PENDING,
    CANCELLED
} 