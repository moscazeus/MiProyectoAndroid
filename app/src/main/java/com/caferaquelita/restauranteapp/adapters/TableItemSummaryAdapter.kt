package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.TableItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador simple para mostrar los productos en el diálogo de cierre de mesa.
 */
class TableItemSummaryAdapter(
    private var tableItems: List<TableItem>
) : RecyclerView.Adapter<TableItemSummaryAdapter.TableItemSummaryViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    class TableItemSummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        val textViewProductQuantity: TextView = itemView.findViewById(R.id.textViewProductQuantity)
        val textViewSubtotal: TextView = itemView.findViewById(R.id.textViewSubtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableItemSummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_product, parent, false)
        return TableItemSummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableItemSummaryViewHolder, position: Int) {
        val tableItem = tableItems[position]
        
        // Configurar información del producto
        holder.textViewProductName.text = tableItem.productName
        holder.textViewProductQuantity.text = "Cantidad: ${tableItem.quantity}"
        holder.textViewSubtotal.text = numberFormat.format(tableItem.subtotal)
        
        // Ocultar el checkbox ya que es solo para mostrar
        holder.itemView.findViewById<View>(R.id.checkBoxRemove).visibility = View.GONE
    }

    override fun getItemCount(): Int = tableItems.size

    fun updateTableItems(newTableItems: List<TableItem>) {
        tableItems = newTableItems
        notifyDataSetChanged()
    }
} 