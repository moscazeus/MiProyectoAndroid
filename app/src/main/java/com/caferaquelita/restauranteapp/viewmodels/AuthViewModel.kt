package com.caferaquelita.restauranteapp.viewmodels

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caferaquelita.restauranteapp.models.User
import com.caferaquelita.restauranteapp.repositories.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para autenticación y gestión de usuarios.
 */
class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Iniciar sesión
     */
    fun login(email: String, password: String) {
        // Validaciones de entrada
        if (email.isBlank()) {
            _loginResult.value = Result.failure(Exception("El email es requerido"))
            return
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginResult.value = Result.failure(Exception("Formato de email no válido"))
            return
        }
        
        if (password.isBlank()) {
            _loginResult.value = Result.failure(Exception("La contraseña es requerida"))
            return
        }
        
        if (password.length < 6) {
            _loginResult.value = Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.login(email, password)
                _loginResult.value = result
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Iniciar sesión con Google
     */
    fun loginWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.loginWithGoogle(account)
                _loginResult.value = result
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Registrar nuevo usuario
     */
    fun register(name: String, email: String, password: String, role: String) {
        // Validaciones de entrada
        if (name.isBlank()) {
            _registerResult.value = Result.failure(Exception("El nombre es requerido"))
            return
        }
        
        if (email.isBlank()) {
            _registerResult.value = Result.failure(Exception("El email es requerido"))
            return
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerResult.value = Result.failure(Exception("Formato de email no válido"))
            return
        }
        
        if (password.isBlank()) {
            _registerResult.value = Result.failure(Exception("La contraseña es requerida"))
            return
        }
        
        if (password.length < 6) {
            _registerResult.value = Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            return
        }
        
        if (role.isBlank()) {
            _registerResult.value = Result.failure(Exception("El rol es requerido"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.register(name, email, password, role)
                _registerResult.value = result
            } catch (e: Exception) {
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        repository.logout()
    }

    /**
     * Obtener usuario actual
     */
    fun getCurrentUser(): User? {
        return repository.getCurrentUser()
    }

    /**
     * Verificar si hay usuario autenticado
     */
    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }
    
    /**
     * Limpiar resultados
     */
    fun clearResults() {
        _loginResult.value = null
        _registerResult.value = null
    }
    
    /**
     * Limpiar datos al destruir ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        _loginResult.value = null
        _registerResult.value = null
        _isLoading.value = false
    }
} 