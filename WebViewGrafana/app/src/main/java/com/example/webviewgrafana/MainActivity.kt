package com.example.webviewgrafana

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        // Caricamento iniziale
        loadDashboard()
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
    }
}



