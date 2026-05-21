package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DettaglioFeedback {

    public final int idServizio;
    public final String cliente;
    public final String tipo;
    public final String categoria;
    public final int voto;
    public final String commento;

    public DettaglioFeedback(int idServizio, String cliente,
                             String tipo, String categoria, int voto, String commento) {
        this.idServizio = idServizio;
        this.cliente = cliente;
        this.tipo = tipo;
        this.categoria = categoria;
        this.voto = voto;
        this.commento = commento;
    }

    @Override
    public String toString() {
        return String.format("#%d [%s/%s] %s - voto %d/5 - %s",
            idServizio, tipo, categoria, cliente, voto, commento == null ? "" : commento);
    }

    public static final class DAO {
        private static final String LIST_QUERY = """
                SELECT s.id_servizio,
                       CONCAT(u.nome, ' ', u.cognome) AS cliente,
                       CASE
                           WHEN d.id_delivery IS NOT NULL THEN 'DELIVERY'
                           WHEN a.id_asporto IS NOT NULL THEN 'ASPORTO'
                           WHEN pt.id_prenotazione IS NOT NULL THEN 'PRENOTAZIONE'
                           ELSE 'SERVIZIO'
                       END AS tipo,
                       COALESCE(f.categoria, 'prodotto') AS categoria,
                       f.voto, f.commento
                FROM FEEDBACK f
                JOIN SERVIZIO s ON f.id_servizio = s.id_servizio
                JOIN UTENTE u ON s.id_utente = u.id_utente
                LEFT JOIN DELIVERY d ON s.id_servizio = d.id_servizio
                LEFT JOIN ASPORTO a ON s.id_servizio = a.id_servizio
                LEFT JOIN PRENOTAZIONE_TAVOLO pt ON s.id_servizio = pt.id_servizio
                ORDER BY s.id_servizio DESC
                """;

        public static List<DettaglioFeedback> lista(Connection connection) {
            Servizio.DAO.preparaFeedbackCompatibile(connection);
            var feedback = new ArrayList<DettaglioFeedback>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY);
                var rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    feedback.add(new DettaglioFeedback(
                        rs.getInt("id_servizio"),
                        rs.getString("cliente"),
                        rs.getString("tipo"),
                        rs.getString("categoria"),
                        rs.getInt("voto"),
                        rs.getString("commento")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return feedback;
        }
    }
}
