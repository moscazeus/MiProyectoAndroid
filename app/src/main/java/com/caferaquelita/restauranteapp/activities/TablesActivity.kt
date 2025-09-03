package com.caferaquelita.restauranteapp.activities


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.TableAdapter
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableStatus
import com.caferaquelita.restauranteapp.viewmodels.TablesViewModel
import com.caferaquelita.restauranteapp.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale








/**
 * Actividad para la gestión de mesas.
 * Mejorada para ser más profesional e intuitiva.
 */
class TablesActivity : AppCompatActivity() {
    private lateinit var userId: String
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewTables: RecyclerView
    private lateinit var linearLayoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddTable: FloatingActionButton
    private lateinit var textViewFreeTables: TextView
    private lateinit var textViewOccupiedTables: TextView
    private lateinit var tableAdapter: TableAdapter
    private val viewModel: TablesViewModel by viewModels()
    private val invoiceRepo by lazy { com.caferaquelita.restauranteapp.repositories.InvoiceRepository(this) }
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val db = FirebaseFirestore.getInstance()

        if (userId.isNotEmpty()) {
            // Leer SIEMPRE del servidor (evita valores viejos del caché)
            db.collection("cash").document("status")
                .get(Source.SERVER)
                .addOnSuccessListener { doc ->
                    val isOpen = doc.getBoolean("isOpen") == true
                    if (isOpen) {
                        iniciarActividadDeMesas()   // ✅ Caja abierta → cargamos la UI de Mesas
                    } else {
                        Toast.makeText(
                            this,
                            "No se encontró caja abierta. Contacta al administrador.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error al verificar la caja: ${e.message ?: "intenta de nuevo"}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTables()
    }



    private fun setupViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            recyclerViewTables = findViewById(R.id.recyclerViewTables)
            linearLayoutEmpty = findViewById(R.id.linearLayoutEmpty)
            progressBar = findViewById(R.id.progressBar)
            fabAddTable = findViewById(R.id.fabAddTable)
            
            // Referencias a los TextViews del resumen
            textViewFreeTables = findViewById(R.id.textViewFreeTables)
            textViewOccupiedTables = findViewById(R.id.textViewOccupiedTables)
        } catch (e: Exception) {
            // Si hay error, usar valores por defecto
            android.util.Log.e("TablesActivity", "Error en setupViews: ${e.message}")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Gestión de Mesas"
        }
    }

    private fun setupRecyclerView() {
        tableAdapter = TableAdapter(
            tables = emptyList(),
            onOpenTable = { table -> showOpenTableDialog(table) },
            onAddProduct = { table -> 
                // Navegar a la nueva actividad de gestión de productos
                val intent = Intent(this, TableProductsActivity::class.java)
                intent.putExtra("table_id", table.id)
                startActivity(intent)
            },
            onCloseTable = { table -> closeTable(table) }   // ← ahora usamos el nuevo método

        )
        
        recyclerViewTables.apply {
            layoutManager = LinearLayoutManager(this@TablesActivity)
            adapter = tableAdapter
        }
    }

    private fun setupUI() {
        fabAddTable.setOnClickListener {
            showAddTableDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.tables.observe(this) { tables ->
            tableAdapter.updateTables(tables)
            updateEmptyState(tables.isEmpty())
            updateSummary(tables)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadData() {
        viewModel.loadTables()
        viewModel.loadProducts()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        linearLayoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewTables.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateSummary(tables: List<Table>) {
        val freeTables = tables.count { it.status == TableStatus.FREE }
        val occupiedTables = tables.count { it.status == TableStatus.OCCUPIED }

        textViewFreeTables.text = freeTables.toString()
        textViewOccupiedTables.text = occupiedTables.toString()

    }

    private fun showOpenTableDialog(table: Table) {
        // Validar que la mesa no esté ya ocupada
        if (table.status == TableStatus.OCCUPIED) {
            Toast.makeText(this, "La mesa ${table.number} ya está ocupada", Toast.LENGTH_LONG).show()
            return
        }
        
        // Crear un EditText simple programáticamente
        val editText = EditText(this).apply {
            hint = "Nombre del mesero"
            setPadding(50, 50, 50, 50)
            setText("")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Abrir Mesa ${table.number}")
            .setMessage("Ingresa el nombre del mesero que atenderá esta mesa:")
            .setView(editText)
            .setPositiveButton("Abrir Mesa", null) // Configurar después
            .setNegativeButton("Cancelar", null) // Configurar después
            .setCancelable(false)
            .create()
        
        // Configurar los botones después de crear el diálogo para que sean visibles
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            // Configurar colores y estilo del botón positivo
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Abrir Mesa"
            
            // Configurar colores y estilo del botón negativo
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"
            
            // Configurar listeners
            positiveButton.setOnClickListener {
                val waiterName = editText.text.toString().trim()
                
                if (waiterName.isNotBlank()) {
                    // Usar el nombre del mesero como ID del mesero
                    val waiterId = waiterName.lowercase().replace(" ", "_")
                    
                    // Mostrar un Toast de confirmación
                    Toast.makeText(this, "Abriendo mesa ${table.number} con mesero: $waiterName", Toast.LENGTH_SHORT).show()
                    
                    // Llamar al ViewModel
                    viewModel.openTable(table.number, waiterId, waiterName)
                    
                    // Cerrar el diálogo
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor ingresa el nombre del mesero", Toast.LENGTH_SHORT).show()
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
        
        // Enfocar el EditText automáticamente
        editText.requestFocus()
    }

    private fun showAddProductDialog(table: Table) {
        val availableProducts = viewModel.getAvailableProducts()
        
        if (availableProducts.isEmpty()) {
            Toast.makeText(this, "No hay productos disponibles. Ve al inventario y agrega productos primero.", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_product_to_table, null)
        val spinnerProduct = dialogView.findViewById<Spinner>(R.id.spinnerProduct)
        val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)
        val buttonMinus = dialogView.findViewById<Button>(R.id.buttonMinus)
        val buttonPlus = dialogView.findViewById<Button>(R.id.buttonPlus)
        val linearLayoutProductInfo = dialogView.findViewById<LinearLayout>(R.id.linearLayoutProductInfo)
        val textViewProductName = dialogView.findViewById<TextView>(R.id.textViewProductName)
        val textViewProductPrice = dialogView.findViewById<TextView>(R.id.textViewProductPrice)
        val textViewProductStock = dialogView.findViewById<TextView>(R.id.textViewProductStock)
        val textViewSubtotal = dialogView.findViewById<TextView>(R.id.textViewSubtotal)

        // Configurar spinner de productos
        val productNames = availableProducts.map { "${it.name} - ${numberFormat.format(it.price)}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = adapter

        var selectedProduct = availableProducts.firstOrNull()
        var currentQuantity = 1

        // Función para actualizar información del producto
        fun updateProductInfo() {
            selectedProduct?.let { product ->
                linearLayoutProductInfo.visibility = View.VISIBLE
                textViewProductName.text = product.name
                textViewProductPrice.text = "Precio: ${numberFormat.format(product.price)}"
                textViewProductStock.text = "Stock disponible: ${product.quantity}"
                textViewSubtotal.text = "Subtotal: ${numberFormat.format(product.price * currentQuantity)}"
            }
        }

        // Configurar botones de cantidad
        buttonMinus.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                editTextQuantity.setText(currentQuantity.toString())
                updateProductInfo()
            }
        }

        buttonPlus.setOnClickListener {
            selectedProduct?.let { product ->
                if (currentQuantity < product.quantity) {
                    currentQuantity++
                    editTextQuantity.setText(currentQuantity.toString())
                    updateProductInfo()
                }
            }
        }

        // Configurar spinner
        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProduct = availableProducts[position]
                currentQuantity = 1
                editTextQuantity.setText("1")
                updateProductInfo()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedProduct = null
                linearLayoutProductInfo.visibility = View.GONE
            }
        }

        // Mostrar información inicial
        updateProductInfo()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Producto a Mesa ${table.number}")
            .setView(dialogView)
            .setPositiveButton("Agregar", null) // Configurar después
            .setNegativeButton("Cancelar", null) // Configurar después
            .setCancelable(false)
            .create()
        
        // Configurar los botones después de crear el diálogo para que sean visibles
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            // Configurar colores y estilo del botón positivo
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Agregar Producto"
            
            // Configurar colores y estilo del botón negativo
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"
            
            // Configurar listeners
            positiveButton.setOnClickListener {
                selectedProduct?.let { product ->
                    val quantity = editTextQuantity.text.toString().toIntOrNull() ?: 1
                    if (quantity > 0 && quantity <= product.quantity) {
                        // Mostrar mensaje de confirmación
                        Toast.makeText(this, "Agregando ${quantity}x ${product.name} a Mesa ${table.number}", Toast.LENGTH_SHORT).show()
                        viewModel.addProductToTable(table.id, product, quantity)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }





    private fun showAddTableDialog() {
        // Por ahora, mostrar un mensaje simple
        Toast.makeText(this, "Las mesas se crean automáticamente (1-10)", Toast.LENGTH_SHORT).show()
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

    private fun iniciarActividadDeMesas() {
        setContentView(R.layout.activity_tables) // ← NECESARIO
        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupUI()
        observeViewModel()
        loadData()
    }

    // Lee SIEMPRE la mesa directo del SERVIDOR para evitar caché
    private fun fetchFreshTable(
        tableId: String,
        onOk: (Table) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("tables")
            .document(tableId)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                val fresh = doc.toObject(Table::class.java)?.copy(id = doc.id)
                if (fresh != null) onOk(fresh) else onError("Mesa no encontrada")
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error de red")
            }
    }


    // === CIERRE DE MESA DESDE EL LISTADO ===

    private fun closeTable(table: Table) {
        // 1) Traemos la mesa FRESCA del servidor
        fetchFreshTable(
            tableId = table.id,
            onOk = { fresh ->
                // Si realmente no hay items, avisar y NO cerrar.
                if (fresh.items.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No hay productos en la mesa para generar factura.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@fetchFreshTable
                }

                // 2) Mostramos el diálogo usando la info fresca (total, etc.)
                val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle("Cerrar Mesa ${fresh.number}")
                    .setMessage("¿Qué deseas hacer?")
                    .setPositiveButton("Cerrar sin propina", null)
                    .setNeutralButton("Cerrar con propina", null)
                    .setNegativeButton("Cancelar", null)
                    .create()

                dialog.setOnShowListener {
                    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val neutralButton  = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                    positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
                    positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    positiveButton.text = "Cerrar sin propina"

                    neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
                    neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    neutralButton.text = "Cerrar con propina"

                    negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                    negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    negativeButton.text = "Cancelar"

                    positiveButton.setOnClickListener {
                        closeTableWithoutTip(fresh)  // usamos la mesa fresca
                        dialog.dismiss()
                    }
                    neutralButton.setOnClickListener {
                        showTipDialog(fresh)         // usamos la mesa fresca
                        dialog.dismiss()
                    }
                    negativeButton.setOnClickListener { dialog.dismiss() }
                }

                dialog.show()
            },
            onError = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        )
    }


    private fun closeTableWithoutTip(table: Table) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Cierre")
            .setMessage("¿Estás seguro de que quieres cerrar la mesa ${table.number} sin propina?")
            .setPositiveButton("Sí, Cerrar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Sí, Cerrar"

            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"

            positiveButton.setOnClickListener {
                generateInvoiceAndCloseTable(table, 0.0)
                dialog.dismiss()
            }
            negativeButton.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private fun showTipDialog(table: Table) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tip_input, null)
        val editTextTip = dialogView.findViewById<EditText>(R.id.editTextTip)

        val standardTip = (table.totalAmount * Constants.STANDARD_TIP_PERCENTAGE).toInt()
        editTextTip.setText(standardTip.toString())
        editTextTip.hint = "Propina estándar: $${standardTip} (5%)"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Propina")
            .setView(dialogView)
            .setPositiveButton("Sí (5%)", null)
            .setNeutralButton("Personalizar", null)
            .setNegativeButton("No", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton  = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Sí (5%)"

            neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
            neutralButton.text = "Personalizar"

            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "No"

            positiveButton.setOnClickListener {
                generateInvoiceAndCloseTable(table, standardTip.toDouble())
                dialog.dismiss()
            }
            neutralButton.setOnClickListener {
                val customTip = editTextTip.text.toString().toDoubleOrNull() ?: standardTip.toDouble()
                generateInvoiceAndCloseTable(table, customTip)
                dialog.dismiss()
            }
            negativeButton.setOnClickListener {
                generateInvoiceAndCloseTable(table, 0.0)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun generateInvoiceAndCloseTable(table: Table, tipAmount: Double) {
        // 1) Guarda la propina en la mesa
        viewModel.updateTableTip(table.id, tipAmount)
        // 2) Cierra la mesa
        viewModel.closeTable(table.id)
        // 3) Refresca la lista (libres/ocupadas)
        viewModel.loadTables()

        Toast.makeText(this, "Mesa ${table.number} cerrada. Generando factura...", Toast.LENGTH_SHORT).show()

        // === 4) Construir y GUARDAR la factura (antes de abrir la pantalla) ===
        val subtotal = table.items.sumOf { it.subtotal }
        val total = subtotal + tipAmount

        val invoice = com.caferaquelita.restauranteapp.models.Invoice(
            id = java.util.UUID.randomUUID().toString(),
            invoiceNumber = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date()),
            tableId = table.id,
            tableNumber = table.number,
            waiterId = table.waiterId,
            waiterName = table.waiterName,
            items = table.items,          // items actuales de la mesa
            subtotal = subtotal,
            tax = 0.0,                    // ajusta si manejas IVA
            tip = tipAmount,
            total = total,                // el modelo expone total y totalAmount (getter)
            paymentMethod = "Efectivo",   // cámbialo si luego preguntas el método
            customerName = "",
            customerDocument = ""
        )

        // Guardar en Firestore y sumar a caja
        lifecycleScope.launch {
            try {
                invoiceRepo.saveInvoice(invoice)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TablesActivity,
                    "Error al guardar factura: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // 5) Abre la pantalla de factura
        val intent = Intent(this, InvoiceActivity::class.java).apply {
            putExtra("table_id", table.id)
            putExtra("table_number", table.number)
            putExtra("waiter_name", table.waiterName)
            putExtra("tip_amount", tipAmount)
            putExtra("invoice_id", invoice.id) // útil si luego quieres cargar esa factura
        }
        startActivity(intent)
    }


}