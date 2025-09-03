package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caferaquelita.restauranteapp.R

/**
 * Actividad para la gestión de facturación.
 */
class BillingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)

        setupUI()
    }

    private fun setupUI() {
        // Botón de volver
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        // Generar nueva factura
        findViewById<Button>(R.id.buttonGenerateInvoice).setOnClickListener {
            showGenerateInvoiceDialog()
        }

        // Historial de facturas
        findViewById<Button>(R.id.buttonInvoiceHistory).setOnClickListener {
            showInvoiceHistory()
        }

        // Exportar facturas
        findViewById<Button>(R.id.buttonExportInvoices).setOnClickListener {
            showExportInvoices()
        }

        // Configuración de facturas
        findViewById<Button>(R.id.buttonInvoiceSettings).setOnClickListener {
            showInvoiceSettings()
        }
    }

    private fun showGenerateInvoiceDialog() {
        // Abre la pantalla donde generas/ves la factura de una mesa
        val intent = Intent(this, InvoiceActivity::class.java)
        startActivity(intent)
    }

    private fun showInvoiceHistory() {
        val intent = Intent(this, InvoiceHistoryActivity::class.java)
        startActivity(intent)
    }


    private fun showExportInvoices() {
        val intent = Intent(this, InvoiceReportActivity::class.java)
        startActivity(intent)
    }


    private fun showInvoiceSettings() {
        Toast.makeText(this, "Configuración de facturas próximamente", Toast.LENGTH_SHORT).show()
    }

} 