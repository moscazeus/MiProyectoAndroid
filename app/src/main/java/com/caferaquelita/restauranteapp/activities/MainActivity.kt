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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupUserInfo()
        setupUI()
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
        val role = currentUser?.role ?: "waiter"
        
        // Mostrar/ocultar botones según permisos
        buttonInventory.visibility = if (currentUser?.hasPermission("manage_inventory") == true) View.VISIBLE else View.GONE
        buttonTables.visibility = if (currentUser?.hasPermission("manage_tables") == true) View.VISIBLE else View.GONE
        buttonBilling.visibility = if (currentUser?.hasPermission("generate_invoices") == true) View.VISIBLE else View.GONE
        buttonCashRegister.visibility = if (currentUser?.hasPermission("manage_cash_register") == true) View.VISIBLE else View.GONE
        buttonDashboard.visibility = if (currentUser?.hasPermission("view_reports") == true) View.VISIBLE else View.GONE
        buttonEmployees.visibility = if (currentUser?.hasPermission("manage_employees") == true) View.VISIBLE else View.GONE
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