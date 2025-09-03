package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ViewModel para el manejo de caja.
 */
class CashRegisterViewModel : ViewModel() {

    // 1) El estado de la apertura en Firestore (true = éxito, false = fallo)
    private val _openResult = MutableLiveData<Boolean>()
    val openResult: LiveData<Boolean> get() = _openResult

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * 2) Envía a Firestore la apertura de caja con el monto dado
     *    y publica en openResult si fue exitoso o no.
     */
    fun openCashRegister(amount: Double) {
        // Aquí defines la estructura de tu documento en Firestore:
        val data = mapOf(
            "openingAmount" to amount,
            "timestamp"     to System.currentTimeMillis()
        )
        firestore.collection("cashRegister")
            .document("open")         // o un ID dinámico si quieres historial
            .set(data)
            .addOnSuccessListener { _openResult.value = true }
            .addOnFailureListener { _openResult.value = false }
    }

    // (Opcional) otros métodos para ventas, retiros, depósitos...
    fun registerSale(amount: Double, tip: Double) { /* ... */ }
    fun withdraw(amount: Double)                { /* ... */ }
    fun deposit(amount: Double)                 { /* ... */ }
    // etc.
}
