package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.CashRegister

/**
 * Repositorio para la gestión de caja en Firestore.
 */
class CashRegisterRepository {
    // TODO: Métodos para apertura, cierre, retiros, consignaciones y resumen de caja.
    fun openCashRegister(amount: Double) {}
    fun registerSale(amount: Double, tip: Double) {}
    fun withdraw(amount: Double) {}
    fun deposit(amount: Double) {}
    fun getSummary(): CashRegister? = null
} 