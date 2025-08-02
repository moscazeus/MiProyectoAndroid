package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.Table
import com.caferaquelita.restauranteapp.models.TableStatus
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para mostrar la lista de mesas.
 */
class TableAdapter(
    private var tables: List<Table>,
    private val onOpenTable: (Table) -> Unit,
    private val onAddProduct: (Table) -> Unit,
    private val onCloseTable: (Table) -> Unit
) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTableNumber: TextView = itemView.findViewById(R.id.textViewTableNumber)
        val textViewTableStatus: TextView = itemView.findViewById(R.id.textViewTableStatus)
        val textViewWaiter: TextView = itemView.findViewById(R.id.textViewWaiter)
        val textViewItems: TextView = itemView.findViewById(R.id.textViewItems)
        val textViewTotal: TextView = itemView.findViewById(R.id.textViewTotal)
        val buttonOpenTable: Button = itemView.findViewById(R.id.buttonOpenTable)
        val buttonAddProduct: Button = itemView.findViewById(R.id.buttonAddProduct)
        val buttonCloseTable: Button = itemView.findViewById(R.id.buttonCloseTable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val table = tables[position]
        
        // Configurar número de mesa
        holder.textViewTableNumber.text = "Mesa ${table.number}"
        
        // Configurar estado de la mesa
        when (table.status) {
            TableStatus.FREE -> {
                holder.textViewTableStatus.text = "LIBRE"
                holder.textViewTableStatus.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_green_light)
                )
                holder.buttonOpenTable.visibility = View.VISIBLE
                holder.buttonAddProduct.visibility = View.GONE
                holder.buttonCloseTable.visibility = View.GONE
            }
            TableStatus.OCCUPIED -> {
                holder.textViewTableStatus.text = "OCUPADA"
                holder.textViewTableStatus.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_orange_dark)
                )
                holder.buttonOpenTable.visibility = View.GONE
                holder.buttonAddProduct.visibility = View.VISIBLE
                holder.buttonCloseTable.visibility = View.VISIBLE
            }
            TableStatus.RESERVED -> {
                holder.textViewTableStatus.text = "RESERVADA"
                holder.textViewTableStatus.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_blue_dark)
                )
                holder.buttonOpenTable.visibility = View.GONE
                holder.buttonAddProduct.visibility = View.GONE
                holder.buttonCloseTable.visibility = View.GONE
            }
        }
        
        // Configurar información del mesero
        if (table.waiterName.isNotEmpty()) {
            holder.textViewWaiter.text = "Mesero: ${table.waiterName}"
        } else {
            holder.textViewWaiter.text = "Mesero: Sin asignar"
        }
        
        // Configurar información de productos
        holder.textViewItems.text = "Productos: ${table.items.size}"
        
        // Configurar total
        val total = table.items.sumOf { it.subtotal } + table.tipAmount
        holder.textViewTotal.text = "Total: ${numberFormat.format(total)}"
        
        // Configurar botones
        holder.buttonOpenTable.setOnClickListener {
            android.util.Log.d("TableAdapter", "DEBUG: Botón Abrir Mesa presionado para mesa ${table.number}")
            onOpenTable(table)
        }
        
        holder.buttonAddProduct.setOnClickListener {
            onAddProduct(table)
        }
        
        holder.buttonCloseTable.setOnClickListener {
            onCloseTable(table)
        }
    }

    override fun getItemCount(): Int = tables.size

    fun updateTables(newTables: List<Table>) {
        tables = newTables
        notifyDataSetChanged()
    }
} 