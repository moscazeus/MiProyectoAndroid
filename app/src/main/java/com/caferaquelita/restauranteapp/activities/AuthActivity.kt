package com.caferaquelita.restauranteapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.caferaquelita.restauranteapp.R
import com.caferaquelita.restauranteapp.viewmodels.AuthViewModel
import androidx.core.widget.doAfterTextChanged

/**
 * Actividad para login y registro de usuarios con roles de empleado y administrador.
 */
class AuthActivity : AppCompatActivity() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setupViews()
        setupUI()
        observeViewModel()
    }

    private fun setupViews() {
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        
        // Configuración simplificada para evitar problemas con Google Play Services
        
        
        // Configuración básica del campo
        editTextPassword.isFocusable = true
        editTextPassword.isFocusableInTouchMode = true
        editTextPassword.isEnabled = true
        editTextPassword.isClickable = true
        
        
        
        // Listener simple que NO modifica el texto (no usar setText aquí)
editTextPassword.doAfterTextChanged {
    // Si quieres, valida para habilitar el botón:
    val ok = (it?.length ?: 0) >= 6
    btnLogin.isEnabled = ok
}

    }

    private fun setupUI() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                val email = editTextEmail.text.toString().trim()
                val password = editTextPassword.text.toString()
                
                progressBar.visibility = View.VISIBLE
                btnLogin.isEnabled = false
                btnRegister.isEnabled = false
                
                viewModel.login(email, password)
            }
        }
        
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                showRegisterDialog()
            }
        }
        
        // Botón de prueba para crear usuarios de prueba
        findViewById<Button>(R.id.btnTestUsers)?.setOnClickListener {
            createTestUsers()
        }
        
        // Botón de prueba para verificar campo de contraseña
        findViewById<Button>(R.id.btnTestPassword)?.setOnClickListener {
            testPasswordField()
        }
        
        // Botón de bypass temporal
        findViewById<Button>(R.id.btnBypass)?.setOnClickListener {
            bypassLogin()
        }
    }

    private fun validateInputs(): Boolean {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString()
        
        // Validar email
        if (email.isBlank()) {
            editTextEmail.error = "El email es requerido"
            editTextEmail.requestFocus()
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Formato de email no válido"
            editTextEmail.requestFocus()
            return false
        }
        
        // Validar contraseña
        if (password.isBlank()) {
            editTextPassword.error = "La contraseña es requerida"
            editTextPassword.requestFocus()
            return false
        }
        
        if (password.length < 6) {
            editTextPassword.error = "La contraseña debe tener al menos 6 caracteres"
            editTextPassword.requestFocus()
            return false
        }
        
        return true
    }

    private fun observeViewModel() {
        // Observar resultado de login
        viewModel.loginResult.observe(this) { result ->
            result.fold(
                onSuccess = { user ->
                    val welcomeMessage = when (user.role) {
                        "admin" -> "¡Bienvenido Administrador ${user.name}!"
                        "manager" -> "¡Bienvenido Gerente ${user.name}!"
                        "waiter" -> "¡Bienvenido Mesero ${user.name}!"
                        "chef" -> "¡Bienvenido Cocinero ${user.name}!"
                        "cashier" -> "¡Bienvenido Cajero ${user.name}!"
                        else -> "¡Bienvenido ${user.name}!"
                    }
                    Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
                    navigateToMainActivity(user.role)
                },
                onFailure = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    resetUI()
                }
            )
        }

        // Observar resultado de registro
        viewModel.registerResult.observe(this) { result ->
            result.fold(
                onSuccess = { user ->
                    Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity(user.role)
                },
                onFailure = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    resetUI()
                }
            )
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
            btnRegister.isEnabled = !isLoading
        }
    }

    private fun showRegisterDialog() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString()
        
        // Por defecto registrar como mesero (rol básico)
        viewModel.register("Usuario", email, password, "waiter")
    }

    private fun navigateToMainActivity(role: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_role", role)
        startActivity(intent)
        finish()
    }
    
    private fun resetUI() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnRegister.isEnabled = true
    }
    
    private fun createTestUsers() {
        // Crear usuarios de prueba automáticamente
        val testUsers = listOf(
            "admin@caferaquelita.com" to "123456",
            "gerente@caferaquelita.com" to "123456",
            "mesero@caferaquelita.com" to "123456",
            "cocinero@caferaquelita.com" to "123456",
            "cajero@caferaquelita.com" to "123456"
        )
        
        var currentIndex = 0
        val showNextUser = {
            if (currentIndex < testUsers.size) {
                val (email, password) = testUsers[currentIndex]
                editTextEmail.setText(email)
                editTextPassword.setText(password)
                currentIndex++
                
                Toast.makeText(this, "Usuario de prueba: $email", Toast.LENGTH_SHORT).show()
            }
        }
        
        showNextUser()
    }
    
    private fun testPasswordField() {
        // Probar el campo de contraseña
        editTextPassword.requestFocus()
        editTextPassword.setText("test123")
        
        Toast.makeText(this, "Campo de contraseña probado. Texto: ${editTextPassword.text}", Toast.LENGTH_LONG).show()
    }
    
    private fun bypassLogin() {
        // Bypass temporal para acceder a la app
        Toast.makeText(this, "Acceso directo - Modo Administrador", Toast.LENGTH_LONG).show()
        navigateToMainActivity("admin")
    }
} 
