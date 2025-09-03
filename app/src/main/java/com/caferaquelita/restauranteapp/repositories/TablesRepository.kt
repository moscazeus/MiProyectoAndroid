package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.Product
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableItem
import com.caferaquelita.restauranteapp.models.TableStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Repositorio para manejar las operaciones de mesas con Firebase Firestore.
 */
class TablesRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tablesCollection = db.collection("tables")

    /**
     * Obtener todas las mesas ordenadas por número
     */
    suspend fun getTables(): Result<List<Table>> {
        return try {
            val snapshot = tablesCollection
                .orderBy("number", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val tables = snapshot.documents.mapNotNull { document ->
                document.toObject(Table::class.java)?.copy(id = document.id)
            }
            
            // Si no hay mesas, crear las mesas por defecto (1-10)
            if (tables.isEmpty()) {
                createDefaultTables()
                return getTables() // Recursivamente obtener las mesas creadas
            }
            
            Result.success(tables)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear mesas por defecto (1-10)
     */
    private suspend fun createDefaultTables() {
        try {
            for (i in 1..10) {
                val table = Table(
                    number = i,
                    status = TableStatus.FREE
                )
                tablesCollection.add(table).await()
            }
        } catch (e: Exception) {
            // Ignorar errores al crear mesas por defecto
        }
    }

    // --- Helper: total = suma subtotales + propina ---
    private fun calcTotal(items: List<TableItem>, tip: Double): Double {
        return items.sumOf { it.subtotal } + tip
    }


    /**
     * Abrir una mesa
     */
    suspend fun openTable(tableNumber: Int, waiterId: String, waiterName: String): Result<Table> {
        return try {
            // Buscar la mesa por número
            val existingTable = tablesCollection
                .whereEqualTo("number", tableNumber)
                .get()
                .await()
                .documents
                .firstOrNull()
            
            if (existingTable != null) {
                // Actualizar mesa existente
                val table = Table(
                    id = existingTable.id,
                    number = tableNumber,
                    status = TableStatus.OCCUPIED,
                    waiterId = waiterId,
                    waiterName = waiterName,
                    openTime = Date(),
                    totalAmount = 0.0,
                    tipAmount = 0.0,
                    items = emptyList()
                )
                
                existingTable.reference.update(
                    mapOf(
                        "status" to table.status.name,
                        "waiterId" to table.waiterId,
                        "waiterName" to table.waiterName,
                        "openTime" to table.openTime,
                        "totalAmount" to table.totalAmount,
                        "tipAmount" to table.tipAmount,
                        "items" to table.items
                    )
                ).await()
                
                Result.success(table)
            } else {
                Result.failure(Exception("Mesa no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Agregar producto a una mesa
     */
    suspend fun addProductToTable(tableId: String, product: Product, quantity: Int): Result<Table> {
        return try {
            val tableDoc = tablesCollection.document(tableId).get().await()
            if (!tableDoc.exists()) {
                return Result.failure(Exception("Mesa no encontrada"))
            }

            val table = tableDoc.toObject(Table::class.java)?.copy(id = tableDoc.id)
                ?: return Result.failure(Exception("Error al obtener datos de la mesa"))

            // Crear el item del producto
            val tableItem = TableItem(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = quantity,
                subtotal = product.price * quantity
            )

            // Agregar o incrementar producto si ya existe
            val updatedItems = table.items.toMutableList()
            val idx = updatedItems.indexOfFirst { it.productId == product.id }
            if (idx >= 0) {
                val current = updatedItems[idx]
                val newQty = current.quantity + quantity
                updatedItems[idx] = current.copy(
                    quantity = newQty,
                    subtotal = current.productPrice * newQty
                )
            } else {
                updatedItems.add(tableItem)
            }


            // Calcular nuevo total
            val newTotal = calcTotal(updatedItems, table.tipAmount)


            // Actualizar la mesa
            tableDoc.reference.update(
                mapOf(
                    "items" to updatedItems,
                    "totalAmount" to newTotal
                )
            ).await()

            Result.success(table.copy(items = updatedItems, totalAmount = newTotal))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar cantidad de un producto en una mesa
     */
    suspend fun updateProductQuantity(tableId: String, productId: String, newQuantity: Int): Result<Table> {
        return try {
            val tableDoc = tablesCollection.document(tableId).get().await()
            if (!tableDoc.exists()) {
                return Result.failure(Exception("Mesa no encontrada"))
            }

            val table = tableDoc.toObject(Table::class.java)?.copy(id = tableDoc.id)
                ?: return Result.failure(Exception("Error al obtener datos de la mesa"))

            // Encontrar y actualizar el producto
            val updatedItems = table.items.map { item ->
                if (item.productId == productId) {
                    item.copy(
                        quantity = newQuantity,
                        subtotal = item.productPrice * newQuantity
                    )
                } else {
                    item
                }
            }

            // Calcular nuevo total
            val newTotal = calcTotal(updatedItems, table.tipAmount)


            // Actualizar la mesa
            tableDoc.reference.update(
                mapOf(
                    "items" to updatedItems,
                    "totalAmount" to newTotal
                )
            ).await()

            Result.success(table.copy(items = updatedItems, totalAmount = newTotal))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /**
     * Quitar productos de una mesa
     */
    suspend fun removeProductsFromTable(tableId: String, itemsToRemove: List<TableItem>): Result<Table> {
        return try {
            val tableDoc = tablesCollection.document(tableId).get().await()
            if (!tableDoc.exists()) {
                return Result.failure(Exception("Mesa no encontrada"))
            }

            val table = tableDoc.toObject(Table::class.java)?.copy(id = tableDoc.id)
                ?: return Result.failure(Exception("Error al obtener datos de la mesa"))

            // Filtrar los productos a quitar
            val productIdsToRemove = itemsToRemove.map { it.productId }.toSet()
            val updatedItems = table.items.filter { it.productId !in productIdsToRemove }

            // Calcular nuevo total
            val newTotal = calcTotal(updatedItems, table.tipAmount)


            // Actualizar la mesa
            tableDoc.reference.update(
                mapOf(
                    "items" to updatedItems,
                    "totalAmount" to newTotal
                )
            ).await()

            Result.success(table.copy(items = updatedItems, totalAmount = newTotal))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quitar producto de una mesa
     */
    suspend fun removeProductFromTable(tableId: String, productId: String): Result<Table> {
        return try {
            val tableDoc = tablesCollection.document(tableId).get().await()
            if (!tableDoc.exists()) {
                return Result.failure(Exception("Mesa no encontrada"))
            }

            val table = tableDoc.toObject(Table::class.java)?.copy(id = tableDoc.id)
                ?: return Result.failure(Exception("Error al obtener datos de la mesa"))

            // Filtrar el producto a quitar
            val updatedItems = table.items.filter { it.productId != productId }

            // Calcular nuevo total
            val newTotal = calcTotal(updatedItems, table.tipAmount)


            // Actualizar la mesa
            tableDoc.reference.update(
                mapOf(
                    "items" to updatedItems,
                    "totalAmount" to newTotal
                )
            ).await()

            Result.success(table.copy(items = updatedItems, totalAmount = newTotal))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerrar una mesa
     */
    /**
     * Cerrar una mesa con verificación sobre datos frescos
     */
    suspend fun closeTable(tableId: String): Result<Table> {
        return try {
            val fresh = getTableFresh(tableId)
                ?: return Result.failure(Exception("Mesa no encontrada"))

            // Si no hay productos, devolvemos error y NO cerramos.
            if (fresh.items.isEmpty()) {
                return Result.failure(IllegalStateException("EMPTY_ITEMS"))
            }

            // Actualizar estado de la mesa
            tablesCollection.document(tableId).update(
                mapOf(
                    "status" to TableStatus.FREE.name,
                    "waiterId" to "",
                    "waiterName" to "",
                    "openTime" to null,
                    "totalAmount" to 0.0,
                    "tipAmount" to 0.0,
                    "items" to emptyList<TableItem>()
                )
            ).await()

            Result.success(
                fresh.copy(
                    status = TableStatus.FREE,
                    waiterId = "",
                    waiterName = "",
                    openTime = null,
                    totalAmount = 0.0,
                    tipAmount = 0.0,
                    items = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Obtener mesa por ID
     */
    suspend fun getTableById(tableId: String): Result<Table> {
        return try {
            val tableDoc = tablesCollection.document(tableId).get().await()
            if (!tableDoc.exists()) {
                return Result.failure(Exception("Mesa no encontrada"))
            }

            val table = tableDoc.toObject(Table::class.java)?.copy(id = tableDoc.id)
                ?: return Result.failure(Exception("Error al obtener datos de la mesa"))

            Result.success(table)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar propina de una mesa
     */
    suspend fun updateTableTip(tableId: String, tipAmount: Double): Result<Table> {
        return try {
            val tableRef = db.collection("tables").document(tableId)
            val table = getTableById(tableId).getOrNull()
            val updateData = mapOf(
                "tipAmount" to tipAmount,
                "totalAmount" to if (table != null) calcTotal(table.items, tipAmount) else tipAmount
            )


            tableRef.update(updateData).await()
            getTableById(tableId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Lee la mesa DIRECTO de Firestore y devuelve el objeto con el id copiado
    suspend fun getTableFresh(tableId: String): Table? {
        val doc = tablesCollection.document(tableId).get().await()
        if (!doc.exists()) return null
        return doc.toObject(Table::class.java)?.copy(id = doc.id)
    }

} 