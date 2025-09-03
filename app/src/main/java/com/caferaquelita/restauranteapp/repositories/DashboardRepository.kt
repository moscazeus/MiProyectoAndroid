package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date


class DashboardRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun startOfDay(): Timestamp {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return Timestamp(c.time)
    }

    private fun endOfDay(): Timestamp {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
        return Timestamp(c.time)
    }

    private fun startOfMonth(): Timestamp {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return Timestamp(c.time)
    }

    /** Utilidad para leer el total en facturas con distintos nombres de campo. */
    private fun readInvoiceTotal(map: Map<String, Any?>): Double {
        val d1 = (map["total"] as? Number)?.toDouble()
        val d2 = (map["totalAmount"] as? Number)?.toDouble()
        return (d1 ?: d2 ?: 0.0)
    }

    /** Stream EN VIVO del dashboard (escucha facturas de HOY, consulta el resto). */
    fun listenDashboard() = callbackFlow {
        val todayStart = startOfDay()
        val todayEnd = endOfDay()

        val invoicesTodayRef = db.collection("invoices")
            .whereGreaterThanOrEqualTo("createdAt", todayStart)
            .whereLessThanOrEqualTo("createdAt", todayEnd)

        val reg = invoicesTodayRef.addSnapshotListener { snap, _ ->
            var todayTotal = 0.0
            var todayCount = 0
            snap?.documents?.forEach { d ->
                todayTotal += readInvoiceTotal(d.data ?: emptyMap())
                todayCount++
            }

            // Mesas libres/ocupadas
            db.collection("tables").get().addOnSuccessListener { tSnap ->
                val occupied = tSnap.documents.count { it.getString("status") == "OCCUPIED" }
                val free = tSnap.size() - occupied

                // Caja
                db.collection("cash").document("status").get().addOnSuccessListener { cDoc ->
                    val cashOpen = cDoc.getBoolean("isOpen") == true
                    val cashInitial = cDoc.getDouble("initial") ?: 0.0
                    // Si no tienes campo "current", calcula: inicial + ventas de hoy
                    val manualCurrent = cDoc.getDouble("current")
                    val cashCurrent = manualCurrent ?: (cashInitial + todayTotal)


                    // Mes (consulta única)
                    val monthStart = startOfMonth()
                    db.collection("invoices")
                        .whereGreaterThanOrEqualTo("createdAt", monthStart)
                        .whereLessThanOrEqualTo("createdAt", todayEnd)
                        .get()
                        .addOnSuccessListener { mSnap ->
                            var monthTotal = 0.0
                            var monthCount = 0
                            mSnap.documents.forEach { md ->
                                monthTotal += readInvoiceTotal(md.data ?: emptyMap())
                                monthCount++
                            }

                            // Armamos tu DashboardData usando solo lo que ya tenemos
                            val financial = FinancialSummary(
                                totalIncome = todayTotal,
                                // Si luego agregas gastos, se rellenan; por ahora 0.0
                                totalExpenses = 0.0,
                                netProfit = todayTotal,   // aproximación: ingreso = “utilidad” si no hay gastos
                                profitMargin = 0.0,
                                averageTicket = if (todayCount > 0) todayTotal / todayCount else 0.0,
                                totalTransactions = todayCount
                            )

                            val sales = SalesMetrics(
                                totalSales = monthTotal,   // ventas del mes
                                totalOrders = monthCount,
                                averageOrderValue = if (monthCount > 0) monthTotal / monthCount else 0.0,
                                salesByPaymentMethod = emptyMap(),
                                salesByCategory = emptyMap()
                            )

                            val data = DashboardData(
                                period = "today",
                                dateRange = DateRange(Date(), Date()),
                                financialSummary = financial,
                                salesMetrics = sales,
                                // usamos tablePerformance como “resumen de mesas” si lo quieres pintar
                                tablePerformance = listOf(
                                    TablePerformance(tableNumber = -1, totalSales = 0.0, totalOrders = free, averageTicket = 0.0, utilizationRate = 0.0),
                                    TablePerformance(tableNumber = -2, totalSales = 0.0, totalOrders = occupied, averageTicket = 0.0, utilizationRate = 0.0),
                                ),
                                // dailySales lo dejamos vacío por ahora
                            )

                            trySend(Triple(data, cashOpen, Pair(cashInitial, cashCurrent)))
                        }
                }
            }
        }

        awaitClose { reg.remove() }
    }

    /** Carga manual (pull-to-refresh). */
    suspend fun fetchOnce(): Triple<DashboardData, Boolean, Pair<Double, Double>> {
        val todayStart = startOfDay()
        val todayEnd = endOfDay()
        val monthStart = startOfMonth()

        val invToday = db.collection("invoices")
            .whereGreaterThanOrEqualTo("createdAt", todayStart)
            .whereLessThanOrEqualTo("createdAt", todayEnd)
            .get().await()

        var todayTotal = 0.0
        var todayCount = 0
        invToday.documents.forEach { d ->
            todayTotal += readInvoiceTotal(d.data ?: emptyMap()); todayCount++
        }

        val tables = db.collection("tables").get().await()
        val occupied = tables.documents.count { it.getString("status") == "OCCUPIED" }
        val free = tables.size() - occupied

        val cash = db.collection("cash").document("status").get().await()
        val cashOpen = cash.getBoolean("isOpen") == true
        val cashInitial = cash.getDouble("initial") ?: 0.0
        val manualCurrent = cash.getDouble("current")
        val cashCurrent = manualCurrent ?: (cashInitial + todayTotal)


        val invMonth = db.collection("invoices")
            .whereGreaterThanOrEqualTo("createdAt", monthStart)
            .whereLessThanOrEqualTo("createdAt", todayEnd)
            .get().await()

        var monthTotal = 0.0
        var monthCount = 0
        invMonth.documents.forEach { d ->
            monthTotal += readInvoiceTotal(d.data ?: emptyMap()); monthCount++
        }

        val financial = FinancialSummary(
            totalIncome = todayTotal,
            totalExpenses = 0.0,
            netProfit = todayTotal,
            profitMargin = 0.0,
            averageTicket = if (todayCount > 0) todayTotal / todayCount else 0.0,
            totalTransactions = todayCount
        )

        val sales = SalesMetrics(
            totalSales = monthTotal,
            totalOrders = monthCount,
            averageOrderValue = if (monthCount > 0) monthTotal / monthCount else 0.0,
            salesByPaymentMethod = emptyMap(),
            salesByCategory = emptyMap()
        )

        val data = DashboardData(
            period = "today",
            dateRange = DateRange(Date(), Date()),
            financialSummary = financial,
            salesMetrics = sales,
            tablePerformance = listOf(
                TablePerformance(tableNumber = -1, totalSales = 0.0, totalOrders = free, averageTicket = 0.0, utilizationRate = 0.0),
                TablePerformance(tableNumber = -2, totalSales = 0.0, totalOrders = occupied, averageTicket = 0.0, utilizationRate = 0.0),
            )
        )

        return Triple(data, cashOpen, Pair(cashInitial, cashCurrent))
    }
}
