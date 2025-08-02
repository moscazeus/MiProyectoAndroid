package com.caferaquelita.restauranteapp.models

import java.util.Date

/**
 * Modelo de usuario para autenticación y roles.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // "admin", "manager", "waiter", "chef", "cashier"
    val isActive: Boolean = true,
    val hireDate: Date = Date(),
    val lastLogin: Date? = null,
    val permissions: List<String> = emptyList()
) {
    /**
     * Verificar si el usuario tiene un permiso específico
     */
    fun hasPermission(permission: String): Boolean {
        return when (role) {
            "admin" -> true // Administrador tiene todos los permisos
            "manager" -> permission in listOf(
                "manage_tables", "manage_inventory", "view_reports", 
                "manage_cash_register", "generate_invoices", "manage_employees"
            )
            "waiter" -> permission in listOf(
                "manage_tables", "generate_invoices", "view_reports"
            )
            "chef" -> permission in listOf(
                "manage_inventory", "view_reports"
            )
            "cashier" -> permission in listOf(
                "manage_cash_register", "generate_invoices", "view_reports"
            )
            else -> false
        }
    }

    /**
     * Obtener el nombre del rol en español
     */
    fun getRoleDisplayName(): String {
        return when (role) {
            "admin" -> "Administrador"
            "manager" -> "Gerente"
            "waiter" -> "Mesero"
            "chef" -> "Cocinero"
            "cashier" -> "Cajero"
            else -> "Usuario"
        }
    }

    /**
     * Verificar si es administrador
     */
    fun isAdmin(): Boolean = role == "admin"

    /**
     * Verificar si es gerente o administrador
     */
    fun isManagerOrAdmin(): Boolean = role in listOf("admin", "manager")
} 