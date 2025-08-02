package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

/**
 * Repositorio para gestión financiera y dashboard.
 */
class FinancialRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val transactionsCollection = db.collection("financial_transactions")
    private val invoicesCollection = db.collection("invoices")
    private val tablesCollection = db.collection("tables")
    private val productsCollection = db.collection("products")
    
    /**
     * Obtiene los datos del dashboard para un rango de fechas.
     */
    fun getDashboardData(startDate: Date, endDate: Date, callback: (DashboardData) -> Unit) {
        val dashboardData = DashboardData()
        
        // Obtener transacciones del período
        transactionsCollection
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .addOnSuccessListener { documents ->
                val transactions = documents.mapNotNull { doc ->
                    doc.toObject(FinancialTransaction::class.java)?.copy(id = doc.id)
                }
                
                // Calcular métricas financieras
                val financialSummary = calculateFinancialSummary(transactions)
                val salesMetrics = calculateSalesMetrics(transactions)
                val expenseMetrics = calculateExpenseMetrics(transactions)
                val profitMetrics = calculateProfitMetrics(financialSummary)
                
                // Obtener datos adicionales
                getTopProducts(startDate, endDate) { topProducts ->
                    getTopWaiters(startDate, endDate) { topWaiters ->
                        getTablePerformance(startDate, endDate) { tablePerformance ->
                            getDailySales(startDate, endDate) { dailySales ->
                                val completeDashboardData = dashboardData.copy(
                                    period = getPeriodFromDates(startDate, endDate),
                                    dateRange = DateRange(startDate, endDate),
                                    financialSummary = financialSummary,
                                    salesMetrics = salesMetrics,
                                    expenseMetrics = expenseMetrics,
                                    profitMetrics = profitMetrics,
                                    topProducts = topProducts,
                                    topWaiters = topWaiters,
                                    tablePerformance = tablePerformance,
                                    dailySales = dailySales
                                )
                                callback(completeDashboardData)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // En caso de error, devolver datos vacíos
                callback(dashboardData)
            }
    }
    
    /**
     * Obtiene todas las transacciones financieras.
     */
    fun getTransactions(callback: (List<FinancialTransaction>) -> Unit) {
        transactionsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val transactions = documents.mapNotNull { doc ->
                    doc.toObject(FinancialTransaction::class.java)?.copy(id = doc.id)
                }
                callback(transactions)
            }
            .addOnFailureListener { exception ->
                callback(emptyList())
            }
    }
    
    /**
     * Agrega una nueva transacción financiera.
     */
    fun addTransaction(transaction: FinancialTransaction, callback: (Boolean) -> Unit) {
        val transactionData = transaction.copy(
            id = UUID.randomUUID().toString(),
            date = Date()
        )
        
        transactionsCollection
            .document(transactionData.id)
            .set(transactionData)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                callback(false)
            }
    }
    
    /**
     * Actualiza una transacción existente.
     */
    fun updateTransaction(transaction: FinancialTransaction, callback: (Boolean) -> Unit) {
        transactionsCollection
            .document(transaction.id)
            .set(transaction)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                callback(false)
            }
    }
    
    /**
     * Elimina una transacción.
     */
    fun deleteTransaction(transactionId: String, callback: (Boolean) -> Unit) {
        transactionsCollection
            .document(transactionId)
            .delete()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                callback(false)
            }
    }
    
    /**
     * Calcula el resumen financiero.
     */
    private fun calculateFinancialSummary(transactions: List<FinancialTransaction>): FinancialSummary {
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val netProfit = totalIncome - totalExpenses
        val profitMargin = if (totalIncome > 0) (netProfit / totalIncome) * 100 else 0.0
        val averageTicket = if (incomeTransactions.isNotEmpty()) totalIncome / incomeTransactions.size else 0.0
        
        return FinancialSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMargin = profitMargin,
            averageTicket = averageTicket,
            totalTransactions = transactions.size
        )
    }
    
    /**
     * Calcula las métricas de ventas.
     */
    private fun calculateSalesMetrics(transactions: List<FinancialTransaction>): SalesMetrics {
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        
        val totalSales = incomeTransactions.sumOf { it.amount }
        val totalOrders = incomeTransactions.size
        val averageOrderValue = if (totalOrders > 0) totalSales / totalOrders else 0.0
        
        // Agrupar por método de pago
        val salesByPaymentMethod = incomeTransactions
            .groupBy { it.paymentMethod }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
        
        // Agrupar por categoría
        val salesByCategory = incomeTransactions
            .groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
        
        return SalesMetrics(
            totalSales = totalSales,
            totalOrders = totalOrders,
            averageOrderValue = averageOrderValue,
            salesByPaymentMethod = salesByPaymentMethod,
            salesByCategory = salesByCategory
        )
    }
    
    /**
     * Calcula las métricas de gastos.
     */
    private fun calculateExpenseMetrics(transactions: List<FinancialTransaction>): ExpenseMetrics {
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val largestExpense = expenseTransactions.maxByOrNull { it.amount }
        
        // Agrupar por categoría
        val expensesByCategory = expenseTransactions
            .groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }
        
        return ExpenseMetrics(
            totalExpenses = totalExpenses,
            expensesByCategory = expensesByCategory,
            largestExpense = largestExpense,
            expenseTrend = 0.0 // Por ahora 0, se puede calcular comparando con períodos anteriores
        )
    }
    
    /**
     * Calcula las métricas de ganancias.
     */
    private fun calculateProfitMetrics(financialSummary: FinancialSummary): ProfitMetrics {
        return ProfitMetrics(
            grossProfit = financialSummary.totalIncome,
            netProfit = financialSummary.netProfit,
            profitMargin = financialSummary.profitMargin,
            profitTrend = 0.0 // Por ahora 0, se puede calcular comparando con períodos anteriores
        )
    }
    
    /**
     * Obtiene los productos más vendidos.
     */
    private fun getTopProducts(startDate: Date, endDate: Date, callback: (List<TopProduct>) -> Unit) {
        // Por ahora devolver lista vacía, se implementará con datos reales
        callback(emptyList())
    }
    
    /**
     * Obtiene los meseros con mejores ventas.
     */
    private fun getTopWaiters(startDate: Date, endDate: Date, callback: (List<TopWaiter>) -> Unit) {
        // Por ahora devolver lista vacía, se implementará con datos reales
        callback(emptyList())
    }
    
    /**
     * Obtiene el rendimiento de las mesas.
     */
    private fun getTablePerformance(startDate: Date, endDate: Date, callback: (List<TablePerformance>) -> Unit) {
        // Por ahora devolver lista vacía, se implementará con datos reales
        callback(emptyList())
    }
    
    /**
     * Obtiene las ventas diarias.
     */
    private fun getDailySales(startDate: Date, endDate: Date, callback: (List<DailySale>) -> Unit) {
        // Por ahora devolver lista vacía, se implementará con datos reales
        callback(emptyList())
    }
    
    /**
     * Determina el período basado en las fechas.
     */
    private fun getPeriodFromDates(startDate: Date, endDate: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        return when {
            isSameDay(startDate, endDate) -> "today"
            getDaysDifference(startDate, endDate) <= 7 -> "week"
            getDaysDifference(startDate, endDate) <= 30 -> "month"
            else -> "year"
        }
    }
    
    /**
     * Verifica si dos fechas son el mismo día.
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Calcula la diferencia en días entre dos fechas.
     */
    private fun getDaysDifference(date1: Date, date2: Date): Int {
        val diffInMillis = date2.time - date1.time
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
} 