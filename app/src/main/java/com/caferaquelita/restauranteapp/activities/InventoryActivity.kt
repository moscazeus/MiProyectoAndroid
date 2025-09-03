package com.caferaquelita.restauranteapp.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.ProductAdapter
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.viewmodels.InventoryViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream

/**
 * Actividad para gestionar el inventario de productos.
 */
class InventoryActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var textViewEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddProduct: FloatingActionButton
    
    private lateinit var productAdapter: ProductAdapter
    private val viewModel: InventoryViewModel by viewModels()
    
    private var selectedImageUri: Uri? = null
    private var selectedImageBytes: ByteArray? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            selectedImageUri = it
            try {
                val inputStream = contentResolver.openInputStream(it)
                selectedImageBytes = inputStream?.readBytes()
                inputStream?.close()
                
                // Actualizar la imagen en el diálogo
                updateDialogImage()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupUI()
        observeViewModel()
        loadProducts()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        searchView = findViewById(R.id.searchView)
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        textViewEmpty = findViewById(R.id.textViewEmpty)
        progressBar = findViewById(R.id.progressBar)
        fabAddProduct = findViewById(R.id.fabAddProduct)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Gestión de Inventario"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products = emptyList(),
            onEditClick = { product -> showEditProductDialog(product) },
            onDeleteClick = { product -> showDeleteProductDialog(product) }
        )
        
        recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@InventoryActivity)
            adapter = productAdapter
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productAdapter.filterProducts(newText ?: "")
                return true
            }
        })
    }

    private fun setupUI() {
        fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(this) { products ->
            productAdapter.updateProducts(products)
            updateEmptyState(products.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProducts() {
        viewModel.loadProducts()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        textViewEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewProducts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddProductDialog() {
        showProductDialog(null)
    }

    private fun showEditProductDialog(product: Product) {
        showProductDialog(product)
    }

    private fun showProductDialog(product: Product?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_product, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Referencias a las vistas del diálogo
        val textViewTitle = dialogView.findViewById<TextView>(R.id.textViewDialogTitle)
        val imageViewProduct = dialogView.findViewById<ImageView>(R.id.imageViewProduct)
        val btnSelectImage = dialogView.findViewById<Button>(R.id.btnSelectImage)
        val editTextName = dialogView.findViewById<EditText>(R.id.editTextProductName)
        val editTextPrice = dialogView.findViewById<EditText>(R.id.editTextProductPrice)
        val editTextCategory = dialogView.findViewById<EditText>(R.id.editTextProductCategory)
        val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextProductQuantity)
        val editTextMinQuantity = dialogView.findViewById<EditText>(R.id.editTextProductMinQuantity)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Configurar título
        textViewTitle.text = if (product == null) "Agregar Producto" else "Editar Producto"

        // Si es edición, llenar los campos
        if (product != null) {
            editTextName.setText(product.name)
            editTextPrice.setText(product.price.toString())
            editTextCategory.setText(product.category)
            editTextQuantity.setText(product.quantity.toString())
            editTextMinQuantity.setText(product.minQuantity.toString())
            
            // TODO: Cargar imagen del producto si existe
        }

        // Configurar botón de selección de imagen
        btnSelectImage.setOnClickListener {
            getContent.launch("image/*")
        }

        // Configurar botón cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Configurar botón guardar
        btnSave.setOnClickListener {
            val name = editTextName.text.toString()
            val priceStr = editTextPrice.text.toString()
            val category = editTextCategory.text.toString()
            val quantityStr = editTextQuantity.text.toString()
            val minQuantityStr = editTextMinQuantity.text.toString()

            if (name.isBlank() || priceStr.isBlank() || category.isBlank() || 
                quantityStr.isBlank() || minQuantityStr.isBlank()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val price = priceStr.toDouble()
                val quantity = quantityStr.toInt()
                val minQuantity = minQuantityStr.toInt()

                val newProduct = Product(
                    id = product?.id ?: "",
                    name = name,
                    price = price,
                    category = category,
                    quantity = quantity,
                    minQuantity = minQuantity,
                    imageUrl = "" // TODO: Implementar subida de imagen a Firebase Storage
                )

                if (product == null) {
                    viewModel.addProduct(newProduct)
                } else {
                    viewModel.updateProduct(newProduct)
                }

                dialog.dismiss()
                Toast.makeText(this, "Producto guardado exitosamente", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Precio y cantidades deben ser números válidos", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateDialogImage() {
        selectedImageBytes?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            // TODO: Actualizar la imagen en el diálogo actual
        }
    }

    private fun showDeleteProductDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar '${product.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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