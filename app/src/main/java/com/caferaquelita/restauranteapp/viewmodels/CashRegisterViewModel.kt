package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.ViewModel
import com.caferaquelita.restauranteapp.models.CashRegister

/**
 * ViewModel para el manejo de caja.
 */
class CashRegisterViewModel : ViewModel() {
    // TODO: Métodos para apertura, cierre, retiros, consignaciones y resumen de caja.
    fun openCashRegister(amount: Double) {}
    fun registerSale(amount: Double, tip: Double) {}
    fun withdraw(amount: Double) {}
    fun deposit(amount: Double) {}
    fun getSummary(): CashRegister? = null
} 