package com.caferaquelita.restauranteapp.utils

import android.content.Context
import android.widget.Toast

/**
 * Utilidad para mostrar notificaciones y alertas.
 */
object NotificationUtils {
    fun showLowInventoryAlert(context: Context, productName: String) {
        Toast.makeText(context, "¡Inventario bajo para $productName!", Toast.LENGTH_LONG).show()
    }
} 