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
import androidx.core.content.ContextCompat
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.TransactionAdapter
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.viewmodels.DashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore




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
        // 🔒 Solo ADMIN puede ver el Dashboard
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "waiter"
                if (role != "admin") {
                    Toast.makeText(this, "Solo un administrador puede ver el dashboard", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                // ✅ Si es ADMIN, recién aquí armamos toda la UI
                initAdminUI()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo verificar el rol", Toast.LENGTH_LONG).show()
                finish()
            }



    }

    private fun initAdminUI() {
        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupPeriodSelector()
        setupUI()
        observeViewModel()

        // ⬇️ empieza a escuchar cambios en vivo (facturas de hoy, caja, etc.)
        viewModel.startListening()
        // ⬇️ y haz una carga manual por si el listener tarda
        viewModel.refreshOnce()
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
        // Estado de caja (mostrar en toolbar)
        viewModel.cashOpen.observe(this) { isOpen ->
            updateCashUi(
                isOpen,
                viewModel.cashInitial.value ?: 0.0,
                viewModel.cashCurrent.value ?: 0.0
            )
        }
        viewModel.cashInitial.observe(this) { initial ->
            updateCashUi(
                viewModel.cashOpen.value ?: false,
                initial,
                viewModel.cashCurrent.value ?: 0.0
            )
        }
        viewModel.cashCurrent.observe(this) { current ->
            updateCashUi(
                viewModel.cashOpen.value ?: false,
                viewModel.cashInitial.value ?: 0.0,
                current
            )
        }


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

    private fun updateCashUi(isOpen: Boolean, initial: Double, current: Double) {
        val status = if (isOpen) "Caja abierta" else "Caja cerrada"
        // Muestra el estado de caja en el subtítulo del toolbar
        toolbar.subtitle = "$status — Inicial: ${numberFormat.format(initial)} | Actual: ${numberFormat.format(current)}"

        // (Opcional) deshabilita el FAB de transacciones si la caja está cerrada
        fabAddTransaction.isEnabled = isOpen
    }


    private fun updateEmptyState(isEmpty: Boolean) {
        textViewEmptyTransactions.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }


    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPaymentMethod = dialogView.findViewById<Spinner>(R.id.spinnerPaymentMethod)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val editTextNotes = dialogView.findViewById<EditText>(R.id.editTextNotes)

        // --- 1) SPINNER MÉTODO DE PAGO: etiquetas ↔ enum ---
        // Lista real de enums
        val paymentEnums = PaymentMethod.values().toList()
        // Etiquetas amigables para mostrar
        val paymentLabels = paymentEnums.map { getPaymentMethodDisplayName(it.name) }
        // Cargamos el spinner con etiquetas (no con los enums)
        val paymentMethodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentLabels)
        paymentMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaymentMethod.adapter = paymentMethodAdapter

        // --- 2) Tipo y categoría seleccionados ---
        var selectedType = TransactionType.INCOME
        var selectedCategory = TransactionCategory.SALES

        // Función para poblar categorías según el tipo actual
        fun populateCategories() {
            val categories = viewModel.getCategoriesForType(selectedType)
            val names = categories.map { getCategoryDisplayName(it) }
            val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = catAdapter
            // Por defecto: la primera
            selectedCategory = categories.first()
            // Cuando el usuario cambie, actualizamos selectedCategory
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategory = categories[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Carga inicial de categorías (para INCOME)
        populateCategories()

        // Cuando cambie el tipo (Ingreso/Gasto) recargamos categorías
        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.radioButtonIncome -> TransactionType.INCOME
                R.id.radioButtonExpense -> TransactionType.EXPENSE
                else -> TransactionType.INCOME
            }
            populateCategories()
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

            positiveButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            negativeButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            negativeButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))


            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()
                val notes = editTextNotes.text.toString().trim()

                if (amount != null && amount > 0 && description.isNotEmpty()) {
                    // ⬇️ Tomamos el enum real según el índice seleccionado
                    val pmEnum = paymentEnums[spinnerPaymentMethod.selectedItemPosition]

                    val transaction = FinancialTransaction(
                        amount = amount,
                        type = selectedType,
                        category = selectedCategory,
                        description = description,
                        paymentMethod = pmEnum,           // ✅ enum correcto (CASH, CARD…)
                        notes = notes,
                        createdBy = "Usuario"
                    )
                    viewModel.addTransaction(transaction)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show()
                }
            }

            negativeButton.setOnClickListener { dialog.dismiss() }
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Transacción")
            .setMessage("¿Estás seguro de que quieres eliminar esta transacción?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton("No", null)
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
                finish()   // ← reemplazo directo
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopListening()
    }
} 