package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.TableItem
import java.text.NumberFormat
import java.util.*

class TableProductManagementAdapter(
    private var items: List<TableItem>,
    private val onQuantityChange: (TableItem, Int) -> Unit,
    private val onRemoveProduct: (TableItem) -> Unit
) : RecyclerView.Adapter<TableProductManagementAdapter.ViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProductName: TextView = view.findViewById(R.id.textViewProductName)
        val textViewProductPrice: TextView = view.findViewById(R.id.textViewProductPrice)
        val textViewSubtotal: TextView = view.findViewById(R.id.textViewSubtotal)
        val textViewQuantity: TextView = view.findViewById(R.id.textViewQuantity)
        val buttonMinus: Button = view.findViewById(R.id.buttonMinus)
        val buttonPlus: Button = view.findViewById(R.id.buttonPlus)
        val buttonRemove: Button = view.findViewById(R.id.buttonRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_product_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.textViewProductName.text = item.productName
        holder.textViewProductPrice.text = "Precio: ${numberFormat.format(item.productPrice)}"
        holder.textViewQuantity.text = item.quantity.toString()
        holder.textViewSubtotal.text = "Subtotal: ${numberFormat.format(item.subtotal)}"

        holder.buttonMinus.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChange(item, item.quantity - 1)
            }
        }

        holder.buttonPlus.setOnClickListener {
            onQuantityChange(item, item.quantity + 1)
        }

        holder.buttonRemove.setOnClickListener {
            onRemoveProduct(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<TableItem>) {
        items = newItems
        notifyDataSetChanged()
    }
} 