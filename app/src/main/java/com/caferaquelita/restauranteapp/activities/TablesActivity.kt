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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.TableAdapter
import com.caferaquelita.restauranteapp.adapters.TableProductAdapter
import com.caferaquelita.restauranteapp.adapters.TableItemSummaryAdapter
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableItem
import com.caferaquelita.restauranteapp.models.TableStatus
import com.caferaquelita.restauranteapp.viewmodels.TablesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

/**
 * Actividad para la gestión de mesas.
 * Mejorada para ser más profesional e intuitiva.
 */
class TablesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewTables: RecyclerView
    private lateinit var linearLayoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddTable: FloatingActionButton
    private lateinit var textViewFreeTables: TextView
    private lateinit var textViewOccupiedTables: TextView
    
    private lateinit var tableAdapter: TableAdapter
    private val viewModel: TablesViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tables)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupUI()
        observeViewModel()
        loadData()
    }

    private fun setupViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            recyclerViewTables = findViewById(R.id.recyclerViewTables)
            linearLayoutEmpty = findViewById(R.id.textViewEmpty)
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
            onCloseTable = { table -> showCloseTableDialog(table) }
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
        
        textViewFreeTables.text = "Mesas Libres: $freeTables"
        textViewOccupiedTables.text = "Mesas Ocupadas: $occupiedTables"
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
        
        val dialog = AlertDialog.Builder(this)
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

        val dialog = AlertDialog.Builder(this)
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

    private fun showCloseTableDialog(table: Table) {
        if (table.items.isEmpty()) {
            // Si la mesa no tiene productos, cerrar directamente
            val dialog = AlertDialog.Builder(this)
                .setTitle("Cerrar Mesa ${table.number}")
                .setMessage("¿Estás seguro de que quieres cerrar esta mesa?")
                .setPositiveButton("Cerrar", null)
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .create()
            
            // Configurar colores de los botones
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                
                positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                
                negativeButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null))
                negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                
                positiveButton.setOnClickListener {
                    viewModel.closeTable(table.id)
                    dialog.dismiss()
                }
                
                negativeButton.setOnClickListener {
                    dialog.dismiss()
                }
            }
            
            dialog.show()
        } else {
            // Si la mesa tiene productos, mostrar opción de quitar productos o cerrar con propina
            val dialog = AlertDialog.Builder(this)
                .setTitle("Cerrar Mesa ${table.number}")
                .setMessage("¿Qué deseas hacer?\n\n1. Quitar productos antes de cerrar\n2. Cerrar mesa con propina\n3. Cerrar mesa directamente")
                .setPositiveButton("Quitar Productos", null)
                .setNegativeButton("Cerrar con Propina", null)
                .setNeutralButton("Cerrar Directamente", null)
                .setCancelable(false)
                .create()
            
            // Configurar colores y listeners de los botones
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                
                // Configurar botón "Quitar Productos"
                positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
                positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                positiveButton.text = "Quitar Productos"
                positiveButton.setOnClickListener {
                    showRemoveProductsDialog(table)
                    dialog.dismiss()
                }
                
                // Configurar botón "Cerrar con Propina"
                negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark, null))
                negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                negativeButton.text = "Cerrar con Propina"
                negativeButton.setOnClickListener {
                    showCloseWithTipDialog(table)
                    dialog.dismiss()
                }
                
                // Configurar botón "Cerrar Directamente"
                neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
                neutralButton.text = "Cerrar Directamente"
                neutralButton.setOnClickListener {
                    viewModel.closeTable(table.id)
                    dialog.dismiss()
                }
            }
            
            dialog.show()
        }
    }

    private fun showCloseWithTipDialog(table: Table) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_close_table_with_tip, null)
        val recyclerViewTableItems = dialogView.findViewById<RecyclerView>(R.id.recyclerViewTableItems)
        val textViewSubtotal = dialogView.findViewById<TextView>(R.id.textViewSubtotal)
        val textViewTip = dialogView.findViewById<TextView>(R.id.textViewTip)
        val textViewTotal = dialogView.findViewById<TextView>(R.id.textViewTotal)
        val checkBoxIncludeTip = dialogView.findViewById<CheckBox>(R.id.checkBoxIncludeTip)

        // Configurar RecyclerView
        val tableItemSummaryAdapter = TableItemSummaryAdapter(table.items)
        recyclerViewTableItems.apply {
            layoutManager = LinearLayoutManager(this@TablesActivity)
            adapter = tableItemSummaryAdapter
        }

        // Calcular totales
        val subtotal = table.items.sumOf { it.subtotal }
        val tipPercentage = 0.05 // 5%
        val tipAmount = if (checkBoxIncludeTip.isChecked) (subtotal * tipPercentage).toInt() else 0
        val total = subtotal + tipAmount

        // Mostrar totales sin decimales
        textViewSubtotal.text = "Subtotal: $${subtotal.toInt()}"
        textViewTip.text = "Propina (5%): $${tipAmount}"
        textViewTotal.text = "Total a pagar: $${total.toInt()}"

        // Configurar checkbox para actualizar totales
        checkBoxIncludeTip.setOnCheckedChangeListener { _, isChecked ->
            val newTipAmount = if (isChecked) (subtotal * tipPercentage).toInt() else 0
            val newTotal = subtotal + newTipAmount
            
            textViewTip.text = "Propina (5%): $${newTipAmount}"
            textViewTotal.text = "Total a pagar: $${newTotal.toInt()}"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Cerrar Mesa ${table.number}")
            .setView(dialogView)
            .setPositiveButton("Generar Factura", null)
            .setNegativeButton("Cerrar Mesa", null)
            .setNeutralButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        // Configurar colores de los botones
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Generar Factura"
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cerrar Mesa"
            
            neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
            neutralButton.text = "Cancelar"
            
            positiveButton.setOnClickListener {
                val finalTipAmount = if (checkBoxIncludeTip.isChecked) (subtotal * tipPercentage).toInt() else 0
                // Por ahora solo generar la factura, la propina se manejará en la facturación
                generateInvoice(table)
                dialog.dismiss()
            }
            
            negativeButton.setOnClickListener {
                val finalTipAmount = if (checkBoxIncludeTip.isChecked) (subtotal * tipPercentage).toInt() else 0
                // Por ahora solo cerrar la mesa, la propina se manejará en la facturación
                viewModel.closeTable(table.id)
                dialog.dismiss()
            }
            
            neutralButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showRemoveProductsDialog(table: Table) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_remove_product_from_table, null)
        val recyclerViewTableItems = dialogView.findViewById<RecyclerView>(R.id.recyclerViewTableItems)
        val textViewTotalBefore = dialogView.findViewById<TextView>(R.id.textViewTotalBefore)
        val textViewTotalAfter = dialogView.findViewById<TextView>(R.id.textViewTotalAfter)

        // Configurar RecyclerView
        var tableProductAdapter: TableProductAdapter? = null
        tableProductAdapter = TableProductAdapter(
            tableItems = table.items,
            onItemCheckedChanged = { _, _ ->
                tableProductAdapter?.let { adapter ->
                    updateTotalAfterRemoval(table, adapter, textViewTotalAfter)
                }
            }
        )

        recyclerViewTableItems.apply {
            layoutManager = LinearLayoutManager(this@TablesActivity)
            adapter = tableProductAdapter
        }

        // Mostrar total actual
        val currentTotal = table.items.sumOf { it.subtotal } + table.tipAmount
        textViewTotalBefore.text = "Total actual: ${numberFormat.format(currentTotal)}"
        textViewTotalAfter.text = "Total después: ${numberFormat.format(currentTotal)}"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Quitar Productos de Mesa ${table.number}")
            .setView(dialogView)
            .setPositiveButton("Quitar Seleccionados", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        // Configurar colores de los botones
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Quitar Seleccionados"
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"
            
            positiveButton.setOnClickListener {
                val selectedItems = tableProductAdapter?.getSelectedItems() ?: emptyList()
                if (selectedItems.isNotEmpty()) {
                    // Quitar productos uno por uno
                    selectedItems.forEach { item ->
                        viewModel.removeProductFromTable(table.id, item.productId)
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "No has seleccionado productos para quitar", Toast.LENGTH_SHORT).show()
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun updateTotalAfterRemoval(table: Table, adapter: TableProductAdapter, textViewTotalAfter: TextView) {
        val selectedItems = adapter.getSelectedItems()
        val totalToRemove = selectedItems.sumOf { it.subtotal }
        val currentTotal = table.items.sumOf { it.subtotal } + table.tipAmount
        val newTotal = currentTotal - totalToRemove
        
        textViewTotalAfter.text = "Total después: ${numberFormat.format(newTotal)}"
    }

    private fun showAddTableDialog() {
        // Por ahora, mostrar un mensaje simple
        Toast.makeText(this, "Las mesas se crean automáticamente (1-10)", Toast.LENGTH_SHORT).show()
    }

    private fun generateInvoice(table: Table) {
        val intent = Intent(this, InvoiceActivity::class.java).apply {
            putExtra("table_id", table.id)
            putExtra("table_number", table.number)
            putExtra("waiter_name", table.waiterName)
        }
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