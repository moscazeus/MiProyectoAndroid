package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.Invoice
import com.caferaquelita.restauranteapp.repositories.InvoiceHistoryRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para el historial de facturas.
 */
class InvoiceHistoryViewModel : ViewModel() {
    private val invoiceRepository = InvoiceHistoryRepository()
    
    private val _invoices = MutableLiveData<List<Invoice>>()
    val invoices: LiveData<List<Invoice>> = _invoices
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Cargar todas las facturas.
     */
    fun loadInvoices() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = invoiceRepository.getAllInvoices()
                if (result.isSuccess) {
                    val invoices = result.getOrNull() ?: emptyList()
                    _invoices.value = invoices
                    android.util.Log.d("InvoiceHistoryViewModel", "Facturas cargadas: ${invoices.size}")
                    invoices.forEach { invoice ->
                        android.util.Log.d("InvoiceHistoryViewModel", "Factura: ${invoice.invoiceNumber}, Productos: ${invoice.items.size}")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _errorMessage.value = "Error al cargar facturas: $error"
                    android.util.Log.e("InvoiceHistoryViewModel", "Error al cargar facturas: $error")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
                android.util.Log.e("InvoiceHistoryViewModel", "Error inesperado: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
} 