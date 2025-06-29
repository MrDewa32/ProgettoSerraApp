# Diagramma di Sequenza - Frontend Dashboard Serra

## Descrizione del Diagramma

Il diagramma di sequenza UML illustra le principali interazioni tra l'utente e i vari componenti del sistema frontend della Dashboard Serra. Il diagramma è suddiviso in quattro sezioni principali che rappresentano i flussi chiave dell'applicazione.

### Componenti Rappresentati

1. **User**: L'utente finale che interagisce con l'interfaccia
2. **Login Page**: La pagina di autenticazione
3. **Dashboard Page**: La dashboard principale dell'applicazione
4. **Grafana Panels**: I pannelli per la visualizzazione dei dati
5. **WebSocket Server**: Il server per la comunicazione real-time

### Flussi Principali

#### 1. Processo di Login
- L'utente inserisce le credenziali nella pagina di login
- La pagina valida localmente gli input
- In caso di successo, reindirizza alla dashboard

#### 2. Monitoraggio Dati
- L'utente accede alla dashboard
- La dashboard richiede i pannelli Grafana
- Grafana restituisce i grafici per temperatura, umidità e livello acqua
- I dati vengono visualizzati in tempo reale

#### 3. Controllo Pompa (Attivazione)
- L'utente clicca il pulsante "Avvia Pompa"
- La dashboard invia il comando "attiva" via WebSocket
- Il server risponde con lo stato "Pompa In Attivazione"
- L'UI aggiorna l'indicatore in giallo (stato transitorio)
- Il server conferma lo stato "Pompa Attiva"
- L'UI aggiorna l'indicatore in verde

#### 4. Controllo Pompa (Spegnimento)
- L'utente clicca il pulsante "Ferma Pompa"
- La dashboard invia il comando "stop" via WebSocket
- Il server risponde con lo stato "Pompa In Spegnimento"
- L'UI aggiorna l'indicatore in giallo (stato transitorio)
- Il server conferma lo stato "Pompa Inattiva"
- L'UI aggiorna l'indicatore in rosso

### Caratteristiche Chiave

1. **Feedback Visivo**
   - Ogni azione dell'utente genera un feedback immediato nell'interfaccia
   - Gli stati transitori sono chiaramente indicati
   - I cambiamenti di stato sono visualizzati attraverso indicatori colorati

2. **Comunicazione Real-time**
   - WebSocket mantiene una connessione persistente
   - Gli aggiornamenti di stato sono immediati
   - La comunicazione è bidirezionale

3. **Gestione Stati**
   - Gli stati della pompa sono gestiti in modo granulare
   - Ogni transizione di stato è tracciata
   - L'interfaccia riflette sempre lo stato corrente del sistema

4. **Integrazione Grafana**
   - I pannelli Grafana sono integrati nella dashboard
   - La visualizzazione dei dati è in tempo reale
   - L'integrazione è trasparente per l'utente

### Considerazioni Tecniche

- Il diagramma mostra come l'architettura frontend gestisce efficacemente le interazioni utente
- La separazione dei componenti permette una chiara distribuzione delle responsabilità
- L'uso di WebSocket garantisce aggiornamenti in tempo reale senza polling
- L'integrazione con Grafana fornisce visualizzazioni dati professionali
- Il sistema di feedback visivo migliora l'esperienza utente

### Vantaggi dell'Architettura

1. **Responsività**
   - Feedback immediato per le azioni utente
   - Aggiornamenti in tempo reale dei dati
   - Transizioni di stato fluide

2. **Manutenibilità**
   - Componenti chiaramente separati
   - Flussi di interazione ben definiti
   - Facile da debuggare e testare

3. **Scalabilità**
   - Facile aggiungere nuovi stati o azioni
   - Possibilità di estendere le funzionalità
   - Architettura modulare 

## Interazioni tra Componenti

### Login Page ↔️ Dashboard
- **Tipo di Interazione**: Reindirizzamento
- **Meccanismo**: 
  ```javascript
  // Dopo validazione credenziali
  window.location.href = 'index.html';
  ```
- **Dati Scambiati**: Nessuno (autenticazione gestita lato client in questa versione)

### Dashboard ↔️ Grafana
- **Tipo di Interazione**: Embedding via iframe
- **Meccanismo**:
  ```html
  <iframe src="http://localhost:3000/d-solo/..." class="grafana-panel"></iframe>
  ```
- **Dati Visualizzati**:
  - Temperatura ambiente
  - Umidità del terreno
  - Livello acqua serbatoio
- **Aggiornamento**: Automatico gestito da Grafana (ogni 10 secondi)

### Dashboard ↔️ WebSocket
- **Tipo di Interazione**: Comunicazione bidirezionale real-time
- **Connessione**:
  ```javascript
  const socket = new WebSocket("ws://localhost:1880/ws/pulsante");
  ```
- **Messaggi Inviati**:
  - `"attiva"`: Richiesta attivazione pompa
  - `"stop"`: Richiesta spegnimento pompa
- **Messaggi Ricevuti**:
  - `"Pompa In Attivazione"`: Stato transitorio
  - `"Pompa Attiva"`: Conferma attivazione
  - `"Pompa In Spegnimento"`: Stato transitorio
  - `"Pompa Inattiva"`: Conferma spegnimento

### Dashboard ↔️ UI
- **Tipo di Interazione**: Manipolazione DOM e gestione eventi
- **Eventi Gestiti**:
  - Click sui pulsanti
  - Aggiornamenti stato pompa
  - Navigazione sezioni
- **Feedback Visivi**:
  - Indicatore stato: rosso (inattivo), giallo (transizione), verde (attivo)
  - Messaggi di stato testuali
  - Disabilitazione pulsanti durante le transizioni

### Flusso Dati Complessivo
1. **Monitoraggio Continuo**:
   ```
   Sensori → Grafana → Dashboard (iframe) → Utente
   ```

2. **Controllo Pompa**:
   ```
   Utente → Dashboard (click) → WebSocket → Server
                ↓
   Utente ← Dashboard (UI update) ← WebSocket ← Server
   ```

3. **Navigazione**:
   ```
   Utente → Dashboard (click) → Sezione Specifica
                                    ↓
                               Aggiornamento UI
   ```

Questa struttura di interazioni permette:
- Aggiornamenti in tempo reale dei dati
- Feedback immediato delle azioni utente
- Gestione efficiente degli stati del sistema
- Separazione chiara delle responsabilità

La comunicazione tra i componenti è stata progettata per essere:
- Affidabile: gestione degli errori e stati inconsistenti
- Efficiente: utilizzo di WebSocket per comunicazione real-time
- Scalabile: facile aggiunta di nuovi componenti o funzionalità
- Manutenibile: chiara separazione delle responsabilità 