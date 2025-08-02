package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Repositorio para manejar las operaciones de inventario con Firebase Firestore.
 */
class InventoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    /**
     * Obtener todos los productos ordenados por nombre
     */
    suspend fun getProducts(): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val products = snapshot.documents.mapNotNull { document ->
                document.toObject(Product::class.java)?.copy(id = document.id)
            }
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Agregar un nuevo producto
     */
    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            val productData = product.copy(
                id = "", // Firebase generará el ID automáticamente
                name = product.name.trim(),
                category = product.category.trim()
            )
            
            productsCollection.add(productData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar un producto existente
     */
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            if (product.id.isBlank()) {
                return Result.failure(Exception("ID de producto requerido"))
            }
            
            val productData = product.copy(
                name = product.name.trim(),
                category = product.category.trim()
            )
            
            productsCollection.document(product.id).set(productData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar un producto
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productsCollection.document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buscar productos por nombre o categoría
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + '\uf8ff')
                .get()
                .await()
            
            val products = snapshot.documents.mapNotNull { document ->
                document.toObject(Product::class.java)?.copy(id = document.id)
            }
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener productos con stock bajo
     */
    suspend fun getLowStockProducts(): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereLessThanOrEqualTo("quantity", "minQuantity")
                .get()
                .await()
            
            val products = snapshot.documents.mapNotNull { document ->
                document.toObject(Product::class.java)?.copy(id = document.id)
            }
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar cantidad de un producto (para ventas)
     */
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Result<Unit> {
        return try {
            productsCollection.document(productId)
                .update("quantity", newQuantity)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 