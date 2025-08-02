package com.caferaquelita.restauranteapp.models

/**
 * Modelo de pedido y productos del pedido.
 */
data class Order(
    val id: String = "",
    val tableId: String = "",
    val waiterId: String = "",
    val products: List<OrderProduct> = listOf(),
    val total: Double = 0.0,
    val tip: Double = 0.0,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderProduct(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0
) 