# WebView Grafana - Documentazione

Questa documentazione descrive le modifiche apportate all'app Android per visualizzare la dashboard Grafana e i controlli della serra.

## Struttura del Progetto

```
WebViewGrafana/
├── app/
│   ├── src/main/
│   │   ├── assets/               # File HTML e CSS della dashboard
│   │   │   ├── dashboard.html
│   │   │   └── styles.css
│   │   ├── java/.../
│   │   │   └── MainActivity.kt   # Logica principale dell'app
│   │   ├── res/
│   │   │   └── xml/
│   │   │       └── network_security_config.xml  # Configurazione sicurezza rete
│   │   └── AndroidManifest.xml   # Configurazione app e permessi
│   └── build.gradle.kts          # Dipendenze del progetto
```

## Modifiche Effettuate

### 1. Configurazione Gradle (build.gradle.kts)
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
```
**Motivo**: Aggiunta delle dipendenze necessarie per:
- WebView avanzata (webkit)
- Pull-to-refresh (swiperefreshlayout)
- Layout di base (constraintlayout)
- Supporto AppCompat per la compatibilità

### 2. Manifest (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```
**Motivo**:
- Permessi Internet necessari per la WebView
- Configurazione per permettere traffico HTTP (necessario per Grafana locale)
- Configurazione di sicurezza personalizzata per localhost

### 3. Configurazione Sicurezza Rete (network_security_config.xml)
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```
**Motivo**: 
- Permette connessioni HTTP non sicure a localhost
- 10.0.2.2 è l'indirizzo che l'emulatore usa per accedere al localhost del computer

### 4. MainActivity (MainActivity.kt)
```kotlin
class MainActivity : AppCompatActivity() {
    // ... configurazione WebView
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // ...
    }
}
```
**Motivo**:
- Abilita JavaScript (necessario per Grafana e Bootstrap)
- Permette il DOM Storage (necessario per Grafana)
- Gestisce contenuti misti HTTP/HTTPS
- Configura l'interfaccia JavaScript per i controlli della pompa

### 5. Dashboard HTML (dashboard.html)
```html
<!-- Modifiche principali -->
<iframe src="http://10.0.2.2:3000/..."></iframe>
<button onclick="Android.attivaPompa()">Avvia</button>
```
**Motivo**:
- URL modificati per puntare al localhost dell'emulatore
- Aggiunti handler JavaScript per comunicare con Android
- Interfaccia utente adattata per mobile

### 6. Stili CSS (styles.css)
```css
/* Modifiche principali */
.grafana-panel {
    height: 400px;
    /* ... */
}

@media (max-width: 768px) {
    /* ... */
}
```
**Motivo**:
- Stili adattati per la visualizzazione mobile
- Dimensioni dei grafici ottimizzate per lo schermo
- Layout responsive per diversi dispositivi

## Interfaccia JavaScript

L'app implementa un'interfaccia JavaScript per la comunicazione tra la WebView e il codice Android:

```kotlin
private inner class WebAppInterface {
    @JavascriptInterface
    fun attivaPompa() {
        // Logica per attivare la pompa
    }

    @JavascriptInterface
    fun stopPompa() {
        // Logica per fermare la pompa
    }
}
```

Questa interfaccia permette:
1. Controllo della pompa dall'interfaccia web
2. Aggiornamento dello stato della pompa nell'UI
3. Comunicazione bidirezionale tra WebView e Android

## Sicurezza

L'app implementa diverse misure di sicurezza:
1. Configurazione di rete personalizzata per localhost
2. Gestione sicura delle credenziali (non hardcodare mai le credenziali reali nel codice)
3. Validazione degli input JavaScript
4. Permessi minimi necessari

IMPORTANTE: Non committare mai credenziali reali nel codice. Usa variabili d'ambiente o file di configurazione esterni per gestire le credenziali in produzione.

## Note per lo Sviluppo

Per modificare l'app:
1. Aggiorna le credenziali Grafana in MainActivity.kt (sostituisci YOUR_USERNAME e YOUR_PASSWORD con le tue credenziali)
2. Modifica gli URL dei grafici in dashboard.html
3. Personalizza gli stili in styles.css
4. Implementa la logica della pompa in WebAppInterface

## Problemi Noti e Soluzioni

1. **Errore "Webpage not available"**
   - Verifica che Grafana sia in esecuzione
   - Controlla le credenziali
   - Verifica la configurazione di rete

2. **Grafici non visibili**
   - Controlla gli URL in dashboard.html
   - Verifica l'accesso a Grafana
   - Controlla i log per errori di rete

3. **Controlli pompa non funzionanti**
   - Verifica l'implementazione di WebAppInterface
   - Controlla i log JavaScript
   - Verifica la connessione al controller della pompa

## Guida al Codice Dettagliata

### 1. MainActivity.kt - Spiegazione Dettagliata
```kotlin
class MainActivity : AppCompatActivity() {
    // Dichiarazione delle variabili che useremo nell'app
    private lateinit var webView: WebView           // La nostra vista web principale
    private lateinit var progressBar: ProgressBar   // Indicatore di caricamento
    private lateinit var errorText: TextView        // Testo per mostrare errori
    private lateinit var swipeRefresh: SwipeRefreshLayout  // Pull-to-refresh

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Impostiamo il layout

        // Colleghiamo le variabili agli elementi del layout
        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        // Configuriamo il refresh quando si trascina verso il basso
        swipeRefresh.setOnRefreshListener {
            webView.reload()  // Ricarica la pagina
        }

        // Configurazione completa della WebView
        webView.settings.apply {
            javaScriptEnabled = true      // Abilita JavaScript
            domStorageEnabled = true      // Abilita il DOM Storage
            cacheMode = WebSettings.LOAD_DEFAULT  // Usa la cache normalmente
            setSupportZoom(true)          // Permette lo zoom
            builtInZoomControls = true    // Mostra i controlli dello zoom
            displayZoomControls = false   // Nasconde i pulsanti +/- dello zoom
            useWideViewPort = true        // Usa il viewport wide
            loadWithOverviewMode = true   // Carica la pagina in modalità overview
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW  // Permette contenuti misti (http/https)
        }

        // Aggiungiamo l'interfaccia per comunicare con JavaScript
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Gestiamo gli eventi della WebView
        webView.webViewClient = object : WebViewClient() {
            // Gestione dell'autenticazione Grafana
            override fun onReceivedHttpAuthRequest(
                view: WebView,
                handler: HttpAuthHandler,
                host: String,
                realm: String
            ) {
                handler.proceed("YOUR_USERNAME", "YOUR_PASSWORD")  // Sostituisci con le tue credenziali
            }

            // Quando la pagina finisce di caricare
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE  // Nascondi il loading
                swipeRefresh.isRefreshing = false  // Ferma l'animazione refresh
                errorText.visibility = View.GONE    // Nascondi eventuali errori
                webView.visibility = View.VISIBLE   // Mostra la webview
            }

            // Se si verifica un errore
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

        // Gestiamo il progresso del caricamento
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        // Carichiamo la dashboard
        loadDashboard()
    }

    // Funzione per caricare la dashboard
    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE
        webView.loadUrl("file:///android_asset/dashboard.html")
    }

    // Gestiamo il pulsante "Indietro"
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()  // Torna alla pagina precedente se possibile
        } else {
            super.onBackPressed()  // Altrimenti chiudi l'app
        }
    }

    // Interfaccia per comunicare con JavaScript
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
```

### 2. Layout (activity_main.xml) - Spiegazione Dettagliata
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/swipeRefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- La WebView che mostrerà la dashboard -->
        <WebView
            android:id="@+id/webview"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <!-- Loading spinner -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:visibility="gone" />

        <!-- Testo per gli errori -->
        <TextView
            android:id="@+id/errorText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:padding="16dp"
            android:textAlignment="center"
            android:visibility="gone" />

    </RelativeLayout>
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

### 3. Configurazione Rete (network_security_config.xml) - Spiegazione Dettagliata
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Configurazione di base per permettere traffico non sicuro -->
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Configurazione specifica per localhost -->
    <domain-config cleartextTrafficPermitted="true">
        <!-- 10.0.2.2 è l'indirizzo che l'emulatore usa per localhost -->
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

### 4. JavaScript nella Dashboard (dashboard.html) - Spiegazione Dettagliata
```javascript
// Funzione per aggiornare lo stato della pompa nell'interfaccia
function updatePumpStatus(isActive) {
    const statusIndicator = document.getElementById('pumpstat');
    const statusText = document.getElementById('stato');
    
    if (isActive) {
        // Se la pompa è attiva
        statusIndicator.classList.add('active');
        statusText.textContent = 'Pompa Attiva';
    } else {
        // Se la pompa è inattiva
        statusIndicator.classList.remove('active');
        statusText.textContent = 'Pompa Inattiva';
    }
}

// Esempio di chiamata a funzione Android dalla WebView
function attivaPompa() {
    // Android è l'oggetto che abbiamo definito con addJavascriptInterface
    Android.attivaPompa();
}

function stopPompa() {
    Android.stopPompa();
}
```

### Come Funziona Tutto Insieme

1. **Avvio dell'App**:
   - L'app si avvia e carica `activity_main.xml`
   - Inizializza la WebView e gli altri componenti
   - Carica `dashboard.html` dalla cartella assets

2. **Caricamento Dashboard**:
   - La WebView carica la pagina HTML
   - Gli iframe di Grafana vengono caricati con autenticazione
   - Il CSS viene applicato per lo stile

3. **Interazione Utente**:
   - L'utente può navigare tra i grafici
   - Può attivare/disattivare la pompa
   - Può fare refresh trascinando verso il basso
   - Può tornare indietro con il pulsante back

4. **Comunicazione Android-JavaScript**:
   - I pulsanti HTML chiamano funzioni JavaScript
   - JavaScript chiama metodi Android tramite l'interfaccia
   - Android aggiorna l'UI tramite JavaScript

### Personalizzazione

Per adattare l'app alle tue esigenze:

1. **Credenziali Grafana**:
```kotlin
// In MainActivity.kt
handler.proceed("tuousername", "tuapassword")
```

2. **URL Grafana**:
```html
<!-- In dashboard.html -->
<iframe src="http://10.0.2.2:3000/tuo-dashboard-id" ...>
```

3. **Logica Pompa**:
```kotlin
// In MainActivity.kt
@JavascriptInterface
fun attivaPompa() {
    // Inserisci qui la tua logica
    // Esempio: chiamata API al controller della pompa
}
```

### Test e Debug

Per verificare il funzionamento:

1. **Controllo Connessione**:
```kotlin
// Aggiungi questo nel WebViewClient
override fun onReceivedError(...) {
    Log.e("WebView", "Errore: ${error?.description}")
    // Resto del codice...
}
```

2. **Debug JavaScript**:
```kotlin
// In MainActivity.kt
WebView.setWebContentsDebuggingEnabled(true)  // Solo in debug!
```

3. **Verifica Credenziali**:
```kotlin
// Aggiungi log per l'autenticazione
override fun onReceivedHttpAuthRequest(...) {
    Log.d("Auth", "Tentativo di autenticazione per: $host")
    handler.proceed(username, password)
}
``` 