package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.repositories.InventoryRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión del inventario.
 */
class InventoryViewModel : ViewModel() {
    private val inventoryRepository = InventoryRepository()
    
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> get() = _products
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    /**
     * Cargar productos del inventario
     */
    fun loadProducts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = inventoryRepository.getProducts()
                result.fold(
                    onSuccess = { products ->
                        if (products.isEmpty()) {
                            // Si no hay productos, agregar algunos de prueba
                            addSampleProducts()
                        } else {
                            _products.value = products
                        }
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al cargar productos: ${exception.message}"
                        // Agregar productos de prueba en caso de error
                        addSampleProducts()
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
                // Agregar productos de prueba en caso de error
                addSampleProducts()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Agregar productos de muestra para pruebas
     */
    private fun addSampleProducts() {
        viewModelScope.launch {
            try {
                val sampleProducts = listOf(
                    Product(name = "Café Americano", price = 5000.0, category = "Bebidas", quantity = 100, minQuantity = 10),
                    Product(name = "Café Latte", price = 6000.0, category = "Bebidas", quantity = 80, minQuantity = 10),
                    Product(name = "Cappuccino", price = 5500.0, category = "Bebidas", quantity = 90, minQuantity = 10),
                    Product(name = "Té Verde", price = 4000.0, category = "Bebidas", quantity = 50, minQuantity = 5),
                    Product(name = "Croissant", price = 3000.0, category = "Postres", quantity = 30, minQuantity = 5),
                    Product(name = "Torta de Chocolate", price = 8000.0, category = "Postres", quantity = 20, minQuantity = 3),
                    Product(name = "Sandwich de Pollo", price = 12000.0, category = "Alimentos", quantity = 25, minQuantity = 5),
                    Product(name = "Ensalada César", price = 15000.0, category = "Alimentos", quantity = 15, minQuantity = 3)
                )
                
                sampleProducts.forEach { product ->
                    inventoryRepository.addProduct(product)
                }
                
                // Recargar productos después de agregar los de muestra
                val result = inventoryRepository.getProducts()
                result.fold(
                    onSuccess = { products ->
                        _products.value = products
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al cargar productos de muestra: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error al agregar productos de muestra: ${e.message}"
            }
        }
    }

    /**
     * Agregar un nuevo producto a Firebase
     */
    fun addProduct(product: Product) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = inventoryRepository.addProduct(product)
                result.fold(
                    onSuccess = { 
                        loadProducts() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al agregar producto: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar un producto existente en Firebase
     */
    fun updateProduct(product: Product) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = inventoryRepository.updateProduct(product)
                result.fold(
                    onSuccess = { 
                        loadProducts() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al actualizar producto: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Eliminar un producto de Firebase
     */
    fun deleteProduct(productId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = inventoryRepository.deleteProduct(productId)
                result.fold(
                    onSuccess = { 
                        loadProducts() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al eliminar producto: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Verificar productos con cantidad mínima
     */
    fun checkMinQuantity() {
        val lowStockProducts = _products.value?.filter { it.quantity <= it.minQuantity } ?: emptyList()
        if (lowStockProducts.isNotEmpty()) {
            _errorMessage.value = "Productos con stock bajo: ${lowStockProducts.joinToString(", ") { it.name }}"
        }
    }
} 