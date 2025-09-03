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
 * Actividad para mostrar detalles de ingresos.
 */
class IncomeDetailsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    
    // Métricas de ingresos
    private lateinit var textViewTotalIncome: TextView
    private lateinit var textViewSalesIncome: TextView
    private lateinit var textViewDeliveryIncome: TextView
    private lateinit var textViewCateringIncome: TextView
    private lateinit var textViewEventsIncome: TextView
    private lateinit var textViewOtherIncome: TextView
    
    // Métricas por método de pago
    private lateinit var textViewCashIncome: TextView
    private lateinit var textViewCardIncome: TextView
    private lateinit var textViewTransferIncome: TextView
    private lateinit var textViewDigitalIncome: TextView
    
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_details)

        setupViews()
        setupToolbar()
        observeViewModel()
        loadIncomeData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        
        // Métricas de ingresos
        textViewTotalIncome = findViewById(R.id.textViewTotalIncome)
        textViewSalesIncome = findViewById(R.id.textViewSalesIncome)
        textViewDeliveryIncome = findViewById(R.id.textViewDeliveryIncome)
        textViewCateringIncome = findViewById(R.id.textViewCateringIncome)
        textViewEventsIncome = findViewById(R.id.textViewEventsIncome)
        textViewOtherIncome = findViewById(R.id.textViewOtherIncome)
        
        // Métricas por método de pago
        textViewCashIncome = findViewById(R.id.textViewCashIncome)
        textViewCardIncome = findViewById(R.id.textViewCardIncome)
        textViewTransferIncome = findViewById(R.id.textViewTransferIncome)
        textViewDigitalIncome = findViewById(R.id.textViewDigitalIncome)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Detalles de Ingresos"
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { dashboardData ->
            updateIncomeDetails(dashboardData)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadIncomeData() {
        viewModel.loadDashboardData("today")
    }

    private fun updateIncomeDetails(dashboardData: DashboardData) {
        val salesMetrics = dashboardData.salesMetrics
        
        // Total de ingresos
        textViewTotalIncome.text = numberFormat.format(salesMetrics.totalSales)
        
        // Ingresos por categoría
        val salesByCategory = salesMetrics.salesByCategory
        textViewSalesIncome.text = numberFormat.format(salesByCategory[TransactionCategory.SALES] ?: 0.0)
        textViewDeliveryIncome.text = numberFormat.format(salesByCategory[TransactionCategory.DELIVERY] ?: 0.0)
        textViewCateringIncome.text = numberFormat.format(salesByCategory[TransactionCategory.CATERING] ?: 0.0)
        textViewEventsIncome.text = numberFormat.format(salesByCategory[TransactionCategory.EVENTS] ?: 0.0)
        textViewOtherIncome.text = numberFormat.format(salesByCategory[TransactionCategory.OTHER_INCOME] ?: 0.0)
        
        // Ingresos por método de pago
        val salesByPaymentMethod = salesMetrics.salesByPaymentMethod
        textViewCashIncome.text = numberFormat.format(salesByPaymentMethod[PaymentMethod.CASH] ?: 0.0)
        textViewCardIncome.text = numberFormat.format(salesByPaymentMethod[PaymentMethod.CARD] ?: 0.0)
        textViewTransferIncome.text = numberFormat.format(salesByPaymentMethod[PaymentMethod.TRANSFER] ?: 0.0)
        textViewDigitalIncome.text = numberFormat.format(salesByPaymentMethod[PaymentMethod.DIGITAL] ?: 0.0)
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