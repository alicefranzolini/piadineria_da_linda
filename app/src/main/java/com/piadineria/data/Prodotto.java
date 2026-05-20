package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta un prodotto del menù della piadineria.
 * Come spiega il README: l'oggetto Java non deve corrispondere
 * esattamente alla tabella del DB. Qui aggreghiamo anche il nome
 * della categoria per comodità di visualizzazione.
 */
public final class Prodotto {

    public final int id;
    public final String nome;
    public final String descrizione;
    public final double prezzo;
    public final String nomeCategoria;
    public final boolean disponibile;

    public Prodotto(int id, String nome, String descrizione,
                    double prezzo, String nomeCategoria, boolean disponibile) {
        this.id            = id;
        this.nome          = nome;
        this.descrizione   = descrizione;
        this.prezzo        = prezzo;
        this.nomeCategoria = nomeCategoria;
        this.disponibile   = disponibile;
    }

    // toString usato dalla JList di Swing per mostrare il nome nella lista
    @Override
    public String toString() {
        return String.format("%s - %.2f€  [%s]", nome, prezzo, nomeCategoria);
    }

    // -------------------------------------------------------------------------
    // DAO interno: unico punto di accesso alle query su PRODOTTO
    // -------------------------------------------------------------------------
    public static final class DAO {

        // Query che lista tutti i prodotti disponibili con il nome della categoria
        private static final String LIST_QUERY = """
                SELECT p.id_prodotto, p.nome, p.descrizione,
                       p.costo_prodotto, c.nome_categoria, p.disponibilita
                FROM   PRODOTTO p
                JOIN   CATEGORIA_PRODOTTO c ON p.id_categoria = c.id_categoria
                WHERE  p.disponibilita = TRUE
                ORDER BY c.nome_categoria, p.nome
                """;

        /**
         * Restituisce la lista di tutti i prodotti disponibili.
         * Usato nella schermata principale per mostrare il menù.
         */
        public static List<Prodotto> listDisponibili(Connection connection) {
            var prodotti = new ArrayList<Prodotto>();

            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY);
                var rs   = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    prodotti.add(new Prodotto(
                        rs.getInt("id_prodotto"),
                        rs.getString("nome"),
                        rs.getString("descrizione"),
                        rs.getDouble("costo_prodotto"),
                        rs.getString("nome_categoria"),
                        rs.getBoolean("disponibilita")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }

            return prodotti;
        }
    }
}
