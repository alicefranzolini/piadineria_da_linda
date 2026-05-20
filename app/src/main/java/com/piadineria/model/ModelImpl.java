package com.piadineria.model;

import com.piadineria.data.*;
import java.sql.Connection;
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
    public int registraUtente(String nome, String cognome,
                               String email, String password) {
        return Utente.DAO.registra(connection, nome, cognome, email, password);
    }

    @Override
    public List<Prodotto> getProdottiDisponibili() {
        return Prodotto.DAO.listDisponibili(connection);
    }

    @Override
    public int creaOrdine(int idUtente, String tipo,
                          String indirizzo, Map<Integer, Integer> prodotti) {
        return Servizio.DAO.creaOrdine(connection, idUtente, tipo, indirizzo, prodotti);
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
    public List<StatisticaProdotto> getStatistiche() {
        return StatisticaProdotto.DAO.lista(connection);
    }
}
