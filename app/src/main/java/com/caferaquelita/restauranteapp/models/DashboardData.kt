package com.caferaquelita.restauranteapp.models

import java.util.Date

/**
 * Modelo para datos del dashboard y reportes financieros.
 */
data class DashboardData(
    val period: String = "", // "today", "week", "month", "year"
    val dateRange: DateRange = DateRange(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val salesMetrics: SalesMetrics = SalesMetrics(),
    val expenseMetrics: ExpenseMetrics = ExpenseMetrics(),
    val profitMetrics: ProfitMetrics = ProfitMetrics(),
    val topProducts: List<TopProduct> = emptyList(),
    val topWaiters: List<TopWaiter> = emptyList(),
    val tablePerformance: List<TablePerformance> = emptyList(),
    val dailySales: List<DailySale> = emptyList()
)

data class DateRange(
    val startDate: Date = Date(),
    val endDate: Date = Date()
)

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitMargin: Double = 0.0,
    val averageTicket: Double = 0.0,
    val totalTransactions: Int = 0
)

data class SalesMetrics(
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val averageOrderValue: Double = 0.0,
    val salesByPaymentMethod: Map<PaymentMethod, Double> = emptyMap(),
    val salesByCategory: Map<TransactionCategory, Double> = emptyMap()
)

data class ExpenseMetrics(
    val totalExpenses: Double = 0.0,
    val expensesByCategory: Map<TransactionCategory, Double> = emptyMap(),
    val largestExpense: FinancialTransaction? = null,
    val expenseTrend: Double = 0.0 // Porcentaje de cambio vs período anterior
)

data class ProfitMetrics(
    val grossProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitMargin: Double = 0.0,
    val profitTrend: Double = 0.0 // Porcentaje de cambio vs período anterior
)

data class TopProduct(
    val productId: String = "",
    val productName: String = "",
    val quantitySold: Int = 0,
    val totalRevenue: Double = 0.0,
    val percentageOfSales: Double = 0.0
)

data class TopWaiter(
    val waiterId: String = "",
    val waiterName: String = "",
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val averageTicket: Double = 0.0,
    val totalTips: Double = 0.0
)

data class TablePerformance(
    val tableNumber: Int = 0,
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val averageTicket: Double = 0.0,
    val utilizationRate: Double = 0.0 // Porcentaje de tiempo ocupada
)

data class DailySale(
    val date: Date = Date(),
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val averageTicket: Double = 0.0
) 