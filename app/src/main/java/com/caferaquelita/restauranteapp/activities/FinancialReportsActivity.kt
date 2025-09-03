package com.caferaquelita.restauranteapp.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.viewmodels.DashboardViewModel
import java.text.NumberFormat
import java.util.*

/**
 * Actividad para mostrar reportes financieros detallados.
 */
class FinancialReportsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    
    // Selector de período
    private lateinit var spinnerPeriod: Spinner
    
    // Métricas principales
    private lateinit var textViewTotalIncome: TextView
    private lateinit var textViewTotalExpenses: TextView
    private lateinit var textViewNetProfit: TextView
    private lateinit var textViewProfitMargin: TextView
    private lateinit var textViewAverageTicket: TextView
    private lateinit var textViewTotalTransactions: TextView
    
    // Métricas de ventas
    private lateinit var textViewTotalSales: TextView
    private lateinit var textViewTotalOrders: TextView
    private lateinit var textViewAverageOrderValue: TextView
    
    // Métricas de gastos
    private lateinit var textViewTotalExpensesReport: TextView
    private lateinit var textViewLargestExpense: TextView
    private lateinit var textViewExpenseTrend: TextView
    
    // Métricas de ganancias
    private lateinit var textViewGrossProfit: TextView
    private lateinit var textViewNetProfitReport: TextView
    private lateinit var textViewProfitTrend: TextView
    
    // Listas de mejores rendimientos
    private lateinit var textViewTopProducts: TextView
    private lateinit var textViewTopWaiters: TextView
    private lateinit var textViewTablePerformance: TextView
    
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_reports)

        setupViews()
        setupToolbar()
        setupPeriodSelector()
        observeViewModel()
        loadInitialData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        
        // Métricas principales
        textViewTotalIncome = findViewById(R.id.textViewTotalIncome)
        textViewTotalExpenses = findViewById(R.id.textViewTotalExpenses)
        textViewNetProfit = findViewById(R.id.textViewNetProfit)
        textViewProfitMargin = findViewById(R.id.textViewProfitMargin)
        textViewAverageTicket = findViewById(R.id.textViewAverageTicket)
        textViewTotalTransactions = findViewById(R.id.textViewTotalTransactions)
        
        // Métricas de ventas
        textViewTotalSales = findViewById(R.id.textViewTotalSales)
        textViewTotalOrders = findViewById(R.id.textViewTotalOrders)
        textViewAverageOrderValue = findViewById(R.id.textViewAverageOrderValue)
        
        // Métricas de gastos
        textViewTotalExpensesReport = findViewById(R.id.textViewTotalExpensesReport)
        textViewLargestExpense = findViewById(R.id.textViewLargestExpense)
        textViewExpenseTrend = findViewById(R.id.textViewExpenseTrend)
        
        // Métricas de ganancias
        textViewGrossProfit = findViewById(R.id.textViewGrossProfit)
        textViewNetProfitReport = findViewById(R.id.textViewNetProfitReport)
        textViewProfitTrend = findViewById(R.id.textViewProfitTrend)
        
        // Listas de mejores rendimientos
        textViewTopProducts = findViewById(R.id.textViewTopProducts)
        textViewTopWaiters = findViewById(R.id.textViewTopWaiters)
        textViewTablePerformance = findViewById(R.id.textViewTablePerformance)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Reportes Financieros"
        }
    }

    private fun setupPeriodSelector() {
        val periods = arrayOf("Hoy", "Esta Semana", "Este Mes", "Este Año")
        val periodValues = arrayOf("today", "week", "month", "year")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter
        
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPeriod = periodValues[position]
                viewModel.loadDashboardData(selectedPeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { dashboardData ->
            updateReportMetrics(dashboardData)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadInitialData() {
        viewModel.loadDashboardData("today")
    }

    private fun updateReportMetrics(dashboardData: DashboardData) {
        val summary = dashboardData.financialSummary
        val salesMetrics = dashboardData.salesMetrics
        val expenseMetrics = dashboardData.expenseMetrics
        val profitMetrics = dashboardData.profitMetrics
        
        // Métricas principales
        textViewTotalIncome.text = numberFormat.format(summary.totalIncome)
        textViewTotalExpenses.text = numberFormat.format(summary.totalExpenses)
        textViewNetProfit.text = numberFormat.format(summary.netProfit)
        textViewProfitMargin.text = "${String.format("%.1f", summary.profitMargin)}%"
        textViewAverageTicket.text = numberFormat.format(summary.averageTicket)
        textViewTotalTransactions.text = summary.totalTransactions.toString()
        
        // Métricas de ventas
        textViewTotalSales.text = numberFormat.format(salesMetrics.totalSales)
        textViewTotalOrders.text = salesMetrics.totalOrders.toString()
        textViewAverageOrderValue.text = numberFormat.format(salesMetrics.averageOrderValue)
        
        // Métricas de gastos
        textViewTotalExpensesReport.text = numberFormat.format(expenseMetrics.totalExpenses)
        textViewLargestExpense.text = expenseMetrics.largestExpense?.let { 
            "${it.description}: ${numberFormat.format(it.amount)}" 
        } ?: "Sin gastos registrados"
        textViewExpenseTrend.text = "${String.format("%.1f", expenseMetrics.expenseTrend)}%"
        
        // Métricas de ganancias
        textViewGrossProfit.text = numberFormat.format(profitMetrics.grossProfit)
        textViewNetProfitReport.text = numberFormat.format(profitMetrics.netProfit)
        textViewProfitTrend.text = "${String.format("%.1f", profitMetrics.profitTrend)}%"
        
        // Actualizar listas de mejores rendimientos
        updateTopPerformers(dashboardData)
        
        // Actualizar colores según el rendimiento
        updateColors(summary)
    }

    private fun updateTopPerformers(dashboardData: DashboardData) {
        // Productos más vendidos
        val topProductsText = if (dashboardData.topProducts.isNotEmpty()) {
            dashboardData.topProducts.take(5).joinToString("\n") { product ->
                "• ${product.productName}: ${product.quantitySold} unidades - ${numberFormat.format(product.totalRevenue)}"
            }
        } else {
            "No hay datos disponibles"
        }
        textViewTopProducts.text = topProductsText
        
        // Meseros con mejores ventas
        val topWaitersText = if (dashboardData.topWaiters.isNotEmpty()) {
            dashboardData.topWaiters.take(5).joinToString("\n") { waiter ->
                "• ${waiter.waiterName}: ${numberFormat.format(waiter.totalSales)} - ${waiter.totalOrders} órdenes"
            }
        } else {
            "No hay datos disponibles"
        }
        textViewTopWaiters.text = topWaitersText
        
        // Rendimiento de mesas
        val tablePerformanceText = if (dashboardData.tablePerformance.isNotEmpty()) {
            dashboardData.tablePerformance.take(5).joinToString("\n") { table ->
                "• Mesa ${table.tableNumber}: ${numberFormat.format(table.totalSales)} - ${table.totalOrders} órdenes"
            }
        } else {
            "No hay datos disponibles"
        }
        textViewTablePerformance.text = tablePerformanceText
    }

    private fun updateColors(summary: FinancialSummary) {
        // Color verde para ganancias positivas, rojo para negativas
        val profitColor = if (summary.netProfit >= 0) 
            getResources().getColor(android.R.color.holo_green_dark, null)
        else 
            getResources().getColor(android.R.color.holo_red_dark, null)
        
        textViewNetProfit.setTextColor(profitColor)
        textViewNetProfitReport.setTextColor(profitColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()   // ← reemplazo directo
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 