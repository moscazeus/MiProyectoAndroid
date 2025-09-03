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
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.widget.Toast



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
    private val authViewModel: AuthViewModel by viewModels()

    // Listener de Firestore y flag de estado de caja
    private var cashStatusListener: ListenerRegistration? = null
    private var isCashOpen: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupUserInfo()
        setupUI()

        // Observa el documento completo del usuario en Firestore (incluye permissions)
        authViewModel.currentUserData.observe(this) { user ->
            currentUser = user
            if (user != null) {
                // Actualiza cabecera (nombre/rol) con lo que viene de Firestore
                updateUserDisplay(user.role)
                // Aplica permisos reales
                applyPermissions(user)
            } else {
                // Si no hay doc o falló, puedes ocultar todo menos "Cerrar sesión" si quieres
                buttonInventory.isVisible = false
                buttonTables.isVisible = false
                buttonBilling.isVisible = false
                buttonCashRegister.isVisible = false
                buttonDashboard.isVisible = false
                buttonEmployees.isVisible = false
            }
        }

    }

    override fun onStart() {
        super.onStart()
        if (authViewModel.isUserLoggedIn()) {
            authViewModel.fetchCurrentUserData() // ← Esto trae el doc de Firestore con "permissions"
            // Escucha en tiempo real si la caja está abierta
            cashStatusListener = FirebaseFirestore.getInstance()
                .collection("cash")
                .document("status")
                .addSnapshotListener { snap, _ ->
                    isCashOpen = snap?.getBoolean("isOpen") == true
                    // Si quieres, aquí puedes mostrar/ocultar algún aviso en la pantalla principal
                    // o actualizar botones, pero lo importante es tener isCashOpen actualizado.
                }

        }
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
        currentUser?.let { applyPermissions(it) } ?: run {
            // Oculta hasta que lleguen permisos reales
            buttonInventory.isVisible = false
            buttonTables.isVisible = false
            buttonBilling.isVisible = false
            buttonCashRegister.isVisible = false
            buttonDashboard.isVisible = false
            buttonEmployees.isVisible = false
        }
    }



    private fun setupButtonListeners() {
        buttonInventory.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        buttonTables.setOnClickListener {
            if (!isCashOpen) {
                Toast.makeText(
                    this,
                    "No hay caja abierta. Abre la caja para gestionar mesas.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this, CashRegisterActivity::class.java))
                return@setOnClickListener
            }
            startActivity(Intent(this, TablesActivity::class.java))
        }


        buttonBilling.setOnClickListener {
            if (!isCashOpen) {
                Toast.makeText(this, "No hay caja abierta. Abre la caja para facturar.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, CashRegisterActivity::class.java))
                return@setOnClickListener
            }
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
            authViewModel.logout() // ← limpia currentUserData en el VM
            startActivity(Intent(this, AuthActivity::class.java))
            finish()

        }

    }
    private fun applyPermissions(user: User) {
        // Claves EXACTAS en snake_case como están en Firestore
        buttonTables.isVisible       = user.hasPermission("manage_tables")
        buttonBilling.isVisible      = user.hasPermission("generate_invoices")
        buttonCashRegister.isVisible = user.hasPermission("manage_cash_register")
        buttonInventory.isVisible    = user.hasPermission("manage_inventory")
        buttonDashboard.isVisible    = user.hasPermission("view_reports")
        buttonEmployees.isVisible    = user.hasPermission("manage_employees")
    }
    override fun onStop() {
        super.onStop()
        cashStatusListener?.remove()
        cashStatusListener = null
    }

} 
