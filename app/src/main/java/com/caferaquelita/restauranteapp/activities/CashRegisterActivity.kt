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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.caferaquelita.restauranteapp.viewmodels.CashRegisterViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder



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

    // Botones de apertura / cierre de caja
    private lateinit var buttonOpenCash: Button
    private lateinit var buttonCloseCash: Button
    private lateinit var cardViewCashStatus: CardView
    private lateinit var textViewCashStatus: TextView

    private val viewModel: DashboardViewModel by viewModels()

    // ViewModel para abrir/cerrar la caja en Firestore
    private val cashRegViewModel: CashRegisterViewModel by viewModels()

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    private var isCashRegisterOpen = false
    private var openingAmount = 0.0
    private var currentAmount = 0.0

    private lateinit var userId: String
    private var userRole: String = "waiter"   // valor seguro por defecto



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_register)

        // 1) Inicializar vistas, toolbar, UI y datos
        setupViews()
        setupToolbar()
        setupUI()
        loadCashRegisterData()
        // Escucha el estado real de la caja en Firestore
        FirebaseFirestore.getInstance()
            .collection("cash").document("status")
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    isCashRegisterOpen = snap.getBoolean("isOpen") ?: false
                    openingAmount      = snap.getDouble("initial") ?: 0.0
                    currentAmount      = snap.getDouble("current") ?: openingAmount
                } else {
                    isCashRegisterOpen = false
                    openingAmount = 0.0
                    currentAmount = 0.0
                }
                updateOpenCloseButtons()

                updateCashRegisterDisplay()
            }




        // 2) Obtener el ID de usuario y cargar rol si existe
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            loadUserRole(userId)
        }

        // 3) Observar resultado de apertura (solo para los Toast)
        cashRegViewModel.openResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Caja abierta en servidor exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al abrir caja en servidor", Toast.LENGTH_LONG).show()
            }
        }

        // 4) Listeners de Abrir / Cerrar caja
        buttonOpenCash.setOnClickListener {
            if (userRole == "admin") {
                showOpenCashRegisterDialog()
            } else {
                Toast.makeText(this, "Solo un administrador puede abrir la caja", Toast.LENGTH_LONG).show()
            }
        }

        buttonCloseCash.setOnClickListener {
            if (userRole == "admin") {
                showCloseCashRegisterDialog()
            } else {
                Toast.makeText(this, "Solo un administrador puede cerrar la caja", Toast.LENGTH_LONG).show()
            }
        }


        // 5) Mostrar u ocultar botones Abrir/Cerrar según estado
        updateOpenCloseButtons()
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
        // Abrir / cerrar caja
        buttonOpenCash  = findViewById(R.id.buttonOpenCash)
        buttonCloseCash = findViewById(R.id.buttonCloseCash)
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
        if (userRole != "admin") {
            Toast.makeText(this, "Solo un administrador puede abrir la caja", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_open_cash_register, null)
        val editTextOpeningAmount = dialogView.findViewById<EditText>(R.id.editTextOpeningAmount)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Abrir Caja")
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
                val amount = editTextOpeningAmount.text.toString().replace(",", ".").toDoubleOrNull()

                if (amount != null && amount >= 0) {
                    val db = FirebaseFirestore.getInstance()
                    val statusRef = db.collection("cash").document("status")

                    db.runTransaction { tx ->
                        val snap = tx.get(statusRef)
                        val isOpen = snap.getBoolean("isOpen") ?: false
                        if (isOpen) {
                            throw IllegalStateException("La caja ya está abierta")
                        }

                        val data = hashMapOf(
                            "isOpen" to true,
                            "initial" to amount,
                            "current" to amount,
                            "openedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "openedBy" to FirebaseAuth.getInstance().currentUser?.uid
                        )
                        tx.set(statusRef, data)
                        null
                    }.addOnSuccessListener {
                        // Refleja en UI local (el listener del PASO 4 también lo actualizará)
                        openingAmount = amount
                        currentAmount = amount
                        isCashRegisterOpen = true
                        updateOpenCloseButtons()
                        updateCashRegisterDisplay()
                        Toast.makeText(this, "Caja abierta con ${numberFormat.format(amount)}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: "No se pudo abrir la caja (¿ya está abierta?)", Toast.LENGTH_LONG).show()
                    }
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

        // Construimos un AlertDialog clásico y luego coloreamos los botones
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Caja")
            .setMessage(message)
            .setPositiveButton("CERRAR CAJA", null) // listeners después
            .setNegativeButton("CANCELAR", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Evita que “desaparezcan” (texto blanco + fondo rojo/gris)
            pos.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            pos.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            neg.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            neg.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))

            // Lógica de cerrar caja (igual que tenías)
            pos.setOnClickListener {
                val db = FirebaseFirestore.getInstance()
                val statusRef = db.collection("cash").document("status")

                db.runTransaction { tx ->
                    val snap = tx.get(statusRef)
                    val isOpen = snap.getBoolean("isOpen") ?: false
                    if (!isOpen) return@runTransaction null
                    tx.update(statusRef, mapOf("isOpen" to false))
                    null
                }.addOnSuccessListener {
                    isCashRegisterOpen = false
                    updateOpenCloseButtons()

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
                    dialog.dismiss()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo cerrar la caja", Toast.LENGTH_LONG).show()
                }
            }

            neg.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

        private fun showAddWithdrawalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cash_movement, null)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextMovementAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMovementDescription)

        editTextDescription.hint = "Motivo del retiro"

            val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Retiro")
            .setView(dialogView)
            .setPositiveButton("Agregar Retiro", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
            positiveButton.setTextColor(resources.getColor(android.R.color.white, null))
            negativeButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            negativeButton.setTextColor(resources.getColor(android.R.color.white, null))

            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().replace(",", ".").toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()

                if (amount != null && amount > 0.0 && description.isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val statusRef = db.collection("cash").document("status")

                    db.runTransaction { tx ->
                        val snap = tx.get(statusRef)
                        val isOpen = snap.getBoolean("isOpen") ?: false
                        if (!isOpen) throw IllegalStateException("La caja está cerrada")

                        val current = snap.getDouble("current") ?: 0.0
                        if (amount > current) throw IllegalStateException("No hay suficiente efectivo en caja")

                        tx.update(statusRef, "current", current - amount)
                        null
                    }.addOnSuccessListener {
                        currentAmount -= amount

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
                        Toast.makeText(this, "Retiro de ${numberFormat.format(amount)} agregado", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: "No se pudo registrar el retiro", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }

            // ¡OJO! Este va FUERA del positiveButton
            negativeButton.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }


    private fun showAddDepositDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cash_movement, null)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextMovementAmount)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextMovementDescription)

        editTextDescription.hint = "Motivo del depósito"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Depósito")
            .setView(dialogView)
            .setPositiveButton("Agregar Depósito", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
            positiveButton.setTextColor(resources.getColor(android.R.color.white, null))
            negativeButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            negativeButton.setTextColor(resources.getColor(android.R.color.white, null))

            positiveButton.setOnClickListener {
                val amount = editTextAmount.text.toString().replace(",", ".").toDoubleOrNull()
                val description = editTextDescription.text.toString().trim()

                if (amount != null && amount > 0.0 && description.isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val statusRef = db.collection("cash").document("status")

                    db.runTransaction { tx ->
                        val snap = tx.get(statusRef)
                        val isOpen = snap.getBoolean("isOpen") ?: false
                        if (!isOpen) throw IllegalStateException("La caja está cerrada")

                        val current = snap.getDouble("current") ?: 0.0
                        tx.update(statusRef, "current", current + amount)
                        null
                    }.addOnSuccessListener {
                        currentAmount += amount

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
                        Toast.makeText(this, "Depósito de ${numberFormat.format(amount)} agregado", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: "No se pudo registrar el depósito", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }

            // FUERA del positiveButton
            negativeButton.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()   // ← reemplazo directo
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadUserRole(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userRole = document.getString("role") ?: "waiter"
                    updateOpenCloseButtons()



                    if (!isCashRegisterOpen) {
                        if (userRole == "admin") {
                            showOpenCashRegisterDialog()
                        } else {
                            Toast.makeText(this, "Solo un administrador puede abrir la caja", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "No se encontró el rol del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }
    }
    /**
     * Muestra u oculta los botones Abrir / Cerrar
     * según si la caja está abierta.
     */
    private fun updateOpenCloseButtons() {
        if (isCashRegisterOpen) {
            // Caja abierta: nunca mostrar "Abrir"; "Cerrar" solo para admin
            buttonOpenCash.visibility = View.GONE
            buttonCloseCash.visibility = if (userRole == "admin") View.VISIBLE else View.GONE
        } else {
            // Caja cerrada: nunca mostrar "Cerrar"; "Abrir" solo para admin
            buttonCloseCash.visibility = View.GONE
            buttonOpenCash.visibility  = if (userRole == "admin") View.VISIBLE else View.GONE
        }
    }




}