package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
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
        private static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS TESSERA_FEDELTA (
                    id_tessera INT NOT NULL AUTO_INCREMENT,
                    numero_tessera VARCHAR(32) NOT NULL,
                    ordini_effettuati INT NOT NULL DEFAULT 0,
                    data_ultimo_ordine DATE NULL,
                    id_utente INT NOT NULL,
                    CONSTRAINT tessera_fedelta_pk PRIMARY KEY (id_tessera)
                )
                """;

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
                prepara(connection);
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

        public static void prepara(Connection connection) {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            try {
                aggiungiColonnaSeManca(connection, "numero_tessera",
                    "VARCHAR(32) NULL");
                aggiungiColonnaSeManca(connection, "ordini_effettuati",
                    "INT NOT NULL DEFAULT 0");
                aggiungiColonnaSeManca(connection, "data_ultimo_ordine",
                    "DATE NULL");
                aggiungiColonnaSeManca(connection, "id_utente",
                    "INT NULL");
                rendiNullableColonneLegacy(connection);
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

        private static void aggiungiColonnaSeManca(Connection connection,
                                                  String colonna,
                                                  String definizione)
                throws SQLException {
            if (colonnaEsiste(connection, colonna)) return;
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE TESSERA_FEDELTA ADD COLUMN "
                    + colonna + " " + definizione);
            }
        }

        private static boolean colonnaEsiste(Connection connection, String colonna)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "TESSERA_FEDELTA", colonna)) {
                if (columns.next()) return true;
            }
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "tessera_fedelta", colonna)) {
                return columns.next();
            }
        }

        private static void rendiNullableColonneLegacy(Connection connection)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "TESSERA_FEDELTA", null)) {
                while (columns.next()) {
                    String colonna = columns.getString("COLUMN_NAME");
                    String nome = colonna.toLowerCase();
                    boolean usata = nome.equals("id_tessera")
                        || nome.equals("numero_tessera")
                        || nome.equals("ordini_effettuati")
                        || nome.equals("data_ultimo_ordine")
                        || nome.equals("id_utente");
                    boolean autoIncrement = "YES".equalsIgnoreCase(
                        columns.getString("IS_AUTOINCREMENT"));
                    boolean nullable = "YES".equalsIgnoreCase(
                        columns.getString("IS_NULLABLE"));
                    if (usata || autoIncrement || nullable) continue;

                    try (var stmt = connection.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE TESSERA_FEDELTA MODIFY COLUMN "
                            + colonna + " " + definizioneColonna(columns) + " NULL");
                    }
                }
            }
        }

        private static String definizioneColonna(java.sql.ResultSet columns)
                throws SQLException {
            int dataType = columns.getInt("DATA_TYPE");
            String typeName = columns.getString("TYPE_NAME");
            int size = columns.getInt("COLUMN_SIZE");
            int decimals = columns.getInt("DECIMAL_DIGITS");

            return switch (dataType) {
                case Types.CHAR, Types.VARCHAR -> typeName + "(" + size + ")";
                case Types.DECIMAL, Types.NUMERIC -> typeName + "(" + size + "," + decimals + ")";
                default -> typeName;
            };
        }
    }
}
