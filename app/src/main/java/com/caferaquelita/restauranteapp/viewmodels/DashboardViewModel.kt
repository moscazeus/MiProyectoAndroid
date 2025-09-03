package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.repositories.DashboardRepository
import com.caferaquelita.restauranteapp.repositories.FinancialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel para el dashboard y gestión financiera.
 * - Lee datos del Dashboard en vivo (facturas de hoy, mesas, caja).
 * - Mantiene compatibilidad con tus transacciones (FinancialRepository).
 */
class DashboardViewModel : ViewModel() {

    // NUEVO: repo de dashboard (stream + consultas)
    private val dashboardRepository = DashboardRepository()

    // EXISTENTE: repo de transacciones (lo mantenemos)
    private val financialRepository = FinancialRepository()

    // ---------- LiveData del Dashboard ----------
    private val _dashboardData = MutableLiveData<DashboardData>()
    val dashboardData: LiveData<DashboardData> = _dashboardData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData("")
    val message: LiveData<String> = _message

    // Caja (estado y montos)
    private val _cashOpen = MutableLiveData(false)
    val cashOpen: LiveData<Boolean> = _cashOpen

    private val _cashInitial = MutableLiveData(0.0)
    val cashInitial: LiveData<Double> = _cashInitial

    private val _cashCurrent = MutableLiveData(0.0)
    val cashCurrent: LiveData<Double> = _cashCurrent

    // ---------- LiveData de transacciones ----------
    private val _transactions = MutableLiveData<List<FinancialTransaction>>(emptyList())
    val transactions: LiveData<List<FinancialTransaction>> = _transactions

    // Período actual (por compatibilidad con tu UI)
    private var currentPeriod = "today"
    private var currentDateRange = DateRange()

    // Job del stream en vivo para poder cancelarlo
    private var liveJob: kotlinx.coroutines.Job? = null

    init {
        // Carga inicial + escucha en vivo
        refreshOnce()
        startListening()
        loadTransactions()
    }

    // === DASHBOARD ===

    /** Escucha EN VIVO facturas de hoy + mesas + caja. */
    fun startListening() {
        // Cancela si ya había un listener activo
        liveJob?.cancel()

        liveJob = viewModelScope.launch(Dispatchers.IO) {
            dashboardRepository.listenDashboard().collectLatest { triple ->
                val (data, isOpen, cashPair) = triple
                _dashboardData.postValue(data)
                _cashOpen.postValue(isOpen)
                _cashInitial.postValue(cashPair.first)
                _cashCurrent.postValue(cashPair.second)
            }
        }
    }

    /** Carga manual (pull-to-refresh). */
    fun refreshOnce() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val (data, isOpen, cashPair) = dashboardRepository.fetchOnce()
                _dashboardData.postValue(data)
                _cashOpen.postValue(isOpen)
                _cashInitial.postValue(cashPair.first)
                _cashCurrent.postValue(cashPair.second)
                _message.postValue("")
            } catch (e: Exception) {
                _message.postValue(e.message ?: "Error al cargar dashboard")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Compat: si tu UI pide “loadDashboardData(period)”, la redirigimos a refreshOnce()
     * y calculamos el rango solo para exponerlo si alguien lo lee.
     */
    fun loadDashboardData(period: String = "today") {
        currentPeriod = period

        val cal = Calendar.getInstance()
        val end = cal.time
        val start: Date = when (period) {
            "today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.time
            }
            "week" -> { cal.add(Calendar.DAY_OF_YEAR, -7); cal.time }
            "month" -> { cal.add(Calendar.MONTH, -1); cal.time }
            "year" -> { cal.add(Calendar.YEAR, -1); cal.time }
            else -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.time
            }
        }
        currentDateRange = DateRange(start, end)

        // Por ahora los KPIs los calculamos para HOY/mes dentro del repo.
        // Si luego quieres filtros por periodo, los agregamos al repo.
        refreshOnce()
    }

    /** Permite a la Activity parar el stream en onDestroy(). */
    fun stopListening() {
        liveJob?.cancel()
        liveJob = null
    }

    fun getCurrentPeriod(): String = currentPeriod
    fun getCurrentDateRange(): DateRange = currentDateRange

    // === TRANSACCIONES (se mantiene tu lógica actual) ===

    fun loadTransactions() {
        _isLoading.value = true
        financialRepository.getTransactions { list ->
            _transactions.value = list
            _isLoading.value = false
        }
    }

    fun addTransaction(transaction: FinancialTransaction) {
        _isLoading.value = true
        financialRepository.addTransaction(transaction) { success ->
            if (success) {
                _message.value = "Transacción agregada exitosamente"
                loadTransactions()
                refreshOnce()
            } else {
                _message.value = "Error al agregar la transacción"
                _isLoading.value = false
            }
        }
    }

    fun updateTransaction(transaction: FinancialTransaction) {
        _isLoading.value = true
        financialRepository.updateTransaction(transaction) { success ->
            if (success) {
                _message.value = "Transacción actualizada exitosamente"
                loadTransactions()
                refreshOnce()
            } else {
                _message.value = "Error al actualizar la transacción"
                _isLoading.value = false
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        _isLoading.value = true
        financialRepository.deleteTransaction(transactionId) { success ->
            if (success) {
                _message.value = "Transacción eliminada exitosamente"
                loadTransactions()
                refreshOnce()
            } else {
                _message.value = "Error al eliminar la transacción"
                _isLoading.value = false
            }
        }
    }

    /** Categorías por tipo (igual que antes). */
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
}
