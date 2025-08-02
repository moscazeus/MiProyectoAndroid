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
import com.caferaquelita.restauranteapp.adapters.TableProductManagementAdapter
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableItem
import com.caferaquelita.restauranteapp.viewmodels.TablesViewModel
import com.caferaquelita.restauranteapp.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.*

/**
 * Actividad para gestionar productos de una mesa específica.
 * Mejorada para ser más profesional e intuitiva.
 */
class TableProductsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var linearLayoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var textViewTableInfo: TextView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonCloseTable: Button
    private lateinit var buttonSaveOrder: Button
    
    private val viewModel: TablesViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    private var currentTable: Table? = null
    private lateinit var tableProductAdapter: TableProductManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_products)

        setupViews()
        setupToolbar()
        setupUI()
        observeViewModel()
        loadTableData()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando se vuelve a la actividad
        loadTableData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        linearLayoutEmpty = findViewById(R.id.linearLayoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        fabAddProduct = findViewById(R.id.fabAddProduct)
        textViewTableInfo = findViewById(R.id.textViewTableInfo)
        textViewTotal = findViewById(R.id.textViewTotal)
        buttonCloseTable = findViewById(R.id.buttonCloseTable)
        buttonSaveOrder = findViewById(R.id.buttonSaveOrder)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Gestión de Pedido"
        }
    }

    private fun setupUI() {
        setupRecyclerView()
        
        // Configurar botón de agregar producto
        fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }
        
        // Configurar botón de guardar pedido
        buttonSaveOrder.setOnClickListener {
            saveOrderAndReturn()
        }
        
        // Configurar botón de cerrar mesa
        buttonCloseTable.setOnClickListener {
            closeTable()
        }
    }

    private fun setupRecyclerView() {
        tableProductAdapter = TableProductManagementAdapter(
            emptyList(),
            onQuantityChange = { item, newQuantity ->
                currentTable?.let { table ->
                    viewModel.updateProductQuantity(table.id, item.productId, newQuantity)
                }
            },
            onRemoveProduct = { item ->
                currentTable?.let { table ->
                    viewModel.removeProductFromTable(table.id, item.productId)
                }
            }
        )
        
        recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@TableProductsActivity)
            adapter = tableProductAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.tables.observe(this) { tables ->
            val tableId = intent.getStringExtra("table_id")
            currentTable = tables.find { it.id == tableId }
            updateTableInfo()
        }

        viewModel.products.observe(this) { products ->
            // Los productos se cargan automáticamente cuando se necesitan
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.successMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                // Recargar datos después de una operación exitosa
                viewModel.loadTables()
                viewModel.clearMessages()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun loadTableData() {
        val tableId = intent.getStringExtra("table_id")
        if (tableId != null) {
            viewModel.loadTables()
            viewModel.loadProducts() // Cargar productos del inventario
        }
    }

    private fun updateTableInfo() {
        currentTable?.let { table ->
            textViewTableInfo.text = "Mesa ${table.number} - ${table.waiterName}"
            textViewTotal.text = "Total: ${numberFormat.format(table.totalAmount)}"
            
            if (table.items.isNotEmpty()) {
                linearLayoutEmpty.visibility = View.GONE
                recyclerViewProducts.visibility = View.VISIBLE
                tableProductAdapter.updateItems(table.items)
            } else {
                linearLayoutEmpty.visibility = View.VISIBLE
                recyclerViewProducts.visibility = View.GONE
            }
        }
    }

    /**
     * Guardar el pedido actual y volver a la pantalla de mesas
     * sin cerrar la mesa
     */
    private fun saveOrderAndReturn() {
        currentTable?.let { table ->
            if (table.items.isEmpty()) {
                Toast.makeText(this, "No hay productos en el pedido para guardar", Toast.LENGTH_LONG).show()
                return
            }

            // Mostrar diálogo de confirmación
            val dialog = AlertDialog.Builder(this)
                .setTitle("Guardar Pedido")
                .setMessage("¿Estás seguro de que quieres guardar el pedido actual y volver a la gestión de mesas?\n\n" +
                        "La mesa permanecerá abierta y podrás volver a editarla más tarde.")
                .setPositiveButton("Guardar y Volver", null)
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                // Botón "Guardar y Volver" - Verde
                positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
                positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                positiveButton.text = "Guardar y Volver"

                // Botón "Cancelar" - Rojo
                negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                negativeButton.text = "Cancelar"

                positiveButton.setOnClickListener {
                    Toast.makeText(this, "Pedido guardado exitosamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    finish() // Volver a la pantalla anterior
                }

                negativeButton.setOnClickListener {
                    dialog.dismiss()
                }
            }

            dialog.show()
        }
    }

    private fun showAddProductDialog() {
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
            .setTitle("Agregar Producto")
            .setView(dialogView)
            .setPositiveButton("Agregar", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Agregar Producto"
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"
            
            positiveButton.setOnClickListener {
                selectedProduct?.let { product ->
                    val quantity = editTextQuantity.text.toString().toIntOrNull() ?: 1
                    if (quantity > 0 && quantity <= product.quantity) {
                        currentTable?.let { table ->
                            viewModel.addProductToTable(table.id, product, quantity)
                            Toast.makeText(this, "Agregando ${quantity}x ${product.name}", Toast.LENGTH_SHORT).show()
                        }
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

    private fun closeTable() {
        currentTable?.let { table ->
            if (table.items.isEmpty()) {
                Toast.makeText(this, "No hay productos en la mesa para generar factura.", Toast.LENGTH_LONG).show()
                return
            }

            // Crear diálogo con botones visibles y colores
            val dialog = AlertDialog.Builder(this)
                .setTitle("Cerrar Mesa ${table.number}")
                .setMessage("¿Qué deseas hacer?")
                .setPositiveButton("Cerrar sin propina", null)
                .setNeutralButton("Cerrar con propina", null)
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.setOnShowListener {
                // Aplicar colores a los botones
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                // Botón "Cerrar sin propina" - Verde
                positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
                positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                positiveButton.text = "Cerrar sin propina"

                // Botón "Cerrar con propina" - Azul
                neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
                neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
                neutralButton.text = "Cerrar con propina"

                // Botón "Cancelar" - Rojo
                negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                negativeButton.text = "Cancelar"

                // Configurar listeners
                positiveButton.setOnClickListener {
                    closeTableWithoutTip(table)
                    dialog.dismiss()
                }

                neutralButton.setOnClickListener {
                    showTipDialog(table)
                    dialog.dismiss()
                }

                negativeButton.setOnClickListener {
                    dialog.dismiss()
                }
            }

            dialog.show()
        }
    }

    private fun closeTableWithoutTip(table: Table) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar Cierre")
            .setMessage("¿Estás seguro de que quieres cerrar la mesa ${table.number} sin propina?")
            .setPositiveButton("Sí, Cerrar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Botón "Sí, Cerrar" - Verde
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Sí, Cerrar"

            // Botón "Cancelar" - Rojo
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Cancelar"

            // Configurar listeners
            positiveButton.setOnClickListener {
                generateInvoiceAndCloseTable(table, 0.0)
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showTipDialog(table: Table) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tip_input, null)
        val editTextTip = dialogView.findViewById<EditText>(R.id.editTextTip)
        
        // Calcular propina estándar del 5%
        val standardTip = (table.totalAmount * Constants.STANDARD_TIP_PERCENTAGE).toInt()
        editTextTip.setText(standardTip.toString())
        editTextTip.hint = "Propina estándar: $${standardTip} (5%)"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Propina")
            .setView(dialogView)
            .setPositiveButton("Sí (5%)", null)
            .setNeutralButton("Personalizar", null)
            .setNegativeButton("No", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Botón "Sí (5%)" - Verde
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Sí (5%)"

            // Botón "Personalizar" - Azul
            neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
            neutralButton.text = "Personalizar"

            // Botón "No" - Rojo
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "No"

            // Configurar listeners
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

    private fun showRemoveProductsDialog(table: Table) {
        // Navegar a la pantalla de gestión de productos (ya estamos ahí)
        Toast.makeText(this, "Usa los botones de quitar productos arriba", Toast.LENGTH_LONG).show()
    }

    private fun generateInvoiceAndCloseTable(table: Table, tipAmount: Double) {
        // Actualizar la mesa con la propina y cerrarla
        viewModel.updateTableTip(table.id, tipAmount)
        
        // Cerrar la mesa inmediatamente
        viewModel.closeTable(table.id)
        
        // Forzar actualización de datos
        viewModel.loadTables()
        
        Toast.makeText(this, "Mesa ${table.number} cerrada. Generando factura...", Toast.LENGTH_SHORT).show()
        
        // Navegar a la actividad de facturación
        val intent = Intent(this, InvoiceActivity::class.java).apply {
            putExtra("table_id", table.id)
            putExtra("table_number", table.number)
            putExtra("waiter_name", table.waiterName)
            putExtra("tip_amount", tipAmount)
        }
        startActivity(intent)
        finish() // Cerrar esta actividad
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

    override fun onBackPressed() {
        // Al presionar atrás, preguntar si quiere guardar el pedido
        currentTable?.let { table ->
            if (table.items.isNotEmpty()) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Salir sin guardar")
                    .setMessage("¿Quieres guardar el pedido antes de salir?")
                    .setPositiveButton("Guardar y Salir", null)
                    .setNegativeButton("Salir sin guardar", null)
                    .setNeutralButton("Cancelar", null)
                    .create()

                dialog.setOnShowListener {
                    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

                    // Botón "Guardar y Salir" - Verde
                    positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
                    positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    positiveButton.text = "Guardar y Salir"

                    // Botón "Salir sin guardar" - Rojo
                    negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
                    negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    negativeButton.text = "Salir sin guardar"

                    // Botón "Cancelar" - Gris
                    neutralButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null))
                    neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
                    neutralButton.text = "Cancelar"

                    positiveButton.setOnClickListener {
                        Toast.makeText(this, "Pedido guardado", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        super.onBackPressed()
                    }

                    negativeButton.setOnClickListener {
                        dialog.dismiss()
                        super.onBackPressed()
                    }

                    neutralButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }

                dialog.show()
            } else {
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }
} 