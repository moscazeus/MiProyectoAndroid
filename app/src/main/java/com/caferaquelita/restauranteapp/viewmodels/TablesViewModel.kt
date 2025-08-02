package com.caferaquelita.restauranteapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableItem
import com.caferaquelita.restauranteapp.models.TableStatus
import com.caferaquelita.restauranteapp.repositories.InventoryRepository
import com.caferaquelita.restauranteapp.repositories.TablesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Date

/**
 * ViewModel para la gestión de mesas.
 */
class TablesViewModel : ViewModel() {
    private val tablesRepository = TablesRepository()
    private val inventoryRepository = InventoryRepository()

    private val _tables = MutableLiveData<List<Table>>()
    val tables: LiveData<List<Table>> get() = _tables

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> get() = _successMessage

    /**
     * Cargar todas las mesas
     */
    fun loadTables() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.getTables()
                result.fold(
                    onSuccess = { tables ->
                        _tables.value = tables
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al cargar mesas: ${exception.message}"
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
     * Cargar todos los productos del inventario
     */
    fun loadProducts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = inventoryRepository.getProducts()
                result.fold(
                    onSuccess = { products ->
                        _products.value = products
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al cargar productos: ${exception.message}"
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
     * Abrir una mesa
     */
    fun openTable(tableNumber: Int, waiterId: String, waiterName: String) {
        android.util.Log.d("TablesViewModel", "DEBUG: openTable llamado - Mesa: $tableNumber, Mesero: $waiterName, ID: $waiterId")
        
        // Validar parámetros
        if (waiterName.isBlank()) {
            android.util.Log.d("TablesViewModel", "DEBUG: Nombre del mesero está vacío")
            _errorMessage.value = "El nombre del mesero no puede estar vacío"
            return
        }
        
        // Mostrar mensaje de carga
        _isLoading.value = true
        _errorMessage.value = ""
        _successMessage.value = ""
        
        viewModelScope.launch {
            try {
                android.util.Log.d("TablesViewModel", "DEBUG: Creando objeto Table")
                val table = Table(
                    number = tableNumber,
                    status = TableStatus.OCCUPIED,
                    waiterId = waiterId,
                    waiterName = waiterName,
                    openTime = Date()
                )

                android.util.Log.d("TablesViewModel", "DEBUG: Llamando a tablesRepository.openTable")
                val result = tablesRepository.openTable(tableNumber, waiterId, waiterName)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("TablesViewModel", "DEBUG: Mesa abierta exitosamente")
                        _successMessage.value = "Mesa ${tableNumber} abierta exitosamente con mesero: $waiterName"
                        loadTables() // Recargar la lista
                        
                        // Limpiar el mensaje después de un momento
                        kotlinx.coroutines.delay(2000)
                        _successMessage.value = ""
                    },
                    onFailure = { exception ->
                        android.util.Log.e("TablesViewModel", "DEBUG: Error al abrir mesa: ${exception.message}")
                        _errorMessage.value = "Error al abrir mesa: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("TablesViewModel", "DEBUG: Error inesperado: ${e.message}")
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cerrar una mesa
     */
    fun closeTable(tableId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.closeTable(tableId)
                result.fold(
                    onSuccess = {
                        loadTables() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al cerrar mesa: ${exception.message}"
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
     * Agregar producto a una mesa
     */
    fun addProductToTable(tableId: String, product: Product, quantity: Int) {
        android.util.Log.d("TablesViewModel", "DEBUG: addProductToTable llamado - Mesa: $tableId, Producto: ${product.name}, Cantidad: $quantity")
        
        // Validar parámetros
        if (quantity <= 0) {
            _errorMessage.value = "La cantidad debe ser mayor a 0"
            return
        }
        
        if (quantity > product.quantity) {
            _errorMessage.value = "No hay suficiente stock disponible"
            return
        }
        
        // Mostrar mensaje de carga
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                android.util.Log.d("TablesViewModel", "DEBUG: Creando TableItem")
                val tableItem = TableItem(
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    quantity = quantity,
                    subtotal = product.price * quantity
                )

                android.util.Log.d("TablesViewModel", "DEBUG: Llamando a tablesRepository.addProductToTable")
                val result = tablesRepository.addProductToTable(tableId, product, quantity)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("TablesViewModel", "DEBUG: Producto agregado exitosamente")
                        _successMessage.value = "Producto agregado exitosamente"
                        
                        // Optimización: Actualizar la lista local sin recargar desde Firebase
                        val currentTables = _tables.value?.toMutableList() ?: mutableListOf()
                        val tableIndex = currentTables.indexOfFirst { it.id == tableId }
                        
                        if (tableIndex != -1) {
                            val updatedTable = currentTables[tableIndex].copy(
                                items = currentTables[tableIndex].items + tableItem,
                                totalAmount = currentTables[tableIndex].totalAmount + tableItem.subtotal
                            )
                            currentTables[tableIndex] = updatedTable
                            _tables.value = currentTables
                            android.util.Log.d("TablesViewModel", "DEBUG: Lista local actualizada")
                        } else {
                            // Si no encontramos la mesa en la lista local, recargar
                            loadTables()
                        }
                        
                        // Limpiar el mensaje después de un momento
                        kotlinx.coroutines.delay(1500)
                        _successMessage.value = ""
                    },
                    onFailure = { exception ->
                        android.util.Log.e("TablesViewModel", "DEBUG: Error al agregar producto: ${exception.message}")
                        _errorMessage.value = "Error al agregar producto: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("TablesViewModel", "DEBUG: Error inesperado: ${e.message}")
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Obtener mesa por ID
     */
    fun getTableById(tableId: String): Table? {
        return _tables.value?.find { it.id == tableId }
    }

    /**
     * Calcular total de una mesa
     */
    fun calculateTableTotal(table: Table): Double {
        return table.items.sumOf { it.subtotal } + table.tipAmount
    }

    /**
     * Obtener productos disponibles (con stock > 0)
     */
    fun getAvailableProducts(): List<Product> {
        return _products.value?.filter { it.quantity > 0 } ?: emptyList()
    }

    /**
     * Quitar productos de una mesa
     */
    fun removeProductsFromTable(tableId: String, itemsToRemove: List<TableItem>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.removeProductsFromTable(tableId, itemsToRemove)
                result.fold(
                    onSuccess = {
                        loadTables() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al quitar productos: ${exception.message}"
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
     * Actualizar cantidad de un producto en una mesa
     */
    fun updateProductQuantity(tableId: String, productId: String, newQuantity: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.updateProductQuantity(tableId, productId, newQuantity)
                result.fold(
                    onSuccess = {
                        loadTables() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al actualizar cantidad: ${exception.message}"
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
     * Remover un producto específico de una mesa
     */
    fun removeProductFromTable(tableId: String, productId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.removeProductFromTable(tableId, productId)
                result.fold(
                    onSuccess = {
                        loadTables() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al remover producto: ${exception.message}"
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
     * Limpiar mensajes
     */
    fun clearMessages() {
        _successMessage.value = ""
        _errorMessage.value = ""
    }

    /**
     * Actualizar propina de una mesa
     */
    fun updateTableTip(tableId: String, tipAmount: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = tablesRepository.updateTableTip(tableId, tipAmount)
                result.fold(
                    onSuccess = {
                        loadTables() // Recargar la lista
                        _errorMessage.value = ""
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al actualizar propina: ${exception.message}"
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
     * Cerrar mesa con propina
     */
    fun closeTableWithTip(tableId: String, tipAmount: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Primero actualizar la propina
                val tipResult = tablesRepository.updateTableTip(tableId, tipAmount)
                tipResult.fold(
                    onSuccess = {
                        // Luego cerrar la mesa
                        val closeResult = tablesRepository.closeTable(tableId)
                        closeResult.fold(
                            onSuccess = {
                                loadTables() // Recargar la lista
                                _errorMessage.value = ""
                            },
                            onFailure = { exception ->
                                _errorMessage.value = "Error al cerrar mesa: ${exception.message}"
                            }
                        )
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al actualizar propina: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 