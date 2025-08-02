package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.Invoice
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar el historial de facturas.
 */
class InvoiceHistoryAdapter(
    private var invoices: List<Invoice>,
    private val onInvoiceClick: (Invoice) -> Unit
) : RecyclerView.Adapter<InvoiceHistoryAdapter.InvoiceHistoryViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    class InvoiceHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewInvoiceNumber: TextView = itemView.findViewById(R.id.textViewInvoiceNumber)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewTableNumber: TextView = itemView.findViewById(R.id.textViewTableNumber)
        val textViewWaiterName: TextView = itemView.findViewById(R.id.textViewWaiterName)
        val textViewTotal: TextView = itemView.findViewById(R.id.textViewTotal)
        val textViewProductsSummary: TextView = itemView.findViewById(R.id.textViewProductsSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_history, parent, false)
        return InvoiceHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvoiceHistoryViewHolder, position: Int) {
        val invoice = invoices[position]
        
        holder.textViewInvoiceNumber.text = "Factura #${invoice.invoiceNumber}"
        holder.textViewDate.text = "Fecha: ${dateFormat.format(Date(invoice.createdAt))}"
        holder.textViewTableNumber.text = "Mesa: ${invoice.tableNumber}"
        holder.textViewWaiterName.text = "Mesero: ${invoice.waiterName}"
        holder.textViewTotal.text = "Total: ${numberFormat.format(invoice.total)}"
        
        // Mostrar resumen de productos
        if (invoice.items.isNotEmpty()) {
            val productSummary = invoice.items.take(3).joinToString(", ") { 
                "${it.quantity}x ${it.productName}" 
            }
            val remainingCount = invoice.items.size - 3
            val summary = if (remainingCount > 0) {
                "$productSummary + $remainingCount más"
            } else {
                productSummary
            }
            holder.textViewProductsSummary.text = "Productos: $summary"
            holder.textViewProductsSummary.visibility = View.VISIBLE
        } else {
            holder.textViewProductsSummary.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onInvoiceClick(invoice)
        }
    }

    override fun getItemCount(): Int = invoices.size

    fun updateInvoices(newInvoices: List<Invoice>) {
        invoices = newInvoices
        notifyDataSetChanged()
    }
} 