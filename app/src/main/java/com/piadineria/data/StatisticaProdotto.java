package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta una riga del report statistiche: prodotto più venduto.
 * Non corrisponde a nessuna tabella del DB — è un oggetto creato
 * appositamente per la funzionalità "Analisi statistiche" (OP A6).
 */
public final class StatisticaProdotto {

    public final String nome;
    public final int    totaleOrdinato;
    public final double votoMedio;

    public StatisticaProdotto(String nome, int totaleOrdinato, double votoMedio) {
        this.nome           = nome;
        this.totaleOrdinato = totaleOrdinato;
        this.votoMedio      = votoMedio;
    }

    @Override
    public String toString() {
        return String.format("%-25s  vendite: %3d   voto medio: %.1f",
                nome, totaleOrdinato, votoMedio);
    }

    public static final class DAO {

        private static final String STATS_QUERY = """
                SELECT p.nome,
                       COALESCE(SUM(c.quantita), 0) AS totale_ordinato,
                       COALESCE(AVG(f.voto), 0)     AS voto_medio
                FROM   PRODOTTO p
                LEFT JOIN CONTIENE c ON p.id_prodotto = c.id_prodotto
                LEFT JOIN SERVIZIO s ON c.id_servizio = s.id_servizio
                                     AND s.id_stato   = 4
                LEFT JOIN FEEDBACK f ON s.id_servizio = f.id_servizio
                GROUP BY p.id_prodotto, p.nome
                ORDER BY totale_ordinato DESC
                """;

        /**
         * Restituisce le statistiche di vendita per ogni prodotto.
         * Usato nella dashboard amministratore.
         */
        public static List<StatisticaProdotto> lista(Connection connection) {
            var risultati = new ArrayList<StatisticaProdotto>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, STATS_QUERY);
                var rs   = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    risultati.add(new StatisticaProdotto(
                        rs.getString("nome"),
                        rs.getInt("totale_ordinato"),
                        rs.getDouble("voto_medio")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return risultati;
        }
    }
}
