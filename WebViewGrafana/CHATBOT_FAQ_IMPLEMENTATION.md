# 🤖 **CHATBOT FAQ IMPLEMENTATION GUIDE**
*Implementazione Chatbot FAQ per Progetto Serra*

---

## 📋 **ANALISI ESERCIZIO NEL CONTESTO SERRA**

### **Obiettivo dell'Esercizio:**
Sviluppare un chatbot intelligente FAQ utilizzando:
- ✅ **Interfaccia Python** (Tkinter/terminale)
- ✅ **Vector Store** (Weaviate) per recupero semantico
- ✅ **PostgreSQL** per dati strutturati
- ✅ **AI Models** (Hugging Face/OpenAI) opzionale

### **Integrazione con Progetto Serra:**
Il chatbot può essere integrato come **sistema di supporto** per l'app Serra, fornendo assistenza agli utenti su:
- 🌱 **FAQ Serra**: Domande su sensori, irrigazione, monitoraggio
- 👤 **Supporto Utente**: Aiuto con login, dashboard, problemi tecnici
- 📊 **Spiegazione Dati**: Interpretazione grafici Grafana, soglie, allarmi
- 🔧 **Troubleshooting**: Risoluzione problemi comuni

---

## 🏗️ **ARCHITETTURA PROPOSTA**

### **Architettura Integrata:**
```
[App Android Serra] ←→ [WebView] ←→ [Chatbot Web Interface]
                                           ↓
[Python Chatbot Service] ←→ [Weaviate] (FAQ Semantiche)
                         ←→ [PostgreSQL] (Dati Utenti + Serra)
                         ←→ [Grafana API] (Dati Real-time)
```

### **Componenti da Sviluppare:**
1. **🐍 Python Chatbot Service**: Backend principale
2. **🌐 Web Interface**: Interfaccia web per Android WebView
3. **📊 FAQ Database**: Knowledge base specifica Serra
4. **🔗 Integration Layer**: Connessione con app esistente

---

## 📂 **STRUTTURA PROGETTO PROPOSTA**

```
ProgettoSerraApp/
├── WebViewGrafana/                     # App Android esistente
└── serra-chatbot/                      # NUOVO: Chatbot Service
    ├── app/
    │   ├── main.py                     # Flask web server
    │   ├── chatbot_service.py          # Logica chatbot
    │   ├── weaviate_client.py          # Vector DB client
    │   ├── postgres_client.py          # Database client
    │   └── config.py                   # Configurazione
    ├── data/
    │   ├── serra_faq.txt               # FAQ specifiche Serra
    │   ├── user_data.sql               # Schema utenti
    │   └── sample_data.sql             # Dati di esempio
    ├── web/
    │   ├── templates/
    │   │   └── chatbot.html           # Interfaccia web
    │   ├── static/
    │   │   ├── chatbot.js             # JavaScript client
    │   │   └── chatbot.css            # Stili
    ├── tests/
    │   └── test_chatbot.py             # Test unitari
    ├── requirements.txt                # Dipendenze Python
    └── README.md                       # Documentazione
```

---

## 📊 **DATASET FAQ SERRA - ESEMPI**

### **File `serra_faq.txt`:**
```
Domanda: Come funziona il sistema di irrigazione automatica?
Risposta: Il sistema monitora l'umidità del terreno tramite sensori e attiva automaticamente la pompa quando i valori scendono sotto la soglia impostata (30%). Puoi controllare manualmente la pompa dal dashboard.
Tag: irrigazione, pompa, umidità
Modulo: controllo_irrigazione

Domanda: Perché la temperatura della serra è troppo alta?
Risposta: Temperature elevate possono essere causate da: esposizione solare diretta, ventilazione insufficiente, o malfunzionamento dei sensori. Controlla i grafici delle ultime 24 ore e verifica le soglie di allarme.
Tag: temperatura, allarme, sensori
Modulo: monitoraggio_temperatura

Domanda: Come faccio il login nell'app?
Risposta: Usa le credenziali fornite: admin@serra.com (password: password) per accesso amministratore, o user@serra.com (password: user123) per accesso utente standard.
Tag: login, autenticazione, credenziali
Modulo: sistema_utenti

Domanda: Cosa significano i grafici di Grafana?
Risposta: I grafici mostrano i dati dei sensori in tempo reale. Temperatura (rosso), umidità terreno (blu), livello acqua (verde). Le linee tratteggiate indicano le soglie di allarme.
Tag: grafici, grafana, interpretazione
Modulo: dashboard_analisi

Domanda: La pompa non si attiva automaticamente, cosa devo fare?
Risposta: Controlla: 1) Livello acqua nel serbatoio, 2) Sensori umidità funzionanti, 3) Soglia automatica impostata correttamente, 4) Connessione elettrica pompa. Prova l'attivazione manuale dal dashboard.
Tag: pompa, troubleshooting, automazione
Modulo: controllo_irrigazione
```

---

## 🔧 **IMPLEMENTAZIONE DETTAGLIATA**

### **1. Setup Ambiente Python**

#### **requirements.txt:**
```txt
flask==2.3.3
weaviate-client==4.4.0
psycopg2-binary==2.9.7
sentence-transformers==2.2.2
python-dotenv==1.0.0
requests==2.31.0
flask-cors==4.0.0
```

#### **config.py:**
```python
import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # Weaviate Configuration
    WEAVIATE_URL = os.getenv("WEAVIATE_URL", "http://localhost:8080")
    WEAVIATE_API_KEY = os.getenv("WEAVIATE_API_KEY", "")
    
    # PostgreSQL Configuration
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_PORT = os.getenv("DB_PORT", "5432")
    DB_NAME = os.getenv("DB_NAME", "serra_db")
    DB_USER = os.getenv("DB_USER", "postgres")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "password")
    
    # Flask Configuration
    FLASK_HOST = os.getenv("FLASK_HOST", "0.0.0.0")
    FLASK_PORT = int(os.getenv("FLASK_PORT", "5000"))
    
    # Grafana Integration
    GRAFANA_URL = os.getenv("GRAFANA_URL", "http://localhost:3000")
    GRAFANA_API_KEY = os.getenv("GRAFANA_API_KEY", "")
```

### **2. Servizio Chatbot Principale**

#### **chatbot_service.py:**
```python
import weaviate
from sentence_transformers import SentenceTransformer
import psycopg2
from psycopg2.extras import RealDictCursor
import json
import re

class SerraChatbot:
    def __init__(self):
        self.weaviate_client = weaviate.Client("http://localhost:8080")
        self.model = SentenceTransformer('all-MiniLM-L6-v2')
        self.confidence_threshold = 0.7
        self.setup_database()
    
    def setup_database(self):
        """Configura connessione PostgreSQL"""
        try:
            self.pg_conn = psycopg2.connect(
                host="localhost",
                database="serra_db",
                user="postgres",
                password="password"
            )
            print("✅ Connessione PostgreSQL stabilita")
        except Exception as e:
            print(f"❌ Errore connessione PostgreSQL: {e}")
    
    def process_question(self, question, user_email=None):
        """Processa una domanda e genera risposta"""
        try:
            # 1. Ricerca semantica FAQ
            faq_results = self.search_faq(question)
            
            # 2. Recupera info utente se disponibile
            user_info = self.get_user_info(user_email) if user_email else None
            
            # 3. Genera risposta combinata
            response = self.generate_response(question, faq_results, user_info)
            
            # 4. Log interazione
            if user_email and faq_results:
                self.log_interaction(user_email, question, response['answer'])
            
            return response
            
        except Exception as e:
            return {
                'answer': f"Mi dispiace, ho riscontrato un errore: {str(e)}",
                'confidence': 0,
                'sources': []
            }
    
    def search_faq(self, question):
        """Cerca FAQ simili usando ricerca semantica"""
        try:
            result = (
                self.weaviate_client.query
                .get("SerraFAQ", ["question", "answer", "tags", "module"])
                .with_near_text({"concepts": [question]})
                .with_limit(3)
                .with_additional(["certainty"])
                .do()
            )
            
            if result["data"]["Get"]["SerraFAQ"]:
                return result["data"]["Get"]["SerraFAQ"]
            else:
                return []
                
        except Exception as e:
            print(f"❌ Errore ricerca FAQ: {e}")
            return []
    
    def get_user_info(self, email):
        """Recupera informazioni utente da PostgreSQL"""
        try:
            with self.pg_conn.cursor(cursor_factory=RealDictCursor) as cursor:
                cursor.execute("""
                    SELECT nome, email, ruolo, ultimo_accesso
                    FROM utenti_serra 
                    WHERE email = %s
                """, (email,))
                return cursor.fetchone()
        except Exception as e:
            print(f"❌ Errore recupero utente: {e}")
            return None
    
    def generate_response(self, question, faq_results, user_info):
        """Genera risposta combinata"""
        if not faq_results:
            return {
                'answer': "Mi dispiace, non ho trovato informazioni specifiche per la tua domanda. Puoi essere più specifico?",
                'confidence': 0,
                'sources': []
            }
        
        best_match = faq_results[0]
        confidence = best_match.get('_additional', {}).get('certainty', 0)
        
        # Risposta base dalla FAQ
        answer = best_match['answer']
        
        # Arricchimento con dati utente
        if user_info:
            answer += f"\n\n👤 **Info Account:** {user_info['nome']} ({user_info['ruolo']})"
        
        # Arricchimento con dati sensori se pertinenti
        if self.is_sensor_related(question):
            sensor_data = self.get_sensor_stats()
            if sensor_data:
                answer += "\n\n📊 **Dati Sensori Attuali:**"
                for sensor in sensor_data:
                    answer += f"\n• {sensor['tipo']}: {sensor['valore']} {sensor['unita']}"
        
        return {
            'answer': answer,
            'confidence': confidence,
            'sources': [best_match.get('module', 'N/A')]
        }
    
    def is_sensor_related(self, question):
        """Controlla se la domanda è relativa ai sensori"""
        keywords = ['temperatura', 'umidità', 'sensore', 'valore', 'lettura']
        return any(keyword in question.lower() for keyword in keywords)
    
    def get_sensor_stats(self):
        """Recupera statistiche sensori"""
        # Simulazione dati sensori
        return [
            {'tipo': 'Temperatura', 'valore': 24.5, 'unita': '°C'},
            {'tipo': 'Umidità', 'valore': 65, 'unita': '%'},
            {'tipo': 'Livello Acqua', 'valore': 80, 'unita': '%'}
        ]
    
    def log_interaction(self, user_email, question, answer):
        """Registra interazione nel database"""
        try:
            with self.pg_conn.cursor() as cursor:
                cursor.execute("""
                    INSERT INTO interazioni_chatbot (utente_email, domanda, risposta, timestamp)
                    VALUES (%s, %s, %s, NOW())
                """, (user_email, question, answer))
                self.pg_conn.commit()
        except Exception as e:
            print(f"❌ Errore logging: {e}")
```

### **3. Flask Web Server**

#### **main.py:**
```python
from flask import Flask, request, jsonify, render_template
from flask_cors import CORS
from chatbot_service import SerraChatbot

app = Flask(__name__)
CORS(app)

# Inizializza chatbot
chatbot = SerraChatbot()

@app.route('/')
def index():
    """Pagina principale chatbot"""
    return render_template('chatbot.html')

@app.route('/api/chat', methods=['POST'])
def chat():
    """Endpoint per domande chatbot"""
    try:
        data = request.json
        question = data.get('question', '')
        user_email = data.get('user_email', None)
        
        if not question:
            return jsonify({'error': 'Domanda richiesta'}), 400
        
        response = chatbot.process_question(question, user_email)
        return jsonify(response)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'service': 'serra-chatbot'})

if __name__ == '__main__':
    print("🚀 Avvio Serra Chatbot Service...")
    app.run(host='0.0.0.0', port=5000, debug=True)
```

---

## 🌐 **INTEGRAZIONE CON APP ANDROID**

### **1. Modifica MainActivity.kt**

Aggiungere nel `WebAppInterface`:
```kotlin
@JavascriptInterface
fun openChatbot() {
    runOnUiThread {
        webView.loadUrl("http://10.0.2.2:5000/")
    }
}
```

### **2. Aggiunta Button nel Dashboard**

Modificare `dashboard.html`:
```html
<!-- Aggiungere nella navbar -->
<li class="nav-item">
    <a class="nav-link" href="#" onclick="Android.openChatbot()">
        <i class="fas fa-robot me-1"></i>Assistente
    </a>
</li>
```

### **3. Interfaccia Web Chatbot**

#### **templates/chatbot.html:**
```html
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assistente Serra AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .chat-container {
            height: 60vh;
            overflow-y: auto;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 15px;
            background-color: #f8f9fa;
        }
        .message {
            margin: 10px 0;
            padding: 10px;
            border-radius: 8px;
            max-width: 80%;
        }
        .user-message {
            background-color: #007bff;
            color: white;
            margin-left: auto;
        }
        .bot-message {
            background-color: #e9ecef;
            color: #333;
        }
    </style>
</head>
<body>
    <div class="container mt-4">
        <div class="row">
            <div class="col-12">
                <h3><i class="fas fa-robot"></i> Assistente Serra AI</h3>
                <div id="chatContainer" class="chat-container">
                    <div class="message bot-message">
                        🤖 Ciao! Sono l'assistente AI della serra. Posso aiutarti con domande su irrigazione, sensori, dashboard e altro. Cosa vorresti sapere?
                    </div>
                </div>
                <div class="input-group mt-3">
                    <input type="text" id="questionInput" class="form-control" placeholder="Scrivi la tua domanda...">
                    <button onclick="sendQuestion()" class="btn btn-primary">Invia</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        async function sendQuestion() {
            const input = document.getElementById('questionInput');
            const question = input.value.trim();
            
            if (!question) return;
            
            // Mostra domanda utente
            addMessage(question, 'user');
            input.value = '';
            
            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        question: question,
                        user_email: getCurrentUser()
                    })
                });
                
                const data = await response.json();
                addMessage(data.answer, 'bot');
                
            } catch (error) {
                addMessage('❌ Errore di connessione', 'bot');
            }
        }
        
        function addMessage(text, sender) {
            const container = document.getElementById('chatContainer');
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${sender}-message`;
            messageDiv.textContent = text;
            container.appendChild(messageDiv);
            container.scrollTop = container.scrollHeight;
        }
        
        function getCurrentUser() {
            // Integrazione con Android
            if (typeof Android !== 'undefined') {
                return Android.getCurrentUser();
            }
            return null;
        }
        
        // Invia con Enter
        document.getElementById('questionInput').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') sendQuestion();
        });
    </script>
</body>
</html>
```

---

## 📋 **PIANO DI IMPLEMENTAZIONE**

### **Fase 1: Setup Base (1-2 giorni)**
1. ✅ Crea cartella `serra-chatbot/`
2. ✅ Installa Python e dipendenze
3. ✅ Configura Weaviate (locale o cloud)
4. ✅ Configura PostgreSQL
5. ✅ Testa connessioni

### **Fase 2: Sviluppo Core (2-3 giorni)**
1. ✅ Implementa `chatbot_service.py`
2. ✅ Crea schema Weaviate
3. ✅ Sviluppa FAQ specifiche Serra
4. ✅ Implementa ricerca semantica
5. ✅ Testa logica chatbot

### **Fase 3: Web Interface (1-2 giorni)**
1. ✅ Sviluppa Flask server
2. ✅ Crea interfaccia HTML
3. ✅ Implementa API endpoints
4. ✅ Testa comunicazione web

### **Fase 4: Integrazione Android (1-2 giorni)**
1. ✅ Modifica MainActivity
2. ✅ Aggiorna dashboard
3. ✅ Testa integrazione WebView
4. ✅ Implementa passaggio dati utente

---

## 💡 **VANTAGGI IMPLEMENTAZIONE**

### **Per il Progetto Serra:**
- 🤖 **Supporto 24/7** per utenti
- 📚 **Knowledge Base** centralizzata
- 🔍 **Ricerca Semantica** intelligente
- 📊 **Analytics** delle domande utenti
- 🚀 **Scalabilità** per nuove FAQ

### **Per l'Esercizio Accademico:**
- ✅ **Tutti i requisiti** soddisfatti
- 🏗️ **Architettura completa** con Vector Store
- 💾 **Database integrato** con dati strutturati
- 🌐 **Interfaccia moderna** e funzionale
- 📱 **Integrazione reale** con app esistente

---

## 🚀 **PROSSIMI PASSI**

### **Cosa fare ora:**
1. **📋 Analizza** questo piano di implementazione
2. **🤔 Valuta** la fattibilità e tempi
3. **💬 Dimmi** cosa ne pensi dell'approccio
4. **🛠️ Scegliamo** da dove iniziare

### **Domande per te:**
- 🔍 Che tipo di domande dovrebbe gestire?
- 📱 Preferisci integrazione o servizio separato?
- 💾 Hai PostgreSQL già configurato?
- 🌐 Weaviate Cloud o locale?
- ⏰ Quanto tempo hai a disposizione?

**Dimmi cosa ne pensi e come vorresti procedere!** 🤖✨ 