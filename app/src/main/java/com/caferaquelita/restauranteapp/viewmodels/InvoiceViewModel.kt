package com.caferaquelita.restauranteapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.repositories.InvoiceRepository
import com.caferaquelita.restauranteapp.repositories.TablesRepository
import com.caferaquelita.restauranteapp.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para la gestión de facturas.
 */
class InvoiceViewModel(private val context: Context) : ViewModel() {
    private val invoiceRepository = InvoiceRepository(context)
    private val tablesRepository = TablesRepository()
    
    private val _invoice = MutableLiveData<Invoice>()
    val invoice: LiveData<Invoice> = _invoice
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _pdfUrl = MutableLiveData<String?>()
    val pdfUrl: LiveData<String?> = _pdfUrl
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Generar factura a partir de una mesa.
     */
    fun generateInvoice(tableId: String, tableNumber: Int, waiterName: String, tipAmount: Double = 0.0) {
        // Validaciones de entrada
        if (tableId.isBlank()) {
            _errorMessage.value = "ID de mesa no válido"
            return
        }
        
        if (tableNumber <= 0) {
            _errorMessage.value = "Número de mesa no válido"
            return
        }
        
        if (waiterName.isBlank()) {
            _errorMessage.value = "Nombre del mesero es requerido"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val tableResult = tablesRepository.getTableById(tableId)
                if (tableResult.isSuccess) {
                    val table = tableResult.getOrNull()
                    if (table != null) {
                        if (table.items.isEmpty()) {
                            _errorMessage.value = "La mesa no tiene productos para facturar"
                            return@launch
                        }
                        
                        val invoice = createInvoiceFromTable(table, tableNumber, waiterName, 0.0)
                        _invoice.value = invoice
                    } else {
                        _errorMessage.value = "No se pudo obtener la información de la mesa"
                    }
                } else {
                    val exception = tableResult.exceptionOrNull()
                    _errorMessage.value = "Error al obtener la mesa: ${exception?.message ?: "Error desconocido"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al generar la factura: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generar PDF de la factura individual.
     */
    fun generateInvoicePdf() {
        val currentInvoice = _invoice.value ?: run {
            _errorMessage.value = "No hay factura para generar PDF"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val pdfUrl = invoiceRepository.generateInvoicePdf(currentInvoice)
                _pdfUrl.value = pdfUrl
            } catch (e: Exception) {
                _errorMessage.value = "Error al generar PDF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generar reporte PDF de facturas por fecha.
     */
    fun generateInvoiceReportPdf(date: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val pdfUrl = invoiceRepository.generateInvoiceReportPdf(date)
                _pdfUrl.value = pdfUrl
            } catch (e: Exception) {
                _errorMessage.value = "Error al generar reporte PDF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Compartir factura.
     */
    fun shareInvoice() {
        val currentInvoice = _invoice.value ?: run {
            _errorMessage.value = "No hay factura para compartir"
            return
        }
        // Implementar lógica de compartir
    }

    /**
     * Imprimir factura.
     */
    fun printInvoice() {
        val currentInvoice = _invoice.value ?: run {
            _errorMessage.value = "No hay factura para imprimir"
            return
        }
        // Implementar lógica de impresión
    }

    /**
     * Crear factura a partir de los datos de la mesa.
     */
    private fun createInvoiceFromTable(table: Table, tableNumber: Int, waiterName: String, tipAmount: Double = 0.0): Invoice {
        val invoiceNumber = generateInvoiceNumber()
        val subtotal = table.totalAmount // Los precios ya incluyen IVA
        val tax = 0.0 // IVA ya incluido en precios
        
        // Si no se proporciona propina, usar el 5% estándar
        val tip = if (tipAmount > 0) tipAmount else (subtotal * Constants.STANDARD_TIP_PERCENTAGE)
        val total = subtotal + tip
        
        return Invoice(
            id = table.id,
            invoiceNumber = invoiceNumber,
            tableId = table.id,
            tableNumber = tableNumber,
            waiterId = table.waiterId,
            waiterName = waiterName,
            items = table.items, // Incluir todos los productos de la mesa
            subtotal = subtotal,
            tax = tax,
            tip = tip,
            total = total,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Generar número de factura único.
     */
    private fun generateInvoiceNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val random = (1000..9999).random()
        return "FAC-$today-$random"
    }
    
    /**
     * Limpiar errores.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Limpiar datos al destruir ViewModel.
     */
    override fun onCleared() {
        super.onCleared()
        _invoice.value = null
        _pdfUrl.value = null
        _errorMessage.value = null
    }
} 