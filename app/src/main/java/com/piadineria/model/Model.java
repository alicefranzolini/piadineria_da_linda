package com.piadineria.model;

import com.piadineria.data.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interfaccia del Model nel pattern MVC.
 *
 * Come spiega il README: usiamo un'interfaccia qui perché ci permette
 * di avere un "MockModel" per testare la GUI senza bisogno del database.
 * Tutti gli altri concetti sono classi dirette.
 *
 * Il Controller usa solo questa interfaccia: non sa se sta parlando
 * col database reale o con dati finti.
 */
public interface Model {

    // ---------- Autenticazione ----------

    /** Tenta il login. Restituisce Optional.empty() se fallisce. */
    Optional<Utente> login(String email, String password);

    /** Tenta il login di un fattorino. */
    Optional<Fattorino> loginFattorino(String email, String password);

    /** Registra un nuovo utente. Restituisce l'id creato. */
    int registraUtente(String nome, String cognome, String email, String password);

    /** Registra un nuovo fattorino. Restituisce l'id creato. */
    int registraFattorino(String nome, String cognome, String email, String password);

    /** Prepara le strutture dati dei fattorini. */
    void preparaFattorini();

    /** Lista tutti i fattorini registrati. */
    List<Fattorino> getFattorini();

    /** Elimina un fattorino. */
    void eliminaFattorino(int idFattorino);

    // ---------- Menù ----------

    /** Lista tutti i prodotti disponibili (per la schermata principale). */
    List<Prodotto> getProdottiDisponibili();

    /** Registra un nuovo prodotto nel menu. */
    int registraProdotto(String nome, String descrizione,
                         double prezzo, String categoria);

    // ---------- Ordini ----------

    /**
     * Crea un nuovo ordine.
     * @param idUtente      chi ordina
     * @param tipo          "DELIVERY" o "ASPORTO"
     * @param indirizzo     indirizzo di consegna (solo per DELIVERY, altrimenti null)
     * @param prodotti      mappa id_prodotto -> quantita
     * @return id del servizio creato
     */
    int creaOrdine(int idUtente, String tipo, String indirizzo,
                   Map<Integer, Integer> prodotti);

    /** Crea una prenotazione tavolo. */
    int prenotaTavolo(int idUtente, LocalDate giorno, LocalTime ora, int persone);

    /** Registra il feedback su un servizio. */
    void lasciaFeedback(int idServizio, int voto, String commento);

    /** Storico ordini dell'utente. */
    List<Servizio> getStoricoOrdini(int idUtente);

    /** Ordini delivery visibili al fattorino. */
    List<Servizio> getOrdiniDelivery();

    // ---------- Statistiche (amministratore) ----------

    /** Statistiche di vendita per prodotto. */
    List<StatisticaProdotto> getStatistiche();
}
