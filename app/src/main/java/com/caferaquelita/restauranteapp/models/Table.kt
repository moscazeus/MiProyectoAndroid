package com.caferaquelita.restauranteapp.models

import java.util.Date

/**
 * Modelo de datos para una mesa del restaurante.
 */
data class Table(
    val id: String = "",
    val number: Int = 0,
    val status: TableStatus = TableStatus.FREE,
    val waiterId: String = "",
    val waiterName: String = "",
    val openTime: Date? = null,
    val totalAmount: Double = 0.0,
    val tipAmount: Double = 0.0,
    val items: List<TableItem> = emptyList()
)

/**
 * Estados posibles de una mesa.
 */
enum class TableStatus {
    FREE,      // Libre
    OCCUPIED,  // Ocupada
    RESERVED   // Reservada
}

/**
 * Item de la mesa (producto con cantidad).
 */
data class TableItem(
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0
) 