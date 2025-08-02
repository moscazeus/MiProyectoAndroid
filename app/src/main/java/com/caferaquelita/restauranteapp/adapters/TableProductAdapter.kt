package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.TableItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para mostrar los productos de una mesa con opción de quitar cantidades específicas.
 */
class TableProductAdapter(
    private var tableItems: List<TableItem>,
    private val onItemCheckedChanged: (TableItem, Boolean) -> Unit
) : RecyclerView.Adapter<TableProductAdapter.TableProductViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    // Mapa para almacenar la cantidad a quitar de cada producto
    private val quantitiesToRemove = mutableMapOf<TableItem, Int>()

    class TableProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxRemove: CheckBox = itemView.findViewById(R.id.checkBoxRemove)
        val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        val textViewProductQuantity: TextView = itemView.findViewById(R.id.textViewProductQuantity)
        val textViewProductPrice: TextView = itemView.findViewById(R.id.textViewProductPrice)
        val textViewSubtotal: TextView = itemView.findViewById(R.id.textViewSubtotal)
        val linearLayoutQuantityControls: LinearLayout = itemView.findViewById(R.id.linearLayoutQuantityControls)
        val buttonMinus: Button = itemView.findViewById(R.id.buttonMinus)
        val buttonPlus: Button = itemView.findViewById(R.id.buttonPlus)
        val textViewQuantityToRemove: TextView = itemView.findViewById(R.id.textViewQuantityToRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_product, parent, false)
        return TableProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableProductViewHolder, position: Int) {
        val tableItem = tableItems[position]
        
        // Configurar información del producto
        holder.textViewProductName.text = tableItem.productName
        holder.textViewProductQuantity.text = "Cantidad: ${tableItem.quantity}"
        holder.textViewProductPrice.text = "Precio: ${numberFormat.format(tableItem.productPrice)}"
        holder.textViewSubtotal.text = numberFormat.format(tableItem.subtotal)
        
        // Obtener la cantidad actual a quitar (0 por defecto)
        val currentQuantityToRemove = quantitiesToRemove[tableItem] ?: 0
        holder.textViewQuantityToRemove.text = currentQuantityToRemove.toString()
        
        // Configurar checkbox
        holder.checkBoxRemove.isChecked = currentQuantityToRemove > 0
        holder.checkBoxRemove.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Si se selecciona, establecer cantidad a quitar como 1
                quantitiesToRemove[tableItem] = 1
                holder.textViewQuantityToRemove.text = "1"
                holder.linearLayoutQuantityControls.visibility = View.VISIBLE
            } else {
                // Si se deselecciona, establecer cantidad a quitar como 0
                quantitiesToRemove[tableItem] = 0
                holder.textViewQuantityToRemove.text = "0"
                holder.linearLayoutQuantityControls.visibility = View.GONE
            }
            onItemCheckedChanged(tableItem, isChecked)
        }
        
        // Configurar botones de cantidad
        holder.buttonMinus.setOnClickListener {
            val currentQuantity = quantitiesToRemove[tableItem] ?: 0
            if (currentQuantity > 0) {
                val newQuantity = currentQuantity - 1
                quantitiesToRemove[tableItem] = newQuantity
                holder.textViewQuantityToRemove.text = newQuantity.toString()
                
                // Si la cantidad llega a 0, desmarcar el checkbox
                if (newQuantity == 0) {
                    holder.checkBoxRemove.isChecked = false
                    holder.linearLayoutQuantityControls.visibility = View.GONE
                }
                
                onItemCheckedChanged(tableItem, newQuantity > 0)
            }
        }
        
        holder.buttonPlus.setOnClickListener {
            val currentQuantity = quantitiesToRemove[tableItem] ?: 0
            if (currentQuantity < tableItem.quantity) {
                val newQuantity = currentQuantity + 1
                quantitiesToRemove[tableItem] = newQuantity
                holder.textViewQuantityToRemove.text = newQuantity.toString()
                
                // Si es la primera vez que se selecciona, mostrar controles
                if (newQuantity == 1) {
                    holder.linearLayoutQuantityControls.visibility = View.VISIBLE
                }
                
                onItemCheckedChanged(tableItem, true)
            }
        }
        
        // Mostrar/ocultar controles de cantidad según el estado
        holder.linearLayoutQuantityControls.visibility = if (currentQuantityToRemove > 0) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = tableItems.size

    fun updateTableItems(newTableItems: List<TableItem>) {
        tableItems = newTableItems
        quantitiesToRemove.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<TableItem> {
        return quantitiesToRemove.filter { it.value > 0 }.map { (tableItem, quantityToRemove) ->
            // Crear un nuevo TableItem con la cantidad a quitar
            tableItem.copy(quantity = quantityToRemove, subtotal = tableItem.productPrice * quantityToRemove)
        }
    }

    fun clearSelection() {
        quantitiesToRemove.clear()
        notifyDataSetChanged()
    }
} 