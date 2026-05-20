package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public final class TesseraFedelta {

    public final String numero;
    public final int ordiniEffettuati;
    public final LocalDate dataUltimoOrdine;

    public TesseraFedelta(String numero, int ordiniEffettuati,
                          LocalDate dataUltimoOrdine) {
        this.numero = numero;
        this.ordiniEffettuati = ordiniEffettuati;
        this.dataUltimoOrdine = dataUltimoOrdine;
    }

    public int mancantiSconto() {
        int resto = ordiniEffettuati % 5;
        return resto == 0 ? 5 : 5 - resto;
    }

    public boolean prossimoOrdineScontato() {
        return ordiniEffettuati > 0 && ordiniEffettuati % 5 == 0;
    }

    public String testo() {
        var testo = new StringBuilder();
        testo.append("Tessera fedelta: ").append(numero).append("\n");
        testo.append("Ordini effettuati: ").append(ordiniEffettuati).append("\n");
        if (dataUltimoOrdine != null) {
            testo.append("Ultimo ordine: ").append(dataUltimoOrdine).append("\n");
        }
        testo.append("\nRegola sconto: ogni 5 ordini, il prossimo ordine ha il 20% di sconto.\n");
        if (prossimoOrdineScontato()) {
            testo.append("Hai diritto al 20% di sconto sul prossimo ordine.");
        } else {
            testo.append("Ordini mancanti al prossimo sconto: ").append(mancantiSconto()).append(".");
        }
        return testo.toString();
    }

    public static final class DAO {
        private static final String FIND_QUERY = """
                SELECT numero_tessera, ordini_effettuati, data_ultimo_ordine
                FROM TESSERA_FEDELTA
                WHERE id_utente = ?
                """;

        private static final String INSERT_QUERY = """
                INSERT INTO TESSERA_FEDELTA
                    (numero_tessera, ordini_effettuati, id_utente)
                VALUES (CONCAT('TF', LPAD(?, 6, '0')), 0, ?)
                """;

        public static TesseraFedelta trovaOCrea(Connection connection, int idUtente) {
            try {
                var tessera = trova(connection, idUtente);
                if (tessera != null) return tessera;

                try (var stmt = DAOUtils.prepareStatement(
                        connection, INSERT_QUERY, idUtente, idUtente)) {
                    stmt.executeUpdate();
                }
                return trova(connection, idUtente);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static TesseraFedelta trova(Connection connection, int idUtente)
                throws SQLException {
            try (
                var stmt = DAOUtils.prepareStatement(connection, FIND_QUERY, idUtente);
                var rs = stmt.executeQuery()
            ) {
                if (!rs.next()) return null;
                var data = rs.getDate("data_ultimo_ordine");
                return new TesseraFedelta(
                    rs.getString("numero_tessera"),
                    rs.getInt("ordini_effettuati"),
                    data == null ? null : data.toLocalDate()
                );
            }
        }
    }
}
