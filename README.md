WoT Counter App

Un'app android che agisce sia da server WoT sia da client, per esporre e consumare un contatore modificabile tramite l'app.

Il progetto è diviso in:
- MainActivity.kt: gestisce UI
- CounterThing.kt: definizione del Thing counter con proprietà e azioni
- CounterServer.kt: espone il Thing tramite WoT

Come funziona?

All'avvio MainActivity:
- crea un servient WoT con supporto HTTP
- istanzia CounterThing
- espone il Thing via CounterServer su localhost:8080
- consuma il Thing via CounterClient
  
L'interfaccia mostra il valore del contatore e ha bottoni per:
- incrementare
- decrementare
- resettare
- aggiornare il valore attuale

Requisiti:
- Android API 26+
- Permessi di rete attivi
- Libreria kotlin-wot
