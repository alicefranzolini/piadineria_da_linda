package com.piadineria.model;

import com.piadineria.data.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Implementazione reale del Model: comunica con MySQL tramite JDBC.
 * Riceve la Connection nel costruttore e la passa a tutti i DAO.
 * La connessione è creata una sola volta in App.java e condivisa.
 */
public final class ModelImpl implements Model {

    private final Connection connection;

    public ModelImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Utente> login(String email, String password) {
        return Utente.DAO.login(connection, email, password);
    }

    @Override
    public Optional<Fattorino> loginFattorino(String email, String password) {
        return Fattorino.DAO.login(connection, email, password);
    }

    @Override
    public int registraUtente(String nome, String cognome,
                               String email, String password) {
        return Utente.DAO.registra(connection, nome, cognome, email, password);
    }

    @Override
    public int registraFattorino(String nome, String cognome,
                                 String email, String password) {
        return Fattorino.DAO.registra(connection, nome, cognome, email, password);
    }

    @Override
    public void preparaFattorini() {
        Fattorino.DAO.preparaTabella(connection);
    }

    @Override
    public List<Fattorino> getFattorini() {
        return Fattorino.DAO.lista(connection);
    }

    @Override
    public void eliminaFattorino(int idFattorino) {
        Fattorino.DAO.elimina(connection, idFattorino);
    }

    @Override
    public List<Prodotto> getProdottiDisponibili() {
        return Prodotto.DAO.listDisponibili(connection);
    }

    @Override
    public int registraProdotto(String nome, String descrizione,
                                double prezzo, String categoria) {
        return Prodotto.DAO.registra(connection, nome, descrizione, prezzo, categoria);
    }

    @Override
    public int creaOrdine(int idUtente, String tipo,
                          String indirizzo, Map<Integer, Integer> prodotti) {
        return Servizio.DAO.creaOrdine(connection, idUtente, tipo, indirizzo, prodotti);
    }

    @Override
    public int prenotaTavolo(int idUtente, LocalDate giorno,
                             LocalTime ora, int persone) {
        return Servizio.DAO.prenotaTavolo(connection, idUtente, giorno, ora, persone);
    }

    @Override
    public void lasciaFeedback(int idServizio, int voto, String commento) {
        Servizio.DAO.lasciaFeedback(connection, idServizio, voto, commento);
    }

    @Override
    public List<Servizio> getStoricoOrdini(int idUtente) {
        return Servizio.DAO.storicoUtente(connection, idUtente);
    }

    @Override
    public List<Servizio> getOrdiniDelivery() {
        return Servizio.DAO.delivery(connection);
    }

    @Override
    public void aggiornaStatoOrdine(int idServizio, String nomeStato) {
        Servizio.DAO.aggiornaStato(connection, idServizio, nomeStato);
    }

    @Override
    public DettaglioOrdine getDettaglioOrdine(int idServizio) {
        return DettaglioOrdine.DAO.find(connection, idServizio);
    }

    @Override
    public List<Servizio> getPrenotazioniTavoli() {
        return Servizio.DAO.prenotazioni(connection);
    }

    @Override
    public void aggiornaStatoPrenotazione(int idServizio, String nomeStato) {
        Servizio.DAO.aggiornaStato(connection, idServizio, nomeStato);
    }

    @Override
    public List<StatisticaProdotto> getStatistiche() {
        return StatisticaProdotto.DAO.lista(connection);
    }
}
