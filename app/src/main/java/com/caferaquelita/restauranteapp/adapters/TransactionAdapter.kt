package com.caferaquelita.restauranteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.FinancialTransaction
import com.caferaquelita.restauranteapp.models.TransactionCategory
import com.caferaquelita.restauranteapp.models.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar transacciones financieras en un RecyclerView.
 */
class TransactionAdapter(
    private var transactions: List<FinancialTransaction>,
    private val onTransactionClick: (FinancialTransaction) -> Unit,
    private val onTransactionEdit: (FinancialTransaction) -> Unit,
    private val onTransactionDelete: (FinancialTransaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View = itemView.findViewById(R.id.cardViewTransaction)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
        val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewPaymentMethod: TextView = itemView.findViewById(R.id.textViewPaymentMethod)
        val imageViewType: View = itemView.findViewById(R.id.imageViewType)
        val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        holder.textViewDescription.text = transaction.description
        holder.textViewAmount.text = numberFormat.format(transaction.amount)
        holder.textViewCategory.text = getCategoryDisplayName(transaction.category)
        holder.textViewDate.text = dateFormat.format(transaction.date)
        holder.textViewPaymentMethod.text = getPaymentMethodDisplayName(transaction.paymentMethod.name)
        
        // Configurar color según el tipo de transacción
        val amountColor = if (transaction.type == TransactionType.INCOME) {
            holder.itemView.context.getResources().getColor(android.R.color.holo_green_dark, null)
        } else {
            holder.itemView.context.getResources().getColor(android.R.color.holo_red_dark, null)
        }
        holder.textViewAmount.setTextColor(amountColor)
        
        // Configurar icono según el tipo
        val iconColor = if (transaction.type == TransactionType.INCOME) {
            holder.itemView.context.getResources().getColor(android.R.color.holo_green_dark, null)
        } else {
            holder.itemView.context.getResources().getColor(android.R.color.holo_red_dark, null)
        }
        holder.imageViewType.setBackgroundColor(iconColor)
        
        // Configurar listeners
        holder.cardView.setOnClickListener {
            onTransactionClick(transaction)
        }
        
        holder.buttonEdit.setOnClickListener {
            onTransactionEdit(transaction)
        }
        
        holder.buttonDelete.setOnClickListener {
            onTransactionDelete(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<FinancialTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    private fun getCategoryDisplayName(category: TransactionCategory): String {
        return when (category) {
            TransactionCategory.SALES -> "Ventas"
            TransactionCategory.DELIVERY -> "Delivery"
            TransactionCategory.CATERING -> "Catering"
            TransactionCategory.EVENTS -> "Eventos"
            TransactionCategory.OTHER_INCOME -> "Otros Ingresos"
            TransactionCategory.INVENTORY -> "Inventario"
            TransactionCategory.SUPPLIES -> "Suministros"
            TransactionCategory.UTILITIES -> "Servicios Públicos"
            TransactionCategory.RENT -> "Alquiler"
            TransactionCategory.SALARY -> "Salarios"
            TransactionCategory.MAINTENANCE -> "Mantenimiento"
            TransactionCategory.MARKETING -> "Marketing"
            TransactionCategory.INSURANCE -> "Seguros"
            TransactionCategory.TAXES -> "Impuestos"
            TransactionCategory.OTHER_EXPENSE -> "Otros Gastos"
        }
    }

    private fun getPaymentMethodDisplayName(paymentMethod: String): String {
        return when (paymentMethod) {
            "CASH" -> "Efectivo"
            "CARD" -> "Tarjeta"
            "TRANSFER" -> "Transferencia"
            "CHECK" -> "Cheque"
            "DIGITAL" -> "Digital"
            else -> paymentMethod
        }
    }
} 