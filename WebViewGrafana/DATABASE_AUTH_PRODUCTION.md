# Guida Database Authentication per Produzione

## 🔐 Credenziali Attuali vs Database

**ATTUALMENTE** le credenziali sono hardcoded in `MainActivity.kt`:

```kotlin
// Linee 132-136 in MainActivity.kt
val validCredentials = mapOf(
    "admin@serra.com" to "password",
    "user@serra.com" to "user123",
    "manager@serra.com" to "manager456"
)
```

**PER LA PRODUZIONE** devi sostituire con un database sicuro.

---

## 📋 Opzioni di Implementazione

### 1. Database Locale (SQLite) ✅ **CONSIGLIATO PER INIZIARE**

### 2. Database Remoto (MySQL/PostgreSQL) ✅ **PER PRODUZIONE**

### 3. API di Autenticazione ✅ **PIÙ SICURO**

---

## 🛠️ OPZIONE 1: Database SQLite Locale

### Step 1: Aggiungi Dipendenze

```kotlin
// File: app/build.gradle.kts
dependencies {
    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Password sicure
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 2: Crea Entità User

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/User.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val passwordHash: String,  // Password hashata con BCrypt
    val role: String,         // admin, user, manager
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0
)
```

### Step 3: Crea DAO

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/UserDao.kt
import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1")
    suspend fun getUserByEmail(email: String): User?
    
    @Insert
    suspend fun insertUser(user: User)
    
    @Query("UPDATE users SET lastLogin = :timestamp WHERE email = :email")
    suspend fun updateLastLogin(email: String, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun getUserCount(email: String): Int
}
```

### Step 4: Crea Database

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/AppDatabase.kt
import androidx.room.*
import android.content.Context

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "serra_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### Step 5: Service di Autenticazione

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/auth/AuthService.kt
import android.content.Context
import org.mindrot.jbcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthService(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    
    /**
     * Autentica utente con database
     */
    suspend fun authenticate(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Cerca utente
                val user = userDao.getUserByEmail(email.lowercase())
                    ?: return@withContext AuthResult.Error("Credenziali non valide")
                
                // Verifica password hashata
                if (!BCrypt.checkpw(password, user.passwordHash)) {
                    return@withContext AuthResult.Error("Credenziali non valide")
                }
                
                // Aggiorna ultimo login
                userDao.updateLastLogin(email.lowercase(), System.currentTimeMillis())
                
                AuthResult.Success(user)
                
            } catch (e: Exception) {
                AuthResult.Error("Errore del server")
            }
        }
    }
    
    /**
     * Crea utenti di default se non esistono
     */
    suspend fun initializeDefaultUsers() {
        withContext(Dispatchers.IO) {
            val defaultUsers = listOf(
                Triple("admin@serra.com", "SecurePass123!", "admin"),
                Triple("user@serra.com", "UserPass123!", "user"),
                Triple("manager@serra.com", "ManagerPass123!", "manager")
            )
            
            for ((email, password, role) in defaultUsers) {
                if (userDao.getUserCount(email) == 0) {
                    // Hash password e inserisci
                    val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12))
                    val user = User(
                        email = email.lowercase(),
                        passwordHash = passwordHash,
                        role = role
                    )
                    userDao.insertUser(user)
                }
            }
        }
    }
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
```

### Step 6: Aggiorna MainActivity

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/MainActivity.kt

class MainActivity : AppCompatActivity() {
    // Aggiungi queste variabili
    private lateinit var authService: AuthService
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializza auth service
        authService = AuthService(this)
        
        // Crea utenti di default
        mainScope.launch {
            authService.initializeDefaultUsers()
        }
        
        // Resto del codice esistente...
        checkLoginStatus()
    }
    
    private inner class WebAppInterface {
        
        @JavascriptInterface
        fun performLogin(email: String, password: String): String {
            // Rimuovi il vecchio codice hardcoded e sostituisci con:
            
            mainScope.launch {
                val authResult = authService.authenticate(email, password)
                
                when (authResult) {
                    is AuthResult.Success -> {
                        // Login valido
                        isLoggedIn = true
                        currentUser = authResult.user.email
                        
                        // Salva sessione
                        saveUserSession(authResult.user.email, authResult.user.role)
                        
                        // Notifica successo
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "setLoginStatus('success', 'Login effettuato con successo!')", 
                                null
                            )
                        }
                    }
                    
                    is AuthResult.Error -> {
                        // Login non valido
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "setLoginStatus('error', '${authResult.message}')", 
                                null
                            )
                        }
                    }
                }
            }
            
            return "processing" // Temporaneo, la risposta arriva via callback
        }
        
        // Altri metodi esistenti...
    }
    
    private fun saveUserSession(email: String, role: String) {
        with(sharedPreferences.edit()) {
            putString("user_email", email)
            putString("user_role", role)  // Nuovo: salva anche il ruolo
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }
}
```

---

## 🔐 SICUREZZA: Password Hashing

### BCrypt vs Password in Chiaro

**❌ MALE (attuale):**
```kotlin
"admin@serra.com" to "password"  // Password in chiaro!
```

**✅ BENE (con database):**
```kotlin
// Password hashata con BCrypt
passwordHash: "$2a$12$NK6YQQ1Y.6zWn4YEOCyGl.XQBwVP7X8Y4KJ1Z2..."

// Verifica sicura
BCrypt.checkpw(inputPassword, storedHash)  // true/false
```

### Vantaggi BCrypt:
- 🔐 **Irreversibile**: Non si può ottenere la password originale
- 🛡️ **Salt automatico**: Ogni hash è unico
- ⏱️ **Lento di proposito**: Resistente a brute force
- 🔧 **Configurabile**: Puoi aumentare la difficoltà

---

## 🚀 OPZIONE 2: Database Remoto

### API Server (Node.js/PHP/Python)

```javascript
// Esempio API endpoint
POST /api/auth/login
{
    "email": "admin@serra.com",
    "password": "password123"
}

// Risposta
{
    "success": true,
    "user": {
        "id": 1,
        "email": "admin@serra.com",
        "role": "admin"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Implementazione Android

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/api/ApiService.kt
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}

data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val success: Boolean, val user: UserData?, val token: String?)
data class UserData(val id: Int, val email: String, val role: String)

class RemoteAuthService {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.serra.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)
    
    suspend fun authenticate(email: String, password: String): AuthResult {
        return try {
            val response = api.login(LoginRequest(email, password))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val userData = response.body()!!.user!!
                AuthResult.Success(
                    User(
                        id = userData.id,
                        email = userData.email,
                        passwordHash = "", // Non memorizzare
                        role = userData.role
                    )
                )
            } else {
                AuthResult.Error("Credenziali non valide")
            }
        } catch (e: Exception) {
            AuthResult.Error("Errore di connessione")
        }
    }
}
```

---

## 🔒 SICUREZZA AVANZATA

### 1. Rate Limiting

```kotlin
// Limita tentativi di login
class RateLimiter(private val context: Context) {
    private val prefs = context.getSharedPreferences("rate_limiter", Context.MODE_PRIVATE)
    
    fun canAttemptLogin(email: String): Boolean {
        val attempts = prefs.getInt("attempts_$email", 0)
        val lastAttempt = prefs.getLong("last_attempt_$email", 0)
        val now = System.currentTimeMillis()
        
        // Reset tentativi dopo 15 minuti
        if (now - lastAttempt > 15 * 60 * 1000) {
            resetAttempts(email)
            return true
        }
        
        return attempts < 5 // Massimo 5 tentativi
    }
    
    fun recordFailedAttempt(email: String) {
        val attempts = prefs.getInt("attempts_$email", 0) + 1
        with(prefs.edit()) {
            putInt("attempts_$email", attempts)
            putLong("last_attempt_$email", System.currentTimeMillis())
            apply()
        }
    }
    
    fun resetAttempts(email: String) {
        with(prefs.edit()) {
            remove("attempts_$email")
            remove("last_attempt_$email")
            apply()
        }
    }
}
```

### 2. Encrypted SharedPreferences

```kotlin
// Salvataggio sicuro delle sessioni
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private fun createEncryptedPrefs(): SharedPreferences {
    val masterKey = MasterKey.Builder(this)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    return EncryptedSharedPreferences.create(
        this,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### 3. Biometric Authentication

```kotlin
// Aggiungi autenticazione biometrica
implementation("androidx.biometric:biometric:1.1.0")

class BiometricAuthManager(private val activity: FragmentActivity) {
    fun authenticateWithBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val biometricPrompt = BiometricPrompt(activity, 
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticazione Biometrica")
            .setSubtitle("Usa la tua impronta per accedere")
            .setNegativeButtonText("Annulla")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}
```

---

## 📝 NUOVE CREDENZIALI SICURE

Con il database, puoi usare password più sicure:

```
Admin: admin@serra.com / SecurePass123!
User: user@serra.com / UserPass123!
Manager: manager@serra.com / ManagerPass123!
```

**Caratteristiche:**
- ✅ Almeno 8 caratteri
- ✅ Maiuscole e minuscole
- ✅ Numeri
- ✅ Caratteri speciali
- ✅ Hash BCrypt con salt

---

## 🛠️ IMPLEMENTAZIONE STEP-BY-STEP

### Fase 1: Database Locale (1-2 giorni)
1. ✅ Aggiungi dipendenze Room
2. ✅ Crea entità User
3. ✅ Implementa AuthService
4. ✅ Aggiorna MainActivity
5. ✅ Test con nuove credenziali

### Fase 2: Sicurezza (1 giorno)
1. ✅ Implementa rate limiting
2. ✅ Encrypted SharedPreferences
3. ✅ Validazione password forte

### Fase 3: Database Remoto (2-3 giorni)
1. ✅ Crea API server
2. ✅ Implementa RemoteAuthService
3. ✅ Gestione token JWT
4. ✅ Fallback locale

### Fase 4: Funzionalità Avanzate (1-2 giorni)
1. ✅ Autenticazione biometrica
2. ✅ Ruoli e permessi
3. ✅ Logout remoto
4. ✅ Sync offline

---

## ⚡ QUICK START - Solo Database Locale

Se vuoi iniziare velocemente:

1. **Copia le dipendenze** in `build.gradle.kts`
2. **Crea i 4 file**: User.kt, UserDao.kt, AppDatabase.kt, AuthService.kt
3. **Aggiorna MainActivity** per usare AuthService
4. **Testa** con le nuove credenziali sicure

**Tempo stimato: 2-3 ore** per un sviluppatore esperto.

---

## 🔍 TESTING

```kotlin
// Test delle credenziali
suspend fun testAuth() {
    val authService = AuthService(context)
    
    // Test login valido
    val result1 = authService.authenticate("admin@serra.com", "SecurePass123!")
    assert(result1 is AuthResult.Success)
    
    // Test login non valido
    val result2 = authService.authenticate("admin@serra.com", "wrong")
    assert(result2 is AuthResult.Error)
}
```

---

Questa guida ti fornisce tutto il necessario per passare da credenziali hardcoded a un sistema di autenticazione sicuro e professionale! 🚀

Vuoi che approfondisca qualche sezione specifica o ti aiuti con l'implementazione? 