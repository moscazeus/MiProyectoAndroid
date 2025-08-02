package com.caferaquelita.restauranteapp.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.Product
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para mostrar la lista de productos en el inventario.
 */
class ProductAdapter(
    private var products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProduct: ImageView = itemView.findViewById(R.id.imageViewProduct)
        val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        val textViewProductPrice: TextView = itemView.findViewById(R.id.textViewProductPrice)
        val textViewProductCategory: TextView = itemView.findViewById(R.id.textViewProductCategory)
        val textViewProductQuantity: TextView = itemView.findViewById(R.id.textViewProductQuantity)
        val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        
        holder.textViewProductName.text = product.name
        holder.textViewProductPrice.text = numberFormat.format(product.price)
        holder.textViewProductCategory.text = product.category
        holder.textViewProductQuantity.text = "Cantidad: ${product.quantity}"
        
        // Configurar color de cantidad según stock
        if (product.quantity <= product.minQuantity) {
            holder.textViewProductQuantity.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
        } else {
            holder.textViewProductQuantity.setTextColor(
                holder.itemView.context.getColor(android.R.color.black)
            )
        }
        
        // Cargar imagen del producto
        loadProductImage(holder.imageViewProduct, product.imageUrl)
        
        holder.buttonEdit.setOnClickListener {
            onEditClick(product)
        }
        
        holder.buttonDelete.setOnClickListener {
            onDeleteClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun filterProducts(query: String) {
        val filteredList = if (query.isEmpty()) {
            products
        } else {
            products.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)
            }
        }
        updateProducts(filteredList)
    }

    private fun loadProductImage(imageView: ImageView, imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            // TODO: Implementar carga de imagen desde Firebase Storage
            // Por ahora usamos una imagen por defecto
            imageView.setImageResource(android.R.drawable.ic_menu_camera)
        } else {
            // Imagen por defecto
            imageView.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }
} 