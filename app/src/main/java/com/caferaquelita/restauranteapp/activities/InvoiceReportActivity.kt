package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import java.io.File
import androidx.core.content.FileProvider
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.viewmodels.InvoiceViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para generar reportes de facturas por fecha.
 */
class InvoiceReportActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var datePicker: DatePicker
    private lateinit var buttonGenerateReport: Button
    private lateinit var buttonViewReport: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewStatus: TextView
    
    private lateinit var viewModel: InvoiceViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_report)

        // Inicializar ViewModel con contexto
        viewModel = InvoiceViewModel(this)
        
        setupViews()
        setupToolbar()
        setupUI()
        observeViewModel()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        datePicker = findViewById(R.id.datePicker)
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport)
        buttonViewReport = findViewById(R.id.buttonViewReport)
        progressBar = findViewById(R.id.progressBar)
        textViewStatus = findViewById(R.id.textViewStatus)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Reporte de Facturas"
        }
    }

    private fun setupUI() {
        // Configurar DatePicker para mostrar solo fecha
        datePicker.maxDate = System.currentTimeMillis()
        
        buttonGenerateReport.setOnClickListener {
            generateReport()
        }
        
        buttonViewReport.setOnClickListener {
            viewReport()
        }
        
        // Inicialmente deshabilitar botón de ver reporte
        buttonViewReport.isEnabled = false
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonGenerateReport.isEnabled = !isLoading
            buttonViewReport.isEnabled = !isLoading && viewModel.pdfUrl.value != null
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.pdfUrl.observe(this) { pdfUrl ->
            pdfUrl?.let {
                textViewStatus.text = "Reporte generado exitosamente"
                buttonViewReport.isEnabled = true
                showReportGeneratedDialog()
            }
        }
    }

    private fun generateReport() {
        val year = datePicker.year
        val month = datePicker.month
        val day = datePicker.dayOfMonth
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val selectedDate = calendar.time
        
        // Validar que la fecha no sea futura
        if (selectedDate.after(Date())) {
            Toast.makeText(this, "No se puede generar reporte para fechas futuras", Toast.LENGTH_LONG).show()
            return
        }
        
        textViewStatus.text = "Generando reporte para ${dateFormat.format(selectedDate)}..."
        viewModel.generateInvoiceReportPdf(selectedDate)
    }

    private fun viewReport() {
        val pdfUrl = viewModel.pdfUrl.value ?: run {
            Toast.makeText(this, "No hay reporte disponible", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (pdfUrl.startsWith("file://")) {
                // Archivo local → usar FileProvider
                val file = File(pdfUrl.removePrefix("file://"))
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } else {
                // URL remota → abrir por navegador/visor
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showReportGeneratedDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Reporte Generado")
            .setMessage("El reporte se ha generado exitosamente. ¿Qué deseas hacer?")
            .setPositiveButton("Ver Reporte", null)
            .setNegativeButton("Compartir", null)
            .setNeutralButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            // Aplicar colores a los botones
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            // Botón "Ver Reporte" - Verde
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            positiveButton.text = "Ver Reporte"

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
                viewReport()
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                shareReport()
                dialog.dismiss()
            }

            neutralButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun shareReport() {
        val pdfUrl = viewModel.pdfUrl.value ?: run {
            Toast.makeText(this, "No hay reporte disponible", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (pdfUrl.startsWith("file://")) {
                // Archivo local → adjuntar el PDF real
                val file = File(pdfUrl.removePrefix("file://"))
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de Facturas Café Raquelita")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Compartir reporte"))
            } else {
                // URL remota → compartir el enlace como texto
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de Facturas Café Raquelita")
                    putExtra(Intent.EXTRA_TEXT, pdfUrl)
                }
                startActivity(Intent.createChooser(intent, "Compartir reporte"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo compartir el reporte: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 