package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.adapters.InvoiceHistoryAdapter
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.viewmodels.InvoiceHistoryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para mostrar el historial de facturas.
 */
class InvoiceHistoryActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewInvoices: RecyclerView
    private lateinit var textViewEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonGenerateReport: Button
    
    private lateinit var invoiceHistoryAdapter: InvoiceHistoryAdapter
    private val viewModel: InvoiceHistoryViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_history)

        setupViews()
        setupToolbar()
        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadInvoices()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerViewInvoices = findViewById(R.id.recyclerViewInvoices)
        textViewEmpty = findViewById(R.id.textViewEmpty)
        progressBar = findViewById(R.id.progressBar)
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Historial de Facturas"
        }
    }

    private fun setupUI() {
        buttonGenerateReport.setOnClickListener {
            startActivity(Intent(this, InvoiceReportActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        invoiceHistoryAdapter = InvoiceHistoryAdapter(
            invoices = emptyList(),
            onInvoiceClick = { invoice ->
                openInvoiceDetail(invoice)
            }
        )
        recyclerViewInvoices.apply {
            layoutManager = LinearLayoutManager(this@InvoiceHistoryActivity)
            adapter = invoiceHistoryAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.invoices.observe(this) { invoices ->
            if (invoices.isEmpty()) {
                textViewEmpty.visibility = View.VISIBLE
                recyclerViewInvoices.visibility = View.GONE
            } else {
                textViewEmpty.visibility = View.GONE
                recyclerViewInvoices.visibility = View.VISIBLE
                invoiceHistoryAdapter.updateInvoices(invoices)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadInvoices() {
        viewModel.loadInvoices()
    }

    private fun openInvoiceDetail(invoice: Invoice) {
        val intent = Intent(this, InvoiceActivity::class.java).apply {
            putExtra("invoice_id", invoice.id)
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