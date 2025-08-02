package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.*
import com.caferaquelita.restauranteapp.viewmodels.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para el control de caja y gestión de efectivo.
 */
class CashRegisterActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    
    // Métricas de caja
    private lateinit var textViewOpeningAmount: TextView
    private lateinit var textViewCurrentAmount: TextView
    
    // Botones de acción
    private lateinit var buttonAddWithdrawal: Button
    private lateinit var buttonAddDeposit: Button
    private lateinit var buttonViewTransactions: Button
    
    // Estado de caja
    private lateinit var cardViewCashStatus: CardView
    private lateinit var textViewCashStatus: TextView
    
    private val viewModel: DashboardViewModel by viewModels()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    
    private var isCashRegisterOpen = false
    private var openingAmount = 0.0
    private var currentAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_register)

        setupViews()
        setupToolbar()
        setupUI()
        loadCashRegisterData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        
        // Métricas de caja
        textViewOpeningAmount = findViewById(R.id.textViewOpeningAmount)
        textViewCurrentAmount = findViewById(R.id.textViewCurrentAmount)
        
        // Botones de acción
        buttonAddWithdrawal = findViewById(R.id.buttonAddWithdrawal)
        buttonAddDeposit = findViewById(R.id.buttonAddDeposit)
        buttonViewTransactions = findViewById(R.id.buttonViewTransactions)
        
        // Estado de caja
        cardViewCashStatus = findViewById(R.id.cardViewCashStatus)
        textViewCashStatus = findViewById(R.id.textViewCashStatus)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Control de Caja"
        }
    }

    private fun setupUI() {
        buttonAddWithdrawal.setOnClickListener {
            showAddWithdrawalDialog()
        }
        
        buttonAddDeposit.setOnClickListener {
            showAddDepositDialog()
        }
        
        buttonViewTransactions.setOnClickListener {
            // Navegar a la actividad de transacciones
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCashRegisterData() {
        // Por ahora cargar datos de ejemplo
        updateCashRegisterDisplay()
    }

    private fun updateCashRegisterDisplay() {
        textViewOpeningAmount.text = "Monto inicial: ${numberFormat.format(openingAmount)}"
        textViewCurrentAmount.text = "Monto actual: ${numberFormat.format(currentAmount)}"
        
        // Actualizar estado de caja
        updateCashStatus()
    }

    private fun updateCashStatus() {
        if (isCashRegisterOpen) {
            textViewCashStatus.text = "CAJA ABIERTA"
            textViewCashStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null))
            cardViewCashStatus.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null))
            
            buttonAddWithdrawal.visibility = View.VISIBLE
            buttonAddDeposit.visibility = View.VISIBLE
        } else {
            textViewCashStatus.text = "CAJA CERRADA"
            textViewCashStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null))
            cardViewCashStatus.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null))
            
            buttonAddWithdrawal.visibility = View.GONE
            buttonAddDeposit.visibility = View.GONE
        }
    }

    private fun showOpenCashRegisterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_open_cash_register, null)
        val editTextOpeningAmount = dialogView.findViewById<EditText>(R.id.editTextOpeningAmount)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Abrir Caja")
            .setMessage("Ingresa el monto inicial de la caja:")
            .setView(dialogView)
            .setPositiveButton("Abrir Caja", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            positiveButton.setOnClickListener {
                val amount = editTextOpeningAmount.text.toString().toDoubleOrNull()
                
                if (amount != null && amount >= 0) {
                    openingAmount = amount
                    currentAmount = amount
                    isCashRegisterOpen = true
                    
                    // Crear transacción de apertura
                    val transaction = FinancialTransaction(
                        amount = amount,
                        type = TransactionType.INCOME,
                        category = TransactionCategory.OTHER_INCOME,
                        description = "Apertura de caja",
                        paymentMethod = PaymentMethod.CASH,
                        notes = "Monto inicial de caja"
                    )
                    viewModel.addTransaction(transaction)
                    
                    updateCashRegisterDisplay()
                    dialog.dismiss()
                    
                    Toast.makeText(this, "Caja abierta con $${numberFormat.format(amount)}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show()
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showCloseCashRegisterDialog() {
        val totalIncome = currentAmount - openingAmount
        
        val message = """
            Resumen de Caja:
            
            Monto inicial: ${numberFormat.format(openingAmount)}
            Monto actual: ${numberFormat.format(currentAmount)}
            Ingresos totales: ${numberFormat.format(totalIncome)}
            
            ¿Estás seguro de que quieres cerrar la caja?
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Cerrar Caja")
            .setMessage(message)
            .setPositiveButton("Cerrar Caja") { _, _ ->
                isCashRegisterOpen = false
                
                // Crear transacción de cierre
                val transaction = FinancialTransaction(
                    amount = currentAmount,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.OTHER_INCOME,
                    description = "Cierre de caja",
                    paymentMethod = PaymentMethod.CASH,
                    notes = "Monto final de caja: ${numberFormat.format(currentAmount)}"
                )
                viewModel.addTransaction(transaction)
                
                updateCashRegisterDisplay()
                Toast.makeText(this, "Caja cerrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddWithdrawalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cash_movement, null)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextMovementAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMovementDescription)
        
        editTextDescription.hint = "Motivo del retiro"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Retiro")
            .setView(dialogView)
            .setPositiveButton("Agregar Retiro", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()
                
                                    if (amount != null && amount > 0.0 && description.isNotEmpty()) {
                        if (amount <= currentAmount) {
                            currentAmount = currentAmount - amount
                        
                        // Crear transacción de retiro
                        val transaction = FinancialTransaction(
                            amount = amount,
                            type = TransactionType.EXPENSE,
                            category = TransactionCategory.OTHER_EXPENSE,
                            description = "Retiro: $description",
                            paymentMethod = PaymentMethod.CASH,
                            notes = "Retiro de caja"
                        )
                        viewModel.addTransaction(transaction)
                        
                        updateCashRegisterDisplay()
                        dialog.dismiss()
                        
                        Toast.makeText(this, "Retiro de ${numberFormat.format(amount.toDouble())} agregado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No hay suficiente efectivo en caja", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showAddDepositDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cash_movement, null)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextMovementAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMovementDescription)
        
        editTextDescription.hint = "Motivo del depósito"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Depósito")
            .setView(dialogView)
            .setPositiveButton("Agregar Depósito", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            negativeButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null))
            negativeButton.setTextColor(getResources().getColor(android.R.color.white, null))
            
            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()
                
                if (amount != null && amount > 0.0 && description.isNotEmpty()) {
                    currentAmount = currentAmount + amount
                    
                    // Crear transacción de depósito
                    val transaction = FinancialTransaction(
                        amount = amount,
                        type = TransactionType.INCOME,
                        category = TransactionCategory.OTHER_INCOME,
                        description = "Depósito: $description",
                        paymentMethod = PaymentMethod.CASH,
                        notes = "Depósito a caja"
                    )
                    viewModel.addTransaction(transaction)
                    
                    updateCashRegisterDisplay()
                    dialog.dismiss()
                    
                    Toast.makeText(this, "Depósito de ${numberFormat.format(amount.toDouble())} agregado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 