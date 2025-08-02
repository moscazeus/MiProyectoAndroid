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
 * Adaptador para mostrar los items de una factura.
 */
class InvoiceItemAdapter(
    private var items: List<TableItem>
) : RecyclerView.Adapter<InvoiceItemAdapter.InvoiceItemViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    class InvoiceItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        val textViewUnitPrice: TextView = itemView.findViewById(R.id.textViewUnitPrice)
        val textViewSubtotal: TextView = itemView.findViewById(R.id.textViewSubtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_product, parent, false)
        return InvoiceItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvoiceItemViewHolder, position: Int) {
        val item = items[position]
        
        holder.textViewProductName.text = item.productName
        holder.textViewQuantity.text = "x${item.quantity}"
        holder.textViewUnitPrice.text = numberFormat.format(item.productPrice)
        holder.textViewSubtotal.text = numberFormat.format(item.subtotal)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TableItem>) {
        items = newItems
        notifyDataSetChanged()
    }
} 