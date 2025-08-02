package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.caferaquelita.restauranteapp.adapters.InvoiceItemAdapter
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.viewmodels.InvoiceViewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para generar y mostrar facturas.
 */
class InvoiceActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var textViewInvoiceNumber: TextView
    private lateinit var textViewDate: TextView
    private lateinit var textViewTableNumber: TextView
    private lateinit var textViewWaiterName: TextView
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textViewSubtotal: TextView
    private lateinit var textViewTax: TextView
    private lateinit var textViewTip: TextView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonGeneratePDF: Button
    private lateinit var buttonShare: Button
    private lateinit var buttonPrint: Button
    private lateinit var progressBar: ProgressBar
    
    private lateinit var invoiceItemAdapter: InvoiceItemAdapter
    private lateinit var viewModel: InvoiceViewModel
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        // Inicializar ViewModel con contexto
        viewModel = InvoiceViewModel(this)
        
        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupUI()
        observeViewModel()
        
        // Obtener datos de la mesa desde el intent
        val tableId = intent.getStringExtra("table_id")
        val tableNumber = intent.getIntExtra("table_number", 0)
        val waiterName = intent.getStringExtra("waiter_name") ?: ""
        val tipAmount = intent.getDoubleExtra("tip_amount", 0.0)
        
        if (tableId != null) {
            viewModel.generateInvoice(tableId, tableNumber, waiterName, tipAmount)
        }
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        textViewInvoiceNumber = findViewById(R.id.textViewInvoiceNumber)
        textViewDate = findViewById(R.id.textViewDate)
        textViewTableNumber = findViewById(R.id.textViewTableNumber)
        textViewWaiterName = findViewById(R.id.textViewWaiterName)
        recyclerViewItems = findViewById(R.id.recyclerViewItems)
        textViewSubtotal = findViewById(R.id.textViewSubtotal)
        textViewTax = findViewById(R.id.textViewTax)
        textViewTip = findViewById(R.id.textViewTip)
        textViewTotal = findViewById(R.id.textViewTotal)
        buttonGeneratePDF = findViewById(R.id.buttonGeneratePDF)
        buttonShare = findViewById(R.id.buttonShare)
        buttonPrint = findViewById(R.id.buttonPrint)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Factura"
        }
    }

    private fun setupRecyclerView() {
        invoiceItemAdapter = InvoiceItemAdapter(emptyList())
        recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@InvoiceActivity)
            adapter = invoiceItemAdapter
        }
    }

    private fun setupUI() {
        buttonGeneratePDF.setOnClickListener {
            viewModel.generateInvoicePdf()
        }
        
        buttonShare.setOnClickListener {
            viewModel.shareInvoice()
        }
        
        buttonPrint.setOnClickListener {
            viewModel.printInvoice()
        }
    }

    private fun observeViewModel() {
        viewModel.invoice.observe(this) { invoice ->
            if (invoice != null) {
                displayInvoice(invoice)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonGeneratePDF.isEnabled = !isLoading
            buttonShare.isEnabled = !isLoading
            buttonPrint.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.pdfUrl.observe(this) { pdfUrl ->
            pdfUrl?.let {
                showPdfGeneratedDialog()
            }
        }
    }

    private fun displayInvoice(invoice: Invoice) {
        // Mostrar información básica de la factura
        textViewInvoiceNumber.text = invoice.invoiceNumber
        textViewDate.text = dateFormat.format(Date(invoice.createdAt))
        textViewTableNumber.text = "Mesa: ${invoice.tableNumber}"
        textViewWaiterName.text = "Mesero: ${invoice.waiterName}"
        
        // Debug: Mostrar información de productos
        Log.d("InvoiceActivity", "Factura items: ${invoice.items.size}")
        invoice.items.forEach { item ->
            Log.d("InvoiceActivity", "Producto: ${item.productName}, Cantidad: ${item.quantity}, Precio: ${item.productPrice}")
        }
        
        // Mostrar productos en el RecyclerView
        if (invoice.items.isNotEmpty()) {
            invoiceItemAdapter.updateItems(invoice.items)
            recyclerViewItems.visibility = View.VISIBLE
            Log.d("InvoiceActivity", "Productos mostrados en RecyclerView")
        } else {
            recyclerViewItems.visibility = View.GONE
            Log.d("InvoiceActivity", "No hay productos para mostrar")
        }
        
        // Mostrar totales
        textViewSubtotal.text = "Subtotal: ${numberFormat.format(invoice.subtotal)}"
        textViewTax.text = "IVA (19%): ${numberFormat.format(invoice.tax)}"
        textViewTip.text = "Propina: ${numberFormat.format(invoice.tip)}"
        textViewTotal.text = "Total: ${numberFormat.format(invoice.total)}"
        
        // Habilitar botones
        buttonGeneratePDF.isEnabled = true
        buttonShare.isEnabled = true
        buttonPrint.isEnabled = true
    }

    private fun showPdfGeneratedDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("PDF Generado")
            .setMessage("La factura se ha generado exitosamente. ¿Qué deseas hacer?")
            .setPositiveButton("Ver PDF", null)
            .setNegativeButton("Compartir", null)
            .setNeutralButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            // Aplicar colores a los botones
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            // Botón "Ver PDF" - Verde
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Ver PDF"

            // Botón "Compartir" - Azul
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            negativeButton.text = "Compartir"

            // Botón "Cancelar" - Rojo
            neutralButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            neutralButton.setTextColor(getResources().getColor(android.R.color.white, null))
            neutralButton.text = "Cancelar"

            // Configurar listeners
            positiveButton.setOnClickListener {
                openPdf()
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                sharePdf()
                dialog.dismiss()
            }

            neutralButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun openPdf() {
        try {
            val pdfUrl = viewModel.pdfUrl.value
            if (pdfUrl != null) {
                // Si es un archivo local, usar FileProvider
                if (pdfUrl.startsWith("file://")) {
                    val file = File(pdfUrl.substring(7))
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "text/html")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                } else {
                    // Si es una URL remota
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.parse(pdfUrl), "text/html")
                    intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "No hay archivo disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        try {
            val pdfUrl = viewModel.pdfUrl.value
            if (pdfUrl != null) {
                            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/html"
                
                // Si es un archivo local, usar FileProvider
                if (pdfUrl.startsWith("file://")) {
                    val file = File(pdfUrl.substring(7))
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pdfUrl))
                }
                
                intent.putExtra(Intent.EXTRA_SUBJECT, "Factura Café Raquelita")
                startActivity(Intent.createChooser(intent, "Compartir factura"))
            } else {
                Toast.makeText(this, "No hay archivo disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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