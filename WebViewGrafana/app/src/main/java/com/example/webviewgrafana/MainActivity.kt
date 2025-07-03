package com.example.webviewgrafana

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var sharedPreferences: SharedPreferences
    
    // Variabili per gestire lo stato di login
    private var isLoggedIn = false
    private var currentUser: String? = null

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inizializzazione SharedPreferences
        sharedPreferences = getSharedPreferences("SerraApp", Context.MODE_PRIVATE)

        // Inizializzazione delle views
        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        // Configurazione SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        // Configurazione WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Aggiungi l'interfaccia JavaScript
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Gestione degli eventi della WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                errorText.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                webView.visibility = View.GONE
                errorText.apply {
                    visibility = View.VISIBLE
                    text = "Errore di caricamento. Controlla la connessione e riprova."
                }
            }
        }

        // Gestione del caricamento
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        // Controllo stato login e caricamento iniziale
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        // Controlla se l'utente è già loggato
        val savedEmail = sharedPreferences.getString("user_email", null)
        val loginTime = sharedPreferences.getLong("login_time", 0)
        val currentTime = System.currentTimeMillis()
        
        // Considera la sessione valida per 24 ore
        val sessionValid = (currentTime - loginTime) < (24 * 60 * 60 * 1000)
        
        if (savedEmail != null && sessionValid) {
            isLoggedIn = true
            currentUser = savedEmail
            loadDashboard()
        } else {
            loadLoginPage()
        }
    }

    private fun loadLoginPage() {
        progressBar.visibility = View.VISIBLE
        webView.loadUrl("file:///android_asset/login.html")
    }

    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE
        webView.loadUrl("file:///android_asset/dashboard.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Interfaccia JavaScript per comunicare con la WebView
    private inner class WebAppInterface {
        
        // ===== METODI PER CONTROLLO POMPA =====
        @JavascriptInterface
        fun attivaPompa() {
            // Qui inseriremo la logica per attivare la pompa
            runOnUiThread {
                webView.evaluateJavascript("updatePumpStatus(true)", null)
            }
        }

        @JavascriptInterface
        fun stopPompa() {
            // Qui inseriremo la logica per fermare la pompa
            runOnUiThread {
                webView.evaluateJavascript("updatePumpStatus(false)", null)
            }
        }
        
        // ===== METODI PER GESTIONE LOGIN =====
        @JavascriptInterface
        fun performLogin(email: String, password: String): String {
            // Credenziali di test (in produzione utilizzare un sistema di autenticazione sicuro)
            val validCredentials = mapOf(
                "admin@serra.com" to "password",
                "user@serra.com" to "user123",
                "manager@serra.com" to "manager456"
            )
            
            return if (validCredentials[email] == password) {
                // Login valido
                isLoggedIn = true
                currentUser = email
                
                // Salva le credenziali
                saveUserSession(email)
                
                // Notifica il successo del login
                runOnUiThread {
                    webView.evaluateJavascript("setLoginStatus('success', 'Login effettuato con successo!')", null)
                }
                
                "success"
            } else {
                // Login non valido
                runOnUiThread {
                    webView.evaluateJavascript("setLoginStatus('error', 'Credenziali non valide')", null)
                }
                "error"
            }
        }
        
        @JavascriptInterface
        fun checkSavedCredentials(): String {
            val savedEmail = sharedPreferences.getString("user_email", null)
            return if (savedEmail != null) {
                JSONObject().apply {
                    put("email", savedEmail)
                }.toString()
            } else {
                ""
            }
        }
        
        @JavascriptInterface
        fun logout() {
            isLoggedIn = false
            currentUser = null
            clearUserSession()
            
            runOnUiThread {
                loadLoginPage()
            }
        }
        
        @JavascriptInterface
        fun forgotPassword() {
            runOnUiThread {
                webView.evaluateJavascript(
                    "showMessage('Controlla la tua email per istruzioni sul reset della password.', 'info')", 
                    null
                )
            }
        }
        
        @JavascriptInterface
        fun getCurrentUser(): String {
            return currentUser ?: ""
        }
        
        @JavascriptInterface
        fun isUserLoggedIn(): Boolean {
            return isLoggedIn
        }
        
        // ===== METODI PER NAVIGAZIONE =====
        @JavascriptInterface
        fun navigateToDashboard() {
            if (isLoggedIn) {
                runOnUiThread {
                    loadDashboard()
                }
            } else {
                runOnUiThread {
                    webView.evaluateJavascript("showMessage('Devi effettuare il login prima.', 'warning')", null)
                }
            }
        }
        
        @JavascriptInterface
        fun navigateToLogin() {
            runOnUiThread {
                loadLoginPage()
            }
        }
    }
    
    // ===== METODI PRIVATI PER GESTIONE SESSIONE =====
    private fun saveUserSession(email: String) {
        with(sharedPreferences.edit()) {
            putString("user_email", email)
            putLong("login_time", System.currentTimeMillis())
            apply()
        }
    }
    
    private fun clearUserSession() {
        with(sharedPreferences.edit()) {
            remove("user_email")
            remove("login_time")
            apply()
        }
    }
}



