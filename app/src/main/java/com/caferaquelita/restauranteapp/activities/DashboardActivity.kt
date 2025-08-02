package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.TransactionAdapter
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.viewmodels.DashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad principal del dashboard con métricas financieras y gestión de transacciones.
 */
class DashboardActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddTransaction: FloatingActionButton
    
    // Cards de métricas principales
    private lateinit var cardViewIncome: CardView
    private lateinit var cardViewExpenses: CardView
    private lateinit var cardViewProfit: CardView
    private lateinit var cardViewTransactions: CardView
    
    // TextViews para métricas
    private lateinit var textViewIncome: TextView
    private lateinit var textViewExpenses: TextView
    private lateinit var textViewProfit: TextView
    private lateinit var textViewProfitMargin: TextView
    private lateinit var textViewAverageTicket: TextView
    private lateinit var textViewTotalTransactions: TextView
    
    // Selector de período
    private lateinit var spinnerPeriod: Spinner
    
    // RecyclerView para transacciones
    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var textViewEmptyTransactions: TextView
    
    // Botones de acción
    private lateinit var buttonViewReports: Button
    private lateinit var buttonViewCashRegister: Button
    
    private lateinit var transactionAdapter: TransactionAdapter
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupPeriodSelector()
        setupUI()
        observeViewModel()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)
        
        // Cards de métricas
        cardViewIncome = findViewById(R.id.cardViewIncome)
        cardViewExpenses = findViewById(R.id.cardViewExpenses)
        cardViewProfit = findViewById(R.id.cardViewProfit)
        cardViewTransactions = findViewById(R.id.cardViewTransactions)
        
        // TextViews de métricas
        textViewIncome = findViewById(R.id.textViewIncome)
        textViewExpenses = findViewById(R.id.textViewExpenses)
        textViewProfit = findViewById(R.id.textViewProfit)
        textViewProfitMargin = findViewById(R.id.textViewProfitMargin)
        textViewAverageTicket = findViewById(R.id.textViewAverageTicket)
        textViewTotalTransactions = findViewById(R.id.textViewTotalTransactions)
        
        // Selector de período
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        
        // RecyclerView
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions)
        textViewEmptyTransactions = findViewById(R.id.textViewEmptyTransactions)
        
        // Botones
        buttonViewReports = findViewById(R.id.buttonViewReports)
        buttonViewCashRegister = findViewById(R.id.buttonViewCashRegister)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Dashboard Financiero"
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            transactions = emptyList(),
            onTransactionClick = { transaction -> showTransactionDetails(transaction) },
            onTransactionEdit = { transaction -> showEditTransactionDialog(transaction) },
            onTransactionDelete = { transaction -> showDeleteTransactionDialog(transaction) }
        )
        
        recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = transactionAdapter
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

    private fun setupUI() {
        fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
        
        buttonViewReports.setOnClickListener {
            val intent = Intent(this, FinancialReportsActivity::class.java)
            startActivity(intent)
        }
        
        buttonViewCashRegister.setOnClickListener {
            val intent = Intent(this, CashRegisterActivity::class.java)
            startActivity(intent)
        }
        
        // Configurar clicks en las cards
        cardViewIncome.setOnClickListener {
            showIncomeDetails()
        }
        
        cardViewExpenses.setOnClickListener {
            showExpenseDetails()
        }
        
        cardViewProfit.setOnClickListener {
            showProfitDetails()
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { dashboardData ->
            updateDashboardMetrics(dashboardData)
        }

        viewModel.transactions.observe(this) { transactions ->
            transactionAdapter.updateTransactions(transactions)
            updateEmptyState(transactions.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDashboardMetrics(dashboardData: DashboardData) {
        val summary = dashboardData.financialSummary
        
        textViewIncome.text = numberFormat.format(summary.totalIncome)
        textViewExpenses.text = numberFormat.format(summary.totalExpenses)
        textViewProfit.text = numberFormat.format(summary.netProfit)
        textViewProfitMargin.text = "Margen: ${String.format("%.1f", summary.profitMargin)}%"
        textViewAverageTicket.text = numberFormat.format(summary.averageTicket)
        textViewTotalTransactions.text = "${summary.totalTransactions} transacciones"
        
        // Actualizar colores según el rendimiento
        updateCardColors(summary)
    }

    private fun updateCardColors(summary: FinancialSummary) {
        // Color verde para ganancias positivas, rojo para negativas
        val profitColor = if (summary.netProfit >= 0) 
            getResources().getColor(android.R.color.holo_green_dark, null)
        else 
            getResources().getColor(android.R.color.holo_red_dark, null)
        
        textViewProfit.setTextColor(profitColor)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        textViewEmptyTransactions.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)
        val radioButtonIncome = dialogView.findViewById<RadioButton>(R.id.radioButtonIncome)
        val radioButtonExpense = dialogView.findViewById<RadioButton>(R.id.radioButtonExpense)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPaymentMethod = dialogView.findViewById<Spinner>(R.id.spinnerPaymentMethod)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val editTextNotes = dialogView.findViewById<EditText>(R.id.editTextNotes)

        // Configurar spinner de método de pago
        val paymentMethods = PaymentMethod.values().map { getPaymentMethodDisplayName(it.name) }
        val paymentMethodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        paymentMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaymentMethod.adapter = paymentMethodAdapter

        var selectedType = TransactionType.INCOME
        var selectedCategory = TransactionCategory.SALES

        // Configurar listener para el tipo
        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.radioButtonIncome -> TransactionType.INCOME
                R.id.radioButtonExpense -> TransactionType.EXPENSE
                else -> TransactionType.INCOME
            }
            updateCategories(spinnerCategory, selectedType)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Transacción")
            .setView(dialogView)
            .setPositiveButton("Agregar", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))

            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))

            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()
                val notes = editTextNotes.text.toString().trim()

                if (amount != null && amount > 0 && description.isNotEmpty()) {
                    val transaction = FinancialTransaction(
                        amount = amount,
                        type = selectedType,
                        category = selectedCategory,
                        description = description,
                        paymentMethod = PaymentMethod.valueOf(spinnerPaymentMethod.selectedItem.toString()),
                        notes = notes,
                        createdBy = "Usuario" // Por ahora hardcodeado
                    )
                    viewModel.addTransaction(transaction)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show()
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateCategories(spinner: Spinner, type: TransactionType) {
        val categories = viewModel.getCategoriesForType(type)
        val categoryNames = categories.map { getCategoryDisplayName(it) }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun getCategoryDisplayName(category: TransactionCategory): String {
        return when (category) {
            TransactionCategory.SALES -> "Ventas"
            TransactionCategory.DELIVERY -> "Delivery"
            TransactionCategory.CATERING -> "Catering"
            TransactionCategory.EVENTS -> "Eventos"
            TransactionCategory.OTHER_INCOME -> "Otros Ingresos"
            TransactionCategory.INVENTORY -> "Inventario"
            TransactionCategory.SUPPLIES -> "Suministros"
            TransactionCategory.UTILITIES -> "Servicios Públicos"
            TransactionCategory.RENT -> "Alquiler"
            TransactionCategory.SALARY -> "Salarios"
            TransactionCategory.MAINTENANCE -> "Mantenimiento"
            TransactionCategory.MARKETING -> "Marketing"
            TransactionCategory.INSURANCE -> "Seguros"
            TransactionCategory.TAXES -> "Impuestos"
            TransactionCategory.OTHER_EXPENSE -> "Otros Gastos"
        }
    }

    private fun getPaymentMethodDisplayName(paymentMethod: String): String {
        return when (paymentMethod) {
            "CASH" -> "Efectivo"
            "CARD" -> "Tarjeta"
            "TRANSFER" -> "Transferencia"
            "CHECK" -> "Cheque"
            "DIGITAL" -> "Digital"
            else -> paymentMethod
        }
    }

    private fun showTransactionDetails(transaction: FinancialTransaction) {
        val message = """
            Descripción: ${transaction.description}
            Monto: ${numberFormat.format(transaction.amount)}
            Tipo: ${if (transaction.type == TransactionType.INCOME) "Ingreso" else "Gasto"}
            Categoría: ${getCategoryDisplayName(transaction.category)}
            Método de Pago: ${transaction.paymentMethod.name}
            Fecha: ${dateFormat.format(transaction.date)}
            Notas: ${transaction.notes.ifEmpty { "Sin notas" }}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Detalles de Transacción")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showEditTransactionDialog(transaction: FinancialTransaction) {
        // Similar a showAddTransactionDialog pero con datos precargados
        Toast.makeText(this, "Editar transacción próximamente", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteTransactionDialog(transaction: FinancialTransaction) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Transacción")
            .setMessage("¿Estás seguro de que quieres eliminar esta transacción?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showIncomeDetails() {
        val intent = Intent(this, IncomeDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun showExpenseDetails() {
        val intent = Intent(this, ExpenseDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun showProfitDetails() {
        val intent = Intent(this, ProfitDetailsActivity::class.java)
        startActivity(intent)
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