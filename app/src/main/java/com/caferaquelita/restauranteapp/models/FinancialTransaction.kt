package com.caferaquelita.restauranteapp.models

import java.util.Date

/**
 * Modelo para transacciones financieras (ingresos y gastos).
 */
data class FinancialTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.INCOME,
    val category: TransactionCategory = TransactionCategory.OTHER_INCOME,
    val description: String = "",
    val date: Date = Date(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val invoiceId: String? = null,
    val tableId: String? = null,
    val createdBy: String = "",
    val notes: String = ""
)

enum class TransactionType {
    INCOME,    // Ingresos
    EXPENSE    // Gastos
}

enum class TransactionCategory {
    // Categorías de Ingresos
    SALES,             // Ventas de comida/bebidas
    DELIVERY,          // Pedidos a domicilio
    CATERING,          // Servicios de catering
    EVENTS,            // Eventos especiales
    OTHER_INCOME,      // Otros ingresos
    
    // Categorías de Gastos
    INVENTORY,         // Compras de inventario
    SUPPLIES,          // Suministros
    UTILITIES,         // Servicios públicos
    RENT,              // Alquiler
    SALARY,            // Salarios
    MAINTENANCE,       // Mantenimiento
    MARKETING,         // Publicidad y marketing
    INSURANCE,         // Seguros
    TAXES,             // Impuestos
    OTHER_EXPENSE      // Otros gastos
}

enum class PaymentMethod {
    CASH,           // Efectivo
    CARD,           // Tarjeta de crédito/débito
    TRANSFER,       // Transferencia bancaria
    CHECK,          // Cheque
    DIGITAL         // Pago digital (Nequi, Daviplata, etc.)
} 