package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.models.User
import com.caferaquelita.restauranteapp.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Actividad principal que muestra el menú de la aplicación según el rol del usuario.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var textViewWelcome: TextView
    private lateinit var textViewRole: TextView
    private lateinit var buttonInventory: Button
    private lateinit var buttonTables: Button
    private lateinit var buttonBilling: Button
    private lateinit var buttonCashRegister: Button
    private lateinit var buttonDashboard: Button
    private lateinit var buttonEmployees: Button
    private lateinit var buttonLogout: Button
    
    private var currentUser: User? = null
    private val authViewModel = AuthViewModel()

private fun renderPermissions(perms: Map<String, Boolean>?) {
    val canInventory     = perms?.get("manage_inventory")      == true
    val canTables        = perms?.get("manage_tables")         == true
    val canBilling       = perms?.get("generate_invoices")     == true
    val canCashRegister  = perms?.get("manage_cash_register")  == true
    val canDashboard     = perms?.get("view_reports")          == true
    val canEmployees     = perms?.get("manage_employees")      == true

    buttonInventory.visibility    = if (canInventory)    View.VISIBLE else View.GONE
    buttonTables.visibility       = if (canTables)       View.VISIBLE else View.GONE
    buttonBilling.visibility      = if (canBilling)      View.VISIBLE else View.GONE
    buttonCashRegister.visibility = if (canCashRegister) View.VISIBLE else View.GONE
    buttonDashboard.visibility    = if (canDashboard)    View.VISIBLE else View.GONE
    buttonEmployees.visibility    = if (canEmployees)    View.VISIBLE else View.GONE
}


    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupUserInfo()
        setupUI()
    }

    override fun onStart() {
    super.onStart()
    currentUser = authViewModel.getCurrentUser()
    renderPermissions(currentUser?.permissions)
}


    private fun setupViews() {
        textViewWelcome = findViewById(R.id.textViewWelcome)
        textViewRole = findViewById(R.id.textViewRole)
        buttonInventory = findViewById(R.id.buttonInventory)
        buttonTables = findViewById(R.id.buttonTables)
        buttonBilling = findViewById(R.id.buttonBilling)
        buttonCashRegister = findViewById(R.id.buttonCashRegister)
        buttonDashboard = findViewById(R.id.buttonDashboard)
        buttonEmployees = findViewById(R.id.buttonEmployees)
        buttonLogout = findViewById(R.id.buttonLogout)
    }

    private fun setupUserInfo() {
        // Obtener información del usuario actual
        currentUser = authViewModel.getCurrentUser()
        
        // Obtener rol desde el intent (si viene de login)
        val userRole = intent.getStringExtra("user_role") ?: currentUser?.role ?: "waiter"
        
        // Actualizar información del usuario
        updateUserDisplay(userRole)
    }

    private fun updateUserDisplay(role: String) {
        val userName = currentUser?.name ?: "Usuario"
        val roleDisplayName = when (role) {
            "admin" -> "Administrador"
            "manager" -> "Gerente"
            "waiter" -> "Mesero"
            "chef" -> "Cocinero"
            "cashier" -> "Cajero"
            else -> "Usuario"
        }

        textViewWelcome.text = "¡Bienvenido, $userName!"
        textViewRole.text = "Rol: $roleDisplayName"
    }

    private fun setupUI() {
        // Configurar botones según el rol del usuario
        setupButtonsByRole()
        
        // Configurar listeners de botones
        setupButtonListeners()
    }

    private fun setupButtonsByRole() {
    renderPermissions(currentUser?.permissions)
}


    private fun setupButtonListeners() {
        buttonInventory.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        
        buttonTables.setOnClickListener {
            startActivity(Intent(this, TablesActivity::class.java))
        }
        
        buttonBilling.setOnClickListener {
            startActivity(Intent(this, InvoiceReportActivity::class.java))
        }
        
        buttonCashRegister.setOnClickListener {
            startActivity(Intent(this, CashRegisterActivity::class.java))
        }
        
        buttonDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        
        buttonEmployees.setOnClickListener {
            // TODO: Implementar EmployeesActivity
            // startActivity(Intent(this, EmployeesActivity::class.java))
        }
        
        buttonLogout.setOnClickListener {
            // Cerrar sesión
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
} 
