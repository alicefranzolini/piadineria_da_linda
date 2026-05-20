package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dettaglio operativo di un ordine delivery per il fattorino.
 */
public final class DettaglioOrdine {

    public final int idServizio;
    public final String nomeCliente;
    public final String indirizzo;
    public final List<String> prodotti;

    public DettaglioOrdine(int idServizio, String nomeCliente,
                           String indirizzo, List<String> prodotti) {
        this.idServizio = idServizio;
        this.nomeCliente = nomeCliente;
        this.indirizzo = indirizzo;
        this.prodotti = prodotti;
    }

    public String testo() {
        var testo = new StringBuilder();
        testo.append("Ordine #").append(idServizio).append("\n");
        testo.append("Nome: ").append(nomeCliente).append("\n");
        testo.append("Indirizzo: ").append(indirizzo).append("\n\n");
        testo.append("Prodotti:\n");
        for (var prodotto : prodotti) {
            testo.append("- ").append(prodotto).append("\n");
        }
        return testo.toString();
    }

    public static final class DAO {

        private static final String TESTATA_QUERY = """
                SELECT CONCAT(u.nome, ' ', u.cognome) AS cliente,
                       d.indirizzo_consegna
                FROM   SERVIZIO s
                JOIN   UTENTE u ON s.id_utente = u.id_utente
                JOIN   DELIVERY d ON s.id_servizio = d.id_servizio
                WHERE  s.id_servizio = ?
                """;

        private static final String PRODOTTI_QUERY = """
                SELECT p.nome, c.quantita
                FROM   CONTIENE c
                JOIN   PRODOTTO p ON c.id_prodotto = p.id_prodotto
                WHERE  c.id_servizio = ?
                ORDER BY p.nome
                """;

        public static DettaglioOrdine find(Connection connection, int idServizio) {
            try {
                String cliente;
                String indirizzo;
                try (
                    var stmt = DAOUtils.prepareStatement(connection, TESTATA_QUERY, idServizio);
                    var rs = stmt.executeQuery()
                ) {
                    if (!rs.next()) throw new DAOException("Ordine delivery non trovato");
                    cliente = rs.getString("cliente");
                    indirizzo = rs.getString("indirizzo_consegna");
                }

                var prodotti = new ArrayList<String>();
                try (
                    var stmt = DAOUtils.prepareStatement(connection, PRODOTTI_QUERY, idServizio);
                    var rs = stmt.executeQuery()
                ) {
                    while (rs.next()) {
                        prodotti.add(rs.getInt("quantita") + "x " + rs.getString("nome"));
                    }
                }

                return new DettaglioOrdine(idServizio, cliente, indirizzo, prodotti);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }
    }
}
