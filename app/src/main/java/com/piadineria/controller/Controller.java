package com.piadineria.controller;

import com.piadineria.data.*;
import com.piadineria.model.Model;
import com.piadineria.view.View;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller nel pattern MVC.
 *
 * Come spiega il README: "leggendo i suoi metodi pubblici si ottiene
 * una visione completa di tutte le interazioni possibili".
 *
 * Il Controller:
 * - riceve le azioni dalla View
 * - le traduce in chiamate al Model
 * - dice alla View cosa mostrare
 * Non contiene logica SQL né logica di disegno grafico.
 */
public final class Controller {

    private final Model model;
    private final View  view;

    private static final String ADMIN_EMAIL = "admin@linda.it";
    private static final String ADMIN_PASSWORD = "admin";

    // Utente attualmente loggato (null se nessuno)
    private Utente utenteCorrente;
    private Fattorino fattorinoCorrente;

    // Carrello corrente: id_prodotto -> quantita
    private final Map<Integer, Integer> carrello = new LinkedHashMap<>();
    private final Map<Integer, Prodotto> prodottiCarrello = new LinkedHashMap<>();

    public Controller(Model model, View view) {
        this.model = model;
        this.view  = view;
    }

    /** Avvia l'applicazione: mostra la schermata di login. */
    public void start() {
        view.mostraLogin();
    }

    // -------------------------------------------------------------------------
    // Autenticazione
    // -------------------------------------------------------------------------

    /** Chiamato dalla View quando l'utente preme "Login". */
    public void login(String email, String password, String ruolo) {
        if ("ADMIN".equals(ruolo)) {
            loginAdmin(email, password);
            return;
        }
        if ("FATTORINO".equals(ruolo)) {
            loginFattorino(email, password);
            return;
        }
        loginUtente(email, password);
    }

    private void loginUtente(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            view.mostraErrore("Inserisci email e password.");
            return;
        }
        var risultato = model.login(email, password);
        if (risultato.isEmpty()) {
            view.mostraErrore("Email o password errati.");
            return;
        }
        utenteCorrente = risultato.get();
        view.mostraHome(utenteCorrente.nome + " " + utenteCorrente.cognome);
        caricaProdotti();
        caricaStorico();
    }

    private void loginAdmin(String email, String password) {
        if (!ADMIN_EMAIL.equalsIgnoreCase(email.trim()) || !ADMIN_PASSWORD.equals(password)) {
            view.mostraErrore("Credenziali admin errate.");
            return;
        }
        utenteCorrente = null;
        fattorinoCorrente = null;
        carrello.clear();
        prodottiCarrello.clear();
        caricaAdmin();
    }

    private void loginFattorino(String email, String password) {
        email = email.trim().toLowerCase();
        if (email.isBlank() || password.isBlank()) {
            view.mostraErrore("Inserisci email e password del fattorino.");
            return;
        }
        if (!emailValida(email)) {
            view.mostraErrore("Inserisci un'email fattorino valida.");
            return;
        }
        var risultato = model.loginFattorino(email, password);
        if (risultato.isEmpty()) {
            view.mostraErrore("Credenziali fattorino errate.");
            return;
        }
        utenteCorrente = null;
        fattorinoCorrente = risultato.get();
        carrello.clear();
        prodottiCarrello.clear();
        view.mostraFattorino(fattorinoCorrente.nomeCompleto());
        caricaOrdiniDelivery();
    }

    /** Chiamato dalla View quando l'utente preme "Registrati". */
    public void registra(String nome, String cognome, String email, String password) {
        if (nome.isBlank() || cognome.isBlank() || email.isBlank() || password.isBlank()) {
            view.mostraErrore("Compila tutti i campi.");
            return;
        }
        try {
            model.registraUtente(nome, cognome, email, password);
            view.mostraMessaggio("Registrazione completata! Ora puoi accedere.");
            view.mostraLogin();
        } catch (DAOException e) {
            view.mostraErrore("Email già registrata o errore nel database.");
        }
    }

    /** Registra un fattorino. Questa azione viene esposta solo nell'area admin. */
    public void registraFattorino(String nome, String cognome, String email, String password) {
        nome = nome.trim();
        cognome = cognome.trim();
        email = email.trim().toLowerCase();

        if (nome.isBlank() || cognome.isBlank() || email.isBlank() || password.isBlank()) {
            view.mostraErrore("Compila tutti i campi del fattorino.");
            return;
        }
        if (!emailValida(email)) {
            view.mostraErrore("Inserisci un'email valida, per esempio fattorino@linda.it.");
            return;
        }
        try {
            model.registraFattorino(nome, cognome, email, password);
            view.mostraMessaggio("Fattorino registrato. Ora puo accedere con email e password.");
            view.pulisciFormFattorino();
            caricaFattorini();
        } catch (DAOException e) {
            view.mostraErrore("Impossibile registrare il fattorino: " + dettaglioErrore(e));
        }
    }

    /** Registra un nuovo prodotto. Questa azione viene esposta solo nell'area admin. */
    public void registraProdotto(String nome, String descrizione,
                                 String prezzoTesto, String categoria) {
        if (nome.isBlank() || descrizione.isBlank() || prezzoTesto.isBlank()) {
            view.mostraErrore("Compila tutti i campi del prodotto.");
            return;
        }

        try {
            double prezzo = Double.parseDouble(prezzoTesto.replace(",", "."));
            if (prezzo <= 0) {
                view.mostraErrore("Il prezzo deve essere maggiore di zero.");
                return;
            }
            model.registraProdotto(nome, descrizione, prezzo, categoria);
            view.mostraMessaggio("Prodotto aggiunto al menu.");
            view.pulisciFormProdotto();
        } catch (NumberFormatException e) {
            view.mostraErrore("Inserisci un prezzo valido, per esempio 6.50.");
        } catch (DAOException e) {
            view.mostraErrore("Prodotto gia presente o errore nel database.");
        }
    }

    /** Logout: torna alla schermata di login. */
    public void logout() {
        utenteCorrente = null;
        fattorinoCorrente = null;
        carrello.clear();
        prodottiCarrello.clear();
        view.mostraLogin();
    }

    // -------------------------------------------------------------------------
    // Menù e Carrello
    // -------------------------------------------------------------------------

    /** Carica i prodotti disponibili e li mostra nella View. */
    public void caricaProdotti() {
        var prodotti = model.getProdottiDisponibili();
        view.mostraProdotti(prodotti);
    }

    /**
     * Aggiunge un prodotto al carrello.
     * Chiamato dalla View quando l'utente clicca "Aggiungi al carrello".
     */
    public void aggiungiAlCarrello(Prodotto prodotto) {
        carrello.merge(prodotto.id, 1, Integer::sum);
        prodottiCarrello.put(prodotto.id, prodotto);
        view.aggiornaCarrello(carrello.size());
        view.mostraCarrello(carrello, prodottiCarrello);
        view.mostraMessaggio("\"" + prodotto.nome + "\" aggiunto al carrello.");
    }

    /** Svuota il carrello. */
    public void svuotaCarrello() {
        carrello.clear();
        prodottiCarrello.clear();
        view.aggiornaCarrello(0);
        view.mostraCarrello(carrello, prodottiCarrello);
    }

    /** Rimuove una singola unita del prodotto selezionato dal carrello. */
    public void rimuoviDalCarrello(int idProdotto) {
        if (!carrello.containsKey(idProdotto)) {
            view.mostraErrore("Prodotto non presente nel carrello.");
            return;
        }

        int nuovaQuantita = carrello.get(idProdotto) - 1;
        if (nuovaQuantita <= 0) {
            carrello.remove(idProdotto);
            prodottiCarrello.remove(idProdotto);
        } else {
            carrello.put(idProdotto, nuovaQuantita);
        }
        view.aggiornaCarrello(carrello.size());
        view.mostraCarrello(carrello, prodottiCarrello);
    }

    // -------------------------------------------------------------------------
    // Ordini
    // -------------------------------------------------------------------------

    /**
     * Conferma l'ordine come DELIVERY.
     * Chiamato dalla View quando l'utente preme "Ordina a domicilio".
     */
    public void confermaOrdineDelivery(String indirizzo) {
        if (utenteCorrente == null) { view.mostraErrore("Non sei loggato."); return; }
        if (carrello.isEmpty())     { view.mostraErrore("Il carrello è vuoto."); return; }
        if (indirizzo.isBlank())    { view.mostraErrore("Inserisci l'indirizzo di consegna."); return; }

        try {
            int id = model.creaOrdine(utenteCorrente.id, "DELIVERY", indirizzo, carrello);
            carrello.clear();
            prodottiCarrello.clear();
            view.aggiornaCarrello(0);
            view.mostraCarrello(carrello, prodottiCarrello);
            view.mostraMessaggio("Ordine #" + id + " confermato! La piadina è in preparazione.");
            caricaStorico();
        } catch (DAOException e) {
            view.mostraErrore("Errore durante la creazione dell'ordine.");
        }
    }

    /**
     * Conferma l'ordine come ASPORTO.
     * Chiamato dalla View quando l'utente preme "Ordina da asporto".
     */
    public void confermaOrdineAsporto() {
        if (utenteCorrente == null) { view.mostraErrore("Non sei loggato."); return; }
        if (carrello.isEmpty())     { view.mostraErrore("Il carrello è vuoto."); return; }

        try {
            int id = model.creaOrdine(utenteCorrente.id, "ASPORTO", null, carrello);
            carrello.clear();
            prodottiCarrello.clear();
            view.aggiornaCarrello(0);
            view.mostraCarrello(carrello, prodottiCarrello);
            view.mostraMessaggio("Ordine #" + id + " confermato! Ritira tra 20 minuti.");
            caricaStorico();
        } catch (DAOException e) {
            view.mostraErrore("Errore durante la creazione dell'ordine.");
        }
    }

    /** Prenota un tavolo per l'utente corrente. */
    public void prenotaTavolo(String giornoTesto, String oraTesto, String personeTesto) {
        if (utenteCorrente == null) {
            view.mostraErrore("Non sei loggato.");
            return;
        }

        try {
            var giorno = LocalDate.parse(giornoTesto.trim());
            var ora = LocalTime.parse(oraTesto.trim());
            int persone = Integer.parseInt(personeTesto.trim());
            if (persone <= 0) {
                view.mostraErrore("Il numero di persone deve essere maggiore di zero.");
                return;
            }

            int id = model.prenotaTavolo(utenteCorrente.id, giorno, ora, persone);
            view.mostraMessaggio("Prenotazione tavolo #" + id + " confermata.");
            caricaStorico();
        } catch (DateTimeParseException e) {
            view.mostraErrore("Usa data AAAA-MM-GG e ora HH:MM, per esempio 2026-05-20 e 20:30.");
        } catch (NumberFormatException e) {
            view.mostraErrore("Inserisci un numero valido di persone.");
        } catch (DAOException e) {
            view.mostraErrore("Errore durante la prenotazione del tavolo: " + dettaglioErrore(e));
        }
    }

    /** Lascia un feedback sull'ordine selezionato nello storico. */
    public void lasciaFeedback(Servizio servizio, int voto, String commento) {
        if (utenteCorrente == null) {
            view.mostraErrore("Non sei loggato.");
            return;
        }
        if (servizio == null) {
            view.mostraErrore("Seleziona un ordine dallo storico.");
            return;
        }
        if (voto < 1 || voto > 5) {
            view.mostraErrore("Il voto deve essere tra 1 e 5.");
            return;
        }

        try {
            model.lasciaFeedback(servizio.id, voto, commento);
            view.mostraMessaggio("Feedback salvato. Grazie!");
        } catch (DAOException e) {
            view.mostraErrore("Errore durante il salvataggio del feedback: " + dettaglioErrore(e));
        }
    }

    // -------------------------------------------------------------------------
    // Storico
    // -------------------------------------------------------------------------

    /** Carica e mostra lo storico ordini dell'utente corrente. */
    public void caricaStorico() {
        if (utenteCorrente == null) return;
        var storico = model.getStoricoOrdini(utenteCorrente.id);
        view.mostraStorico(storico);
    }

    /** Carica gli ordini delivery per il fattorino. */
    public void caricaOrdiniDelivery() {
        var ordini = model.getOrdiniDelivery();
        view.mostraOrdiniDelivery(ordini);
    }

    // -------------------------------------------------------------------------
    // Statistiche (amministratore)
    // -------------------------------------------------------------------------

    /** Carica e mostra le statistiche di vendita. */
    public void caricaStatistiche() {
        var stats = model.getStatistiche();
        view.mostraStatistiche(stats);
    }

    /** Carica la dashboard amministratore. */
    public void caricaAdmin() {
        try {
            model.preparaFattorini();
            var stats = model.getStatistiche();
            view.mostraAdmin(stats);
            caricaFattorini();
        } catch (DAOException e) {
            view.mostraErrore("Errore durante l'apertura dell'area admin: " + dettaglioErrore(e));
        }
    }

    /** Mostra la lista dei fattorini registrati. */
    public void caricaFattorini() {
        try {
            view.mostraFattorini(model.getFattorini());
        } catch (DAOException e) {
            view.mostraErrore("Errore durante il caricamento dei fattorini: " + dettaglioErrore(e));
        }
    }

    /** Elimina il fattorino selezionato dall'admin. */
    public void eliminaFattorino(Fattorino fattorino) {
        if (fattorino == null) {
            view.mostraErrore("Seleziona un fattorino dalla lista.");
            return;
        }
        try {
            model.eliminaFattorino(fattorino.id);
            view.mostraMessaggio("Fattorino eliminato.");
            caricaFattorini();
        } catch (DAOException e) {
            view.mostraErrore("Errore durante l'eliminazione del fattorino: " + dettaglioErrore(e));
        }
    }

    private boolean emailValida(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private String dettaglioErrore(DAOException e) {
        var cause = e.getCause();
        if (cause != null && cause.getMessage() != null) return cause.getMessage();
        if (e.getMessage() != null) return e.getMessage();
        return "errore sconosciuto";
    }

    /** Torna indietro dalla schermata statistiche in base al ruolo corrente. */
    public void tornaDaStatistiche() {
        if (utenteCorrente == null) {
            logout();
            return;
        }
        view.mostraHome(utenteCorrente.nome + " " + utenteCorrente.cognome);
    }
}
