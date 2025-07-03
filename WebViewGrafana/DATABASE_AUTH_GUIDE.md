# Guida Implementazione Database Authentication

Questa guida fornisce il codice completo per sostituire le credenziali hardcoded con un sistema di autenticazione basato su database, includendo tutte le misure di sicurezza necessarie per la produzione.

## Indice

1. [Database Locale (SQLite)](#database-locale-sqlite)
2. [Database Remoto (MySQL/PostgreSQL)](#database-remoto-mysqlpostgresql)
3. [API di Autenticazione](#api-di-autenticazione)
4. [Sicurezza e Crittografia](#sicurezza-e-crittografia)
5. [Gestione Errori e Logging](#gestione-errori-e-logging)
6. [Best Practices di Produzione](#best-practices-di-produzione)

---

## Database Locale (SQLite)

### 1. Dipendenze (build.gradle.kts)

```kotlin
dependencies {
    // Esistenti...
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Per hashing password
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Per coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
}
```

### 2. Entità Database

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/User.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0
)
```

### 3. Data Access Object (DAO)

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/UserDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1")
    suspend fun getUserByEmail(email: String): User?
    
    @Insert
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET lastLogin = :timestamp WHERE email = :email")
    suspend fun updateLastLogin(email: String, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun getUserCount(email: String): Int
}
```

### 4. Database

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/database/AppDatabase.kt
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
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

### 5. Service di Autenticazione

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/auth/AuthService.kt
import android.content.Context
import android.util.Log
import org.mindrot.jbcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthService(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    
    companion object {
        private const val TAG = "AuthService"
        private const val BCRYPT_ROUNDS = 12
    }
    
    /**
     * Autentica un utente con email e password
     */
    suspend fun authenticate(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting authentication for: $email")
                
                // Validazione input
                if (!isValidEmail(email)) {
                    return@withContext AuthResult.Error("Email non valida")
                }
                
                if (password.length < 6) {
                    return@withContext AuthResult.Error("Password troppo corta")
                }
                
                // Cerca utente nel database
                val user = userDao.getUserByEmail(email.lowercase())
                
                if (user == null) {
                    Log.w(TAG, "User not found: $email")
                    return@withContext AuthResult.Error("Credenziali non valide")
                }
                
                // Verifica password
                if (!BCrypt.checkpw(password, user.passwordHash)) {
                    Log.w(TAG, "Invalid password for: $email")
                    return@withContext AuthResult.Error("Credenziali non valide")
                }
                
                // Aggiorna ultimo login
                userDao.updateLastLogin(email.lowercase(), System.currentTimeMillis())
                
                Log.i(TAG, "Authentication successful for: $email")
                AuthResult.Success(user)
                
            } catch (e: Exception) {
                Log.e(TAG, "Authentication error", e)
                AuthResult.Error("Errore del server")
            }
        }
    }
    
    /**
     * Crea un nuovo utente
     */
    suspend fun createUser(email: String, password: String, role: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Verifica se utente esiste già
                if (userDao.getUserCount(email.lowercase()) > 0) {
                    return@withContext AuthResult.Error("Utente già esistente")
                }
                
                // Hash della password
                val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
                
                // Crea nuovo utente
                val user = User(
                    email = email.lowercase(),
                    passwordHash = passwordHash,
                    role = role
                )
                
                userDao.insertUser(user)
                
                Log.i(TAG, "User created successfully: $email")
                AuthResult.Success(user)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user", e)
                AuthResult.Error("Errore nella creazione utente")
            }
        }
    }
    
    /**
     * Inizializza utenti di default
     */
    suspend fun initializeDefaultUsers() {
        withContext(Dispatchers.IO) {
            try {
                // Crea utenti di default se non esistono
                val defaultUsers = listOf(
                    Triple("admin@serra.com", "admin123!@#", "admin"),
                    Triple("user@serra.com", "user123!@#", "user"),
                    Triple("manager@serra.com", "manager123!@#", "manager")
                )
                
                for ((email, password, role) in defaultUsers) {
                    if (userDao.getUserCount(email) == 0) {
                        createUser(email, password, role)
                    }
                }
                
                Log.i(TAG, "Default users initialized")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing default users", e)
            }
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}

/**
 * Risultato dell'autenticazione
 */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
```

### 6. MainActivity Aggiornata

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/MainActivity.kt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // Variabili esistenti...
    private lateinit var authService: AuthService
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializzazione esistente...
        authService = AuthService(this)
        
        // Inizializza utenti di default
        mainScope.launch {
            authService.initializeDefaultUsers()
        }
        
        // Resto del codice esistente...
        checkLoginStatus()
    }
    
    private inner class WebAppInterface {
        
        @JavascriptInterface
        fun performLogin(email: String, password: String): String {
            var result = "error"
            
            // Esegui autenticazione in background
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
                        
                        result = "success"
                    }
                    
                    is AuthResult.Error -> {
                        // Login non valido
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "setLoginStatus('error', '${authResult.message}')", 
                                null
                            )
                        }
                        
                        result = "error"
                    }
                }
            }
            
            return result
        }
        
        // Altri metodi esistenti...
    }
    
    private fun saveUserSession(email: String, role: String) {
        with(sharedPreferences.edit()) {
            putString("user_email", email)
            putString("user_role", role)
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }
    
    // Altri metodi esistenti...
}
```

---

## Database Remoto (MySQL/PostgreSQL)

### 1. Dipendenze Aggiuntive

```kotlin
dependencies {
    // HTTP Client per chiamate API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Sicurezza
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

### 2. API Interface

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/api/AuthApi.kt
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(@Body logoutRequest: LogoutRequest): Response<BaseResponse>
}

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse?,
    val token: String?
)

data class UserResponse(
    val id: Int,
    val email: String,
    val role: String,
    val name: String?
)

data class LogoutRequest(
    val token: String
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)
```

### 3. Network Service

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/network/NetworkService.kt
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import java.util.concurrent.TimeUnit

class NetworkService(private val context: Context) {
    companion object {
        private const val BASE_URL = "https://api.serra.com/v1/"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) 
                HttpLoggingInterceptor.Level.BODY 
            else 
                HttpLoggingInterceptor.Level.NONE
        })
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}
```

### 4. Remote Auth Service

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/auth/RemoteAuthService.kt
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteAuthService(private val context: Context) {
    private val networkService = NetworkService(context)
    
    companion object {
        private const val TAG = "RemoteAuthService"
    }
    
    suspend fun authenticate(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Remote authentication for: $email")
                
                val deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                
                val response = networkService.authApi.login(
                    LoginRequest(
                        email = email,
                        password = password,
                        deviceId = deviceId
                    )
                )
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    
                    if (authResponse?.success == true && authResponse.user != null) {
                        // Salva token per future chiamate
                        saveAuthToken(authResponse.token)
                        
                        Log.i(TAG, "Remote authentication successful for: $email")
                        AuthResult.Success(
                            User(
                                id = authResponse.user.id,
                                email = authResponse.user.email,
                                passwordHash = "", // Non memorizzare password
                                role = authResponse.user.role
                            )
                        )
                    } else {
                        Log.w(TAG, "Remote authentication failed: ${authResponse?.message}")
                        AuthResult.Error(authResponse?.message ?: "Credenziali non valide")
                    }
                } else {
                    Log.w(TAG, "HTTP error: ${response.code()}")
                    AuthResult.Error("Errore del server")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Remote authentication error", e)
                AuthResult.Error("Errore di connessione")
            }
        }
    }
    
    private fun saveAuthToken(token: String?) {
        if (token != null) {
            val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString("auth_token", token)
                apply()
            }
        }
    }
}
```

---

## API di Autenticazione

### 1. JWT Token Management

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/auth/TokenManager.kt
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import org.json.JSONObject

class TokenManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val TOKEN_KEY = "auth_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveTokens(accessToken: String, refreshToken: String? = null) {
        with(encryptedPrefs.edit()) {
            putString(TOKEN_KEY, accessToken)
            refreshToken?.let { putString(REFRESH_TOKEN_KEY, it) }
            apply()
        }
    }
    
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(TOKEN_KEY, null)
    }
    
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
    }
    
    fun clearTokens() {
        with(encryptedPrefs.edit()) {
            remove(TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            apply()
        }
    }
    
    fun isTokenValid(): Boolean {
        val token = getAccessToken() ?: return false
        
        try {
            // Decodifica JWT (parte payload)
            val parts = token.split(".")
            if (parts.size != 3) return false
            
            val payload = String(Base64.decode(parts[1], Base64.DEFAULT))
            val json = JSONObject(payload)
            
            // Controlla scadenza
            val exp = json.getLong("exp")
            val now = System.currentTimeMillis() / 1000
            
            return exp > now
        } catch (e: Exception) {
            return false
        }
    }
}
```

### 2. Interceptor per Autenticazione

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/network/AuthInterceptor.kt
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = tokenManager.getAccessToken()
        
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}
```

---

## Sicurezza e Crittografia

### 1. Password Hashing

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/security/PasswordUtils.kt
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.regex.Pattern

object PasswordUtils {
    private const val BCRYPT_ROUNDS = 12
    private const val MIN_LENGTH = 8
    
    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    )
    
    /**
     * Hasha una password usando BCrypt
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
    }
    
    /**
     * Verifica una password contro il suo hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
    
    /**
     * Valida la forza della password
     */
    fun validatePassword(password: String): PasswordValidationResult {
        return when {
            password.length < MIN_LENGTH -> 
                PasswordValidationResult.Error("Password deve essere almeno $MIN_LENGTH caratteri")
            
            !PASSWORD_PATTERN.matcher(password).matches() ->
                PasswordValidationResult.Error("Password deve contenere almeno: 1 maiuscola, 1 minuscola, 1 numero e 1 carattere speciale")
            
            else -> PasswordValidationResult.Valid
        }
    }
    
    /**
     * Genera una password sicura
     */
    fun generateSecurePassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&+="
        val random = SecureRandom()
        
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}

sealed class PasswordValidationResult {
    object Valid : PasswordValidationResult()
    data class Error(val message: String) : PasswordValidationResult()
}
```

### 2. Encryption Utils

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/security/EncryptionUtils.kt
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.security.SecureRandom

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    
    /**
     * Genera una chiave AES
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
    
    /**
     * Cripta dati sensibili
     */
    fun encrypt(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        
        val encryptedData = cipher.doFinal(data.toByteArray())
        val encryptedWithIv = iv + encryptedData
        
        return Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
    }
    
    /**
     * Decripta dati sensibili
     */
    fun decrypt(encryptedData: String, key: SecretKey): String {
        val encryptedWithIv = Base64.decode(encryptedData, Base64.DEFAULT)
        
        val iv = encryptedWithIv.sliceArray(0 until GCM_IV_LENGTH)
        val encrypted = encryptedWithIv.sliceArray(GCM_IV_LENGTH until encryptedWithIv.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val decryptedData = cipher.doFinal(encrypted)
        return String(decryptedData)
    }
}
```

### 3. Biometric Authentication

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/auth/BiometricAuthManager.kt
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val activity: FragmentActivity) {
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticazione Biometrica")
            .setSubtitle("Usa la tua impronta o volto per accedere")
            .setNegativeButtonText("Annulla")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}
```

---

## Gestione Errori e Logging

### 1. Logging Sicuro

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/utils/SecureLogger.kt
import android.util.Log
import java.util.regex.Pattern

object SecureLogger {
    private const val TAG = "SerraApp"
    
    // Pattern per dati sensibili
    private val sensitivePatterns = listOf(
        Pattern.compile("password", Pattern.CASE_INSENSITIVE),
        Pattern.compile("token", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
        Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b") // Carte di credito
    )
    
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, sanitizeMessage(message))
        }
    }
    
    fun i(message: String) {
        Log.i(TAG, sanitizeMessage(message))
    }
    
    fun w(message: String) {
        Log.w(TAG, sanitizeMessage(message))
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, sanitizeMessage(message), throwable)
    }
    
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        sensitivePatterns.forEach { pattern ->
            sanitized = pattern.matcher(sanitized).replaceAll("***MASKED***")
        }
        
        return sanitized
    }
}
```

### 2. Error Handler

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/error/ErrorHandler.kt
import android.content.Context
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandler(private val context: Context) {
    
    fun handleError(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "Errore di connessione. Verifica la tua connessione internet."
            is SocketTimeoutException -> "Timeout della richiesta. Riprova più tardi."
            is ConnectException -> "Impossibile connettersi al server."
            else -> "Si è verificato un errore imprevisto."
        }
    }
    
    fun logError(error: Throwable, context: String = "") {
        SecureLogger.e("Error in $context: ${error.message}", error)
    }
}
```

---

## Best Practices di Produzione

### 1. Configurazione Sicura

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/config/SecurityConfig.kt
object SecurityConfig {
    
    // Configurazione per produzione
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minuti
    const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 ore
    const val TOKEN_REFRESH_THRESHOLD_MS = 5 * 60 * 1000L // 5 minuti
    
    // Headers di sicurezza
    const val HEADER_USER_AGENT = "SerraApp/1.0 (Android)"
    const val HEADER_ACCEPT = "application/json"
    const val HEADER_CONTENT_TYPE = "application/json"
    
    // Timeout di rete
    const val CONNECT_TIMEOUT_S = 10L
    const val READ_TIMEOUT_S = 30L
    const val WRITE_TIMEOUT_S = 30L
    
    // Configurazione database
    const val DB_NAME = "serra_secure.db"
    const val DB_VERSION = 1
    
    // Chiavi SharedPreferences
    const val PREF_NAME = "serra_secure_prefs"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_LOGIN_TIME = "login_time"
    const val PREF_LOGIN_ATTEMPTS = "login_attempts"
    const val PREF_LOCKOUT_TIME = "lockout_time"
}
```

### 2. Rate Limiting

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/security/RateLimiter.kt
import android.content.Context

class RateLimiter(private val context: Context) {
    private val prefs = context.getSharedPreferences("rate_limiter", Context.MODE_PRIVATE)
    
    fun isLocked(identifier: String): Boolean {
        val attempts = prefs.getInt("attempts_$identifier", 0)
        val lockoutTime = prefs.getLong("lockout_$identifier", 0)
        val currentTime = System.currentTimeMillis()
        
        return if (attempts >= SecurityConfig.MAX_LOGIN_ATTEMPTS) {
            if (currentTime - lockoutTime < SecurityConfig.LOCKOUT_DURATION_MS) {
                true // Ancora bloccato
            } else {
                // Reset tentativi dopo lockout
                resetAttempts(identifier)
                false
            }
        } else {
            false
        }
    }
    
    fun incrementAttempts(identifier: String) {
        val attempts = prefs.getInt("attempts_$identifier", 0) + 1
        
        with(prefs.edit()) {
            putInt("attempts_$identifier", attempts)
            
            if (attempts >= SecurityConfig.MAX_LOGIN_ATTEMPTS) {
                putLong("lockout_$identifier", System.currentTimeMillis())
            }
            
            apply()
        }
    }
    
    fun resetAttempts(identifier: String) {
        with(prefs.edit()) {
            remove("attempts_$identifier")
            remove("lockout_$identifier")
            apply()
        }
    }
    
    fun getRemainingLockoutTime(identifier: String): Long {
        val lockoutTime = prefs.getLong("lockout_$identifier", 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lockoutTime
        
        return maxOf(0, SecurityConfig.LOCKOUT_DURATION_MS - elapsed)
    }
}
```

### 3. MainActivity Finale con Sicurezza

```kotlin
// File: app/src/main/java/com/example/webviewgrafana/MainActivity.kt (Versione Sicura)
class MainActivity : AppCompatActivity() {
    
    private lateinit var authService: AuthService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var errorHandler: ErrorHandler
    private lateinit var biometricAuthManager: BiometricAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializzazione servizi sicuri
        authService = AuthService(this)
        rateLimiter = RateLimiter(this)
        errorHandler = ErrorHandler(this)
        biometricAuthManager = BiometricAuthManager(this)
        
        // Controllo sicurezza all'avvio
        if (isDeviceSecure()) {
            checkLoginStatus()
        } else {
            showSecurityAlert()
        }
    }
    
    private inner class WebAppInterface {
        
        @JavascriptInterface
        fun performLogin(email: String, password: String): String {
            val sanitizedEmail = email.trim().lowercase()
            
            // Controllo rate limiting
            if (rateLimiter.isLocked(sanitizedEmail)) {
                val remainingTime = rateLimiter.getRemainingLockoutTime(sanitizedEmail)
                val minutes = remainingTime / 60000
                
                runOnUiThread {
                    webView.evaluateJavascript(
                        "setLoginStatus('error', 'Account bloccato per $minutes minuti')",
                        null
                    )
                }
                return "locked"
            }
            
            // Validazione password
            val passwordValidation = PasswordUtils.validatePassword(password)
            if (passwordValidation is PasswordValidationResult.Error) {
                runOnUiThread {
                    webView.evaluateJavascript(
                        "setLoginStatus('error', '${passwordValidation.message}')",
                        null
                    )
                }
                return "invalid_password"
            }
            
            // Autenticazione asincrona
            mainScope.launch {
                try {
                    val authResult = authService.authenticate(sanitizedEmail, password)
                    
                    when (authResult) {
                        is AuthResult.Success -> {
                            // Reset tentativi su successo
                            rateLimiter.resetAttempts(sanitizedEmail)
                            
                            // Salva sessione sicura
                            saveSecureSession(authResult.user)
                            
                            // Notifica successo
                            runOnUiThread {
                                webView.evaluateJavascript(
                                    "setLoginStatus('success', 'Accesso effettuato con successo')",
                                    null
                                )
                            }
                        }
                        
                        is AuthResult.Error -> {
                            // Incrementa tentativi falliti
                            rateLimiter.incrementAttempts(sanitizedEmail)
                            
                            // Log errore sicuro
                            errorHandler.logError(
                                Exception("Failed login for: $sanitizedEmail"),
                                "Authentication"
                            )
                            
                            runOnUiThread {
                                webView.evaluateJavascript(
                                    "setLoginStatus('error', '${authResult.message}')",
                                    null
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorHandler.logError(e, "performLogin")
                    
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "setLoginStatus('error', 'Errore del sistema')",
                            null
                        )
                    }
                }
            }
            
            return "processing"
        }
        
        @JavascriptInterface
        fun enableBiometric() {
            if (biometricAuthManager.isBiometricAvailable()) {
                biometricAuthManager.authenticate(
                    onSuccess = {
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "setLoginStatus('success', 'Autenticazione biometrica riuscita')",
                                null
                            )
                        }
                    },
                    onError = { error ->
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "setLoginStatus('error', 'Errore biometrico: $error')",
                                null
                            )
                        }
                    }
                )
            } else {
                runOnUiThread {
                    webView.evaluateJavascript(
                        "setLoginStatus('error', 'Autenticazione biometrica non disponibile')",
                        null
                    )
                }
            }
        }
    }
    
    private fun saveSecureSession(user: User) {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            this,
            SecurityConfig.PREF_NAME,
            MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        with(encryptedPrefs.edit()) {
            putString(SecurityConfig.PREF_USER_EMAIL, user.email)
            putString(SecurityConfig.PREF_USER_ROLE, user.role)
            putLong(SecurityConfig.PREF_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }
    
    private fun isDeviceSecure(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceSecure
    }
    
    private fun showSecurityAlert() {
        runOnUiThread {
            webView.evaluateJavascript(
                "showMessage('Dispositivo non sicuro. Imposta un blocco schermo per continuare.', 'error')",
                null
            )
        }
    }
}
```

---

## Checklist di Sicurezza per Produzione

### ✅ **Autenticazione**
- [ ] Password hashate con BCrypt (rounds >= 12)
- [ ] Rate limiting implementato
- [ ] Lockout temporaneo dopo tentativi falliti
- [ ] Validazione password forte obbligatoria
- [ ] Autenticazione biometrica opzionale

### ✅ **Autorizzazione**
- [ ] Controllo ruoli utente
- [ ] Sessioni con timeout automatico
- [ ] Token JWT con scadenza
- [ ] Refresh token sicuri

### ✅ **Crittografia**
- [ ] EncryptedSharedPreferences per dati sensibili
- [ ] HTTPS obbligatorio per API
- [ ] Certificate pinning implementato
- [ ] Dati sensibili mai nei log

### ✅ **Sicurezza Dispositivo**
- [ ] Controllo root/jailbreak
- [ ] Blocco schermo obbligatorio
- [ ] Controllo integrità app
- [ ] Obfuscazione codice

### ✅ **Monitoraggio**
- [ ] Logging sicuro implementato
- [ ] Alerting per tentativi di accesso
- [ ] Audit trail completo
- [ ] Monitoring performance

---

## Conclusioni

Questo documento fornisce una guida completa per implementare un sistema di autenticazione sicuro in produzione. Le implementazioni mostrate garantiscono:

1. **Sicurezza**: Crittografia, hashing, rate limiting
2. **Scalabilità**: Supporto database locale e remoto
3. **Usabilità**: Autenticazione biometrica, sessioni persistenti
4. **Conformità**: Best practices di sicurezza
5. **Manutenibilità**: Codice ben strutturato e documentato

**Prossimo Step**: Implementare gradualmente le funzionalità, iniziando dal database locale e aggiungendo progressivamente le funzionalità di sicurezza avanzate. 