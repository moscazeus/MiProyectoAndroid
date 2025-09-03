package com.caferaquelita.restauranteapp.repositories

import com.caferaquelita.restauranteapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository para autenticación y gestión de usuarios.
 */
class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    /**
     * Iniciar sesión con email y contraseña
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Obtener datos del usuario desde Firestore
                val userDoc = usersCollection.document(user.uid).get().await()
                val userData = userDoc.toObject(User::class.java)

                if (userData != null) {
                    // Actualizar último login
                    updateLastLogin(user.uid)
                    Result.success(userData)
                } else {
                    // Si no existe en Firestore, crear usuario básico
                    val basicUser = User(
                        id = user.uid,
                        name = user.displayName ?: "Usuario",
                        email = user.email ?: email,
                        role = "waiter",
                        lastLogin = Date()
                    )
                    saveUserToFirestore(basicUser)
                    Result.success(basicUser)
                }
            } else {
                Result.failure(Exception("Error al iniciar sesión"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Iniciar sesión con Google
     */
    suspend fun loginWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): Result<User> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                val userData = User(
                    id = user.uid,
                    name = user.displayName ?: "Usuario",
                    email = user.email ?: "",
                    role = "waiter",
                    lastLogin = Date()
                )
                saveUserToFirestore(userData)
                Result.success(userData)
            } else {
                Result.failure(Exception("Error al iniciar sesión con Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registrar nuevo usuario
     */
    suspend fun register(name: String, email: String, password: String, role: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                val userData = User(
                    id = user.uid,
                    name = name,
                    email = email,
                    role = role,
                    hireDate = Date(),
                    lastLogin = Date()
                )
                saveUserToFirestore(userData)
                Result.success(userData)
            } else {
                Result.failure(Exception("Error al registrar usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Obtener usuario actual
     */
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Usuario",
                email = firebaseUser.email ?: "",
                role = "waiter" // Por defecto
            )
        } else null
    }

    /**
     * Verificar si hay usuario autenticado
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Crear usuarios de prueba para diferentes roles
     */
    suspend fun createTestUsers() {
        val testUsers = listOf(
            User(
                id = "admin_test",
                name = "Administrador",
                email = "admin@caferaquelita.com",
                role = "admin",
                hireDate = Date(),
                lastLogin = Date()
            ),
            User(
                id = "manager_test",
                name = "Gerente",
                email = "gerente@caferaquelita.com",
                role = "manager",
                hireDate = Date(),
                lastLogin = Date()
            ),
            User(
                id = "waiter_test",
                name = "Mesero",
                email = "mesero@caferaquelita.com",
                role = "waiter",
                hireDate = Date(),
                lastLogin = Date()
            ),
            User(
                id = "chef_test",
                name = "Cocinero",
                email = "cocinero@caferaquelita.com",
                role = "chef",
                hireDate = Date(),
                lastLogin = Date()
            ),
            User(
                id = "cashier_test",
                name = "Cajero",
                email = "cajero@caferaquelita.com",
                role = "cashier",
                hireDate = Date(),
                lastLogin = Date()
            )
        )

        for (user in testUsers) {
            saveUserToFirestore(user)
        }
    }

    /**
     * Guardar usuario en Firestore
     */
    private suspend fun saveUserToFirestore(user: User) {
        usersCollection.document(user.id).set(user).await()
    }

    /**
     * Actualizar último login
     */
    private suspend fun updateLastLogin(userId: String) {
        usersCollection.document(userId).update("lastLogin", Date()).await()
    }
}

