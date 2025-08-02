package com.caferaquelita.restauranteapp.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.viewmodels.DashboardViewModel
import java.text.NumberFormat
import java.util.*

/**
 * Actividad para mostrar detalles de gastos.
 */
class ExpenseDetailsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    
    // Métricas de gastos
    private lateinit var textViewTotalExpenses: TextView
    private lateinit var textViewInventoryExpenses: TextView
    private lateinit var textViewSuppliesExpenses: TextView
    private lateinit var textViewUtilitiesExpenses: TextView
    private lateinit var textViewRentExpenses: TextView
    private lateinit var textViewSalaryExpenses: TextView
    private lateinit var textViewMaintenanceExpenses: TextView
    private lateinit var textViewMarketingExpenses: TextView
    private lateinit var textViewInsuranceExpenses: TextView
    private lateinit var textViewTaxesExpenses: TextView
    private lateinit var textViewOtherExpenses: TextView
    
    // Métricas adicionales
    private lateinit var textViewLargestExpense: TextView
    private lateinit var textViewExpenseTrend: TextView
    private lateinit var textViewAverageExpense: TextView
    
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_details)

        setupViews()
        setupToolbar()
        observeViewModel()
        loadExpenseData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        
        // Métricas de gastos
        textViewTotalExpenses = findViewById(R.id.textViewTotalExpenses)
        textViewInventoryExpenses = findViewById(R.id.textViewInventoryExpenses)
        textViewSuppliesExpenses = findViewById(R.id.textViewSuppliesExpenses)
        textViewUtilitiesExpenses = findViewById(R.id.textViewUtilitiesExpenses)
        textViewRentExpenses = findViewById(R.id.textViewRentExpenses)
        textViewSalaryExpenses = findViewById(R.id.textViewSalaryExpenses)
        textViewMaintenanceExpenses = findViewById(R.id.textViewMaintenanceExpenses)
        textViewMarketingExpenses = findViewById(R.id.textViewMarketingExpenses)
        textViewInsuranceExpenses = findViewById(R.id.textViewInsuranceExpenses)
        textViewTaxesExpenses = findViewById(R.id.textViewTaxesExpenses)
        textViewOtherExpenses = findViewById(R.id.textViewOtherExpenses)
        
        // Métricas adicionales
        textViewLargestExpense = findViewById(R.id.textViewLargestExpense)
        textViewExpenseTrend = findViewById(R.id.textViewExpenseTrend)
        textViewAverageExpense = findViewById(R.id.textViewAverageExpense)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Detalles de Gastos"
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { dashboardData ->
            updateExpenseDetails(dashboardData)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadExpenseData() {
        viewModel.loadDashboardData("today")
    }

    private fun updateExpenseDetails(dashboardData: DashboardData) {
        val expenseMetrics = dashboardData.expenseMetrics
        
        // Total de gastos
        textViewTotalExpenses.text = numberFormat.format(expenseMetrics.totalExpenses)
        
        // Gastos por categoría
        val expensesByCategory = expenseMetrics.expensesByCategory
        textViewInventoryExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.INVENTORY] ?: 0.0)
        textViewSuppliesExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.SUPPLIES] ?: 0.0)
        textViewUtilitiesExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.UTILITIES] ?: 0.0)
        textViewRentExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.RENT] ?: 0.0)
        textViewSalaryExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.SALARY] ?: 0.0)
        textViewMaintenanceExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.MAINTENANCE] ?: 0.0)
        textViewMarketingExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.MARKETING] ?: 0.0)
        textViewInsuranceExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.INSURANCE] ?: 0.0)
        textViewTaxesExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.TAXES] ?: 0.0)
        textViewOtherExpenses.text = numberFormat.format(expensesByCategory[TransactionCategory.OTHER_EXPENSE] ?: 0.0)
        
        // Métricas adicionales
        expenseMetrics.largestExpense?.let { largestExpense ->
            textViewLargestExpense.text = "${largestExpense.description}: ${numberFormat.format(largestExpense.amount)}"
        } ?: run {
            textViewLargestExpense.text = "No hay gastos registrados"
        }
        
        textViewExpenseTrend.text = "${String.format("%.1f", expenseMetrics.expenseTrend)}%"
        
        // Calcular gasto promedio
        val totalExpenses = expenseMetrics.totalExpenses
        val expenseCount = expensesByCategory.values.count { it > 0 }
        val averageExpense = if (expenseCount > 0) totalExpenses / expenseCount.toDouble() else 0.0
        textViewAverageExpense.text = numberFormat.format(averageExpense)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 