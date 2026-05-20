package com.piadineria.controller;

import com.piadineria.data.*;
import com.piadineria.model.Model;
import com.piadineria.view.View;

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

    // Utente attualmente loggato (null se nessuno)
    private Utente utenteCorrente;

    // Carrello corrente: id_prodotto -> quantita
    private final Map<Integer, Integer> carrello = new LinkedHashMap<>();

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
    public void login(String email, String password) {
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

    /** Logout: torna alla schermata di login. */
    public void logout() {
        utenteCorrente = null;
        carrello.clear();
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
        view.aggiornaCarrello(carrello.size());
        view.mostraMessaggio("\"" + prodotto.nome + "\" aggiunto al carrello.");
    }

    /** Svuota il carrello. */
    public void svuotaCarrello() {
        carrello.clear();
        view.aggiornaCarrello(0);
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
            view.aggiornaCarrello(0);
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
            view.aggiornaCarrello(0);
            view.mostraMessaggio("Ordine #" + id + " confermato! Ritira tra 20 minuti.");
            caricaStorico();
        } catch (DAOException e) {
            view.mostraErrore("Errore durante la creazione dell'ordine.");
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

    // -------------------------------------------------------------------------
    // Statistiche (amministratore)
    // -------------------------------------------------------------------------

    /** Carica e mostra le statistiche di vendita. */
    public void caricaStatistiche() {
        var stats = model.getStatistiche();
        view.mostraStatistiche(stats);
    }
}
