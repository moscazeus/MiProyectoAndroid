package com.caferaquelita.restauranteapp.models

/**
 * Modelo de producto para el inventario.
 */
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val imageUrl: String = ""
) 