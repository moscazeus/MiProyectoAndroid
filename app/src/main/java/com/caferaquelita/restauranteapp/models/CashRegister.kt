package com.caferaquelita.restauranteapp.models

/**
 * Modelo de caja y movimientos de caja.
 */
data class CashRegister(
    val id: String = "",
    val openingAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val totalTips: Double = 0.0,
    val totalIncome: Double = 0.0,
    val withdrawals: List<CashMovement> = listOf(),
    val deposits: List<CashMovement> = listOf(),
    val date: String = ""
)

data class CashMovement(
    val id: String = "",
    val amount: Double = 0.0,
    val type: String = "", // "withdrawal" o "deposit"
    val createdAt: Long = System.currentTimeMillis()
) 