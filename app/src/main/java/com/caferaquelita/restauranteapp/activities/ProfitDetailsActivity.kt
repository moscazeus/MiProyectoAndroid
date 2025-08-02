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
 * Actividad para mostrar detalles de ganancias.
 */
class ProfitDetailsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    
    // Métricas de ganancias
    private lateinit var textViewGrossProfit: TextView
    private lateinit var textViewNetProfit: TextView
    private lateinit var textViewProfitMargin: TextView
    private lateinit var textViewProfitTrend: TextView
    
    // Métricas de ingresos y gastos
    private lateinit var textViewTotalIncome: TextView
    private lateinit var textViewTotalExpenses: TextView
    private lateinit var textViewAverageTicket: TextView
    private lateinit var textViewTotalTransactions: TextView
    
    // Análisis de rentabilidad
    private lateinit var textViewProfitabilityAnalysis: TextView
    private lateinit var textViewRecommendations: TextView
    
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profit_details)

        setupViews()
        setupToolbar()
        observeViewModel()
        loadProfitData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        
        // Métricas de ganancias
        textViewGrossProfit = findViewById(R.id.textViewGrossProfit)
        textViewNetProfit = findViewById(R.id.textViewNetProfit)
        textViewProfitMargin = findViewById(R.id.textViewProfitMargin)
        textViewProfitTrend = findViewById(R.id.textViewProfitTrend)
        
        // Métricas de ingresos y gastos
        textViewTotalIncome = findViewById(R.id.textViewTotalIncome)
        textViewTotalExpenses = findViewById(R.id.textViewTotalExpenses)
        textViewAverageTicket = findViewById(R.id.textViewAverageTicket)
        textViewTotalTransactions = findViewById(R.id.textViewTotalTransactions)
        
        // Análisis de rentabilidad
        textViewProfitabilityAnalysis = findViewById(R.id.textViewProfitabilityAnalysis)
        textViewRecommendations = findViewById(R.id.textViewRecommendations)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Detalles de Ganancias"
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { dashboardData ->
            updateProfitDetails(dashboardData)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadProfitData() {
        viewModel.loadDashboardData("today")
    }

    private fun updateProfitDetails(dashboardData: DashboardData) {
        val financialSummary = dashboardData.financialSummary
        val profitMetrics = dashboardData.profitMetrics
        
        // Métricas de ganancias
        textViewGrossProfit.text = numberFormat.format(profitMetrics.grossProfit)
        textViewNetProfit.text = numberFormat.format(profitMetrics.netProfit)
        textViewProfitMargin.text = "${String.format("%.1f", profitMetrics.profitMargin)}%"
        textViewProfitTrend.text = "${String.format("%.1f", profitMetrics.profitTrend)}%"
        
        // Métricas de ingresos y gastos
        textViewTotalIncome.text = numberFormat.format(financialSummary.totalIncome)
        textViewTotalExpenses.text = numberFormat.format(financialSummary.totalExpenses)
        textViewAverageTicket.text = numberFormat.format(financialSummary.averageTicket)
        textViewTotalTransactions.text = financialSummary.totalTransactions.toString()
        
        // Análisis de rentabilidad
        updateProfitabilityAnalysis(financialSummary, profitMetrics)
        
        // Actualizar colores según el rendimiento
        updateProfitColors(profitMetrics)
    }

    private fun updateProfitabilityAnalysis(financialSummary: FinancialSummary, profitMetrics: ProfitMetrics) {
        val analysis = StringBuilder()
        val recommendations = StringBuilder()
        
        // Análisis de rentabilidad
        analysis.append("Análisis de Rentabilidad:\n\n")
        
        if (profitMetrics.profitMargin > 20) {
            analysis.append("✅ Excelente rentabilidad (${String.format("%.1f", profitMetrics.profitMargin)}%)\n")
            analysis.append("Tu negocio está generando ganancias muy saludables.\n\n")
        } else if (profitMetrics.profitMargin > 10) {
            analysis.append("✅ Buena rentabilidad (${String.format("%.1f", profitMetrics.profitMargin)}%)\n")
            analysis.append("Tu negocio está funcionando bien.\n\n")
        } else if (profitMetrics.profitMargin > 0) {
            analysis.append("⚠️ Rentabilidad baja (${String.format("%.1f", profitMetrics.profitMargin)}%)\n")
            analysis.append("Hay espacio para mejorar la rentabilidad.\n\n")
        } else {
            analysis.append("❌ Pérdidas (${String.format("%.1f", profitMetrics.profitMargin)}%)\n")
            analysis.append("Es necesario revisar la estrategia de negocio.\n\n")
        }
        
        // Análisis de estructura de costos
        val expenseRatio = if (financialSummary.totalIncome > 0) {
            (financialSummary.totalExpenses / financialSummary.totalIncome) * 100
        } else 0.0
        
        analysis.append("Estructura de Costos:\n")
        analysis.append("• Ingresos: ${numberFormat.format(financialSummary.totalIncome)}\n")
        analysis.append("• Gastos: ${numberFormat.format(financialSummary.totalExpenses)}\n")
        analysis.append("• Ratio Gastos/Ingresos: ${String.format("%.1f", expenseRatio)}%\n\n")
        
        // Recomendaciones
        recommendations.append("Recomendaciones:\n\n")
        
        if (profitMetrics.profitMargin < 10) {
            recommendations.append("• Revisa y optimiza los costos operativos\n")
            recommendations.append("• Considera aumentar los precios de manera estratégica\n")
            recommendations.append("• Analiza qué productos/servicios son más rentables\n")
        }
        
        if (financialSummary.averageTicket < 50000) { // Ejemplo: menos de $50,000
            recommendations.append("• Considera estrategias para aumentar el ticket promedio\n")
            recommendations.append("• Ofrece combos o promociones para incrementar ventas\n")
        }
        
        if (expenseRatio > 80) {
            recommendations.append("• Los gastos son muy altos en relación a los ingresos\n")
            recommendations.append("• Busca formas de reducir costos sin afectar la calidad\n")
        }
        
        if (recommendations.toString() == "Recomendaciones:\n\n") {
            recommendations.append("• Mantén el excelente trabajo que estás haciendo\n")
            recommendations.append("• Continúa monitoreando las métricas regularmente\n")
        }
        
        textViewProfitabilityAnalysis.text = analysis.toString()
        textViewRecommendations.text = recommendations.toString()
    }

    private fun updateProfitColors(profitMetrics: ProfitMetrics) {
        // Color verde para ganancias positivas, rojo para negativas
        val profitColor = if (profitMetrics.netProfit >= 0) 
            getResources().getColor(android.R.color.holo_green_dark, null)
        else 
            getResources().getColor(android.R.color.holo_red_dark, null)
        
        textViewNetProfit.setTextColor(profitColor)
        textViewProfitMargin.setTextColor(profitColor)
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