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
    val permissions: Map<String, Boolean> = emptyMap()

) {
    /**
     * Verificar si el usuario tiene un permiso específico
     */
    fun hasPermission(permission: String): Boolean {
        // 1) Si es admin, tiene todos los permisos.
        if (isAdmin()) return true

        // 2) Probamos con la clave tal cual, en minúsculas y en snake_case.
        val keyOriginal   = permission
        val keyLower      = permission.trim().lowercase()
        val keySnake      = camelToSnake(permission)

        return permissions[keyOriginal] == true ||
                permissions[keyLower]   == true ||
                permissions[keySnake]   == true
    }

    // Convierte "manageTables" -> "manage_tables", y normaliza guiones/espacios.
    private fun camelToSnake(s: String): String =
        s.trim()
            .replace(Regex("([a-z])([A-Z]+)"), "$1_$2")
            .replace(Regex("[-\\s]+"), "_")
            .lowercase()




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