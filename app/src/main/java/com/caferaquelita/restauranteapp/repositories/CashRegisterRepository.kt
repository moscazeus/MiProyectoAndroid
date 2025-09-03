package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.CashMovement
import com.caferaquelita.restauranteapp.models.CashRegister
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para la gestión de caja en Firestore.
 *
 * Cada documento en la colección "cash_registers" representa una sesión de caja (un día de trabajo).
 * Se almacena el monto de apertura, el monto actual, las propinas, los ingresos,
 * y listas de retiros y depósitos con sus fechas.
 */
class CashRegisterRepository {

    private val db = FirebaseFirestore.getInstance()
    private val cashRegisterCollection = db.collection("cash_registers")

    /**
     * Abre una nueva caja con un monto inicial.
     * Crea un documento en Firestore con los valores iniciales.
     */
    fun openCashRegister(amount: Double, callback: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newRegister = CashRegister(
            id = id,
            openingAmount = amount,
            currentAmount = amount,
            totalTips = 0.0,
            totalIncome = 0.0,
            withdrawals = listOf(),
            deposits = listOf(),
            date = dateString
        )

        cashRegisterCollection.document(id)
            .set(newRegister)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    /**
     * Crea una nueva sesión de caja básica (sin monto de apertura).
     */
    fun createSession(callback: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val newSession = CashRegister(
            id = id,
            openingAmount = 0.0,
            currentAmount = 0.0,
            totalTips = 0.0,
            totalIncome = 0.0,
            withdrawals = listOf(),
            deposits = listOf(),
            date = dateString
        )

        cashRegisterCollection
            .document(id)
            .set(newSession)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    /**
     * Registra una venta: suma el importe y la propina al monto actual.
     * También incrementa el total de ingresos y de propinas.
     */
    fun registerSale(amount: Double, tip: Double, callback: (Boolean) -> Unit) {
        getSummary { register ->
            if (register != null) {
                val updatedRegister = register.copy(
                    currentAmount = register.currentAmount + amount + tip,
                    totalIncome = register.totalIncome + amount,
                    totalTips = register.totalTips + tip
                )
                cashRegisterCollection.document(register.id)
                    .set(updatedRegister)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            } else {
                callback(false)
            }
        }
    }

    /**
     * Registra un retiro de caja.
     * Agrega un movimiento de tipo "withdrawal" y descuenta el monto del total.
     */
    fun withdraw(amount: Double, callback: (Boolean) -> Unit) {
        getSummary { register ->
            if (register != null) {
                if (amount > register.currentAmount) {
                    // No hay suficiente efectivo en caja
                    callback(false)
                    return@getSummary
                }
                val movement = CashMovement(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = "withdrawal",
                    createdAt = System.currentTimeMillis()
                )
                val updatedWithdrawals = register.withdrawals + movement
                val updatedRegister = register.copy(
                    withdrawals = updatedWithdrawals,
                    currentAmount = register.currentAmount - amount
                )
                cashRegisterCollection.document(register.id)
                    .set(updatedRegister)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            } else {
                callback(false)
            }
        }
    }

    /**
     * Registra un depósito en caja.
     * Agrega un movimiento de tipo "deposit" y suma el monto al total.
     */
    fun deposit(amount: Double, callback: (Boolean) -> Unit) {
        getSummary { register ->
            if (register != null) {
                val movement = CashMovement(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = "deposit",
                    createdAt = System.currentTimeMillis()
                )
                val updatedDeposits = register.deposits + movement
                val updatedRegister = register.copy(
                    deposits = updatedDeposits,
                    currentAmount = register.currentAmount + amount
                )
                cashRegisterCollection.document(register.id)
                    .set(updatedRegister)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            } else {
                callback(false)
            }
        }
    }

    // ✅ Suma a currentAmount (y a totalIncome) en la sesión más reciente
    suspend fun addToCurrent(amount: Double) {
        val last = cashRegisterCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = last.documents.firstOrNull()
            ?: throw IllegalStateException("No hay sesión de caja creada")

        val current = (doc.getDouble("currentAmount") ?: 0.0) + amount
        val income  = (doc.getDouble("totalIncome")   ?: 0.0) + amount

        cashRegisterCollection.document(doc.id)
            .update(mapOf("currentAmount" to current, "totalIncome" to income))
            .await()
    }


    /**
     * Obtiene la sesión de caja más reciente.
     * Devuelve null si no existe ninguna sesión.
     */
    fun getSummary(callback: (CashRegister?) -> Unit) {
        cashRegisterCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val register = doc.toObject(CashRegister::class.java)?.copy(id = doc.id)
                    callback(register)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
