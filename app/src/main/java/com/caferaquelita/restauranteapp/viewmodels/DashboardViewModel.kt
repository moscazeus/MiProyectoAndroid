package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.repositories.FinancialRepository
import java.util.*

/**
 * ViewModel para el dashboard y gestión financiera.
 */
class DashboardViewModel : ViewModel() {
    
    private val financialRepository = FinancialRepository()
    
    // LiveData para el dashboard
    private val _dashboardData = MutableLiveData<DashboardData>()
    val dashboardData: LiveData<DashboardData> = _dashboardData
    
    // LiveData para transacciones
    private val _transactions = MutableLiveData<List<FinancialTransaction>>()
    val transactions: LiveData<List<FinancialTransaction>> = _transactions
    
    // LiveData para estados de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData para mensajes
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    // Período actual del dashboard
    private var currentPeriod = "today"
    private var currentDateRange = DateRange()
    
    init {
        loadDashboardData()
        loadTransactions()
    }
    
    /**
     * Carga los datos del dashboard para el período especificado.
     */
    fun loadDashboardData(period: String = "today") {
        _isLoading.value = true
        currentPeriod = period
        
        // Calcular el rango de fechas según el período
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        val startDate = when (period) {
            "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            "week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.time
            }
            "month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }
            "year" -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.time
            }
            else -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
        }
        
        currentDateRange = DateRange(startDate, endDate)
        
        // Cargar datos desde el repositorio
        financialRepository.getDashboardData(startDate, endDate) { dashboardData ->
            _dashboardData.value = dashboardData
            _isLoading.value = false
        }
    }
    
    /**
     * Carga las transacciones financieras.
     */
    fun loadTransactions() {
        _isLoading.value = true
        financialRepository.getTransactions { transactions ->
            _transactions.value = transactions
            _isLoading.value = false
        }
    }
    
    /**
     * Agrega una nueva transacción financiera.
     */
    fun addTransaction(transaction: FinancialTransaction) {
        _isLoading.value = true
        financialRepository.addTransaction(transaction) { success ->
            if (success) {
                _message.value = "Transacción agregada exitosamente"
                loadTransactions()
                loadDashboardData(currentPeriod)
            } else {
                _message.value = "Error al agregar la transacción"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Actualiza una transacción existente.
     */
    fun updateTransaction(transaction: FinancialTransaction) {
        _isLoading.value = true
        financialRepository.updateTransaction(transaction) { success ->
            if (success) {
                _message.value = "Transacción actualizada exitosamente"
                loadTransactions()
                loadDashboardData(currentPeriod)
            } else {
                _message.value = "Error al actualizar la transacción"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Elimina una transacción.
     */
    fun deleteTransaction(transactionId: String) {
        _isLoading.value = true
        financialRepository.deleteTransaction(transactionId) { success ->
            if (success) {
                _message.value = "Transacción eliminada exitosamente"
                loadTransactions()
                loadDashboardData(currentPeriod)
            } else {
                _message.value = "Error al eliminar la transacción"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Obtiene las categorías de transacciones según el tipo.
     */
    fun getCategoriesForType(type: TransactionType): List<TransactionCategory> {
        return when (type) {
            TransactionType.INCOME -> listOf(
                TransactionCategory.SALES,
                TransactionCategory.DELIVERY,
                TransactionCategory.CATERING,
                TransactionCategory.EVENTS,
                TransactionCategory.OTHER_INCOME
            )
            TransactionType.EXPENSE -> listOf(
                TransactionCategory.INVENTORY,
                TransactionCategory.SUPPLIES,
                TransactionCategory.UTILITIES,
                TransactionCategory.RENT,
                TransactionCategory.SALARY,
                TransactionCategory.MAINTENANCE,
                TransactionCategory.MARKETING,
                TransactionCategory.INSURANCE,
                TransactionCategory.TAXES,
                TransactionCategory.OTHER_EXPENSE
            )
        }
    }
    
    /**
     * Obtiene el período actual del dashboard.
     */
    fun getCurrentPeriod(): String = currentPeriod
    
    /**
     * Obtiene el rango de fechas actual.
     */
    fun getCurrentDateRange(): DateRange = currentDateRange
} 