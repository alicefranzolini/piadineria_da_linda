package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta un servizio (ordine delivery, asporto o prenotazione tavolo).
 * È l'entità centrale del sistema.
 */
public final class Servizio {

    public final int       id;
    public final String    tipoServizio;   // "DELIVERY", "ASPORTO", "PRENOTAZIONE"
    public final double    totaleCosto;
    public final double    scontoApplicato;
    public final String    statoNome;
    public final LocalDate giorno;
    public final LocalTime ora;

    public Servizio(int id, String tipoServizio, double totaleCosto,
                    double scontoApplicato, String statoNome,
                    LocalDate giorno, LocalTime ora) {
        this.id              = id;
        this.tipoServizio    = tipoServizio;
        this.totaleCosto     = totaleCosto;
        this.scontoApplicato = scontoApplicato;
        this.statoNome       = statoNome;
        this.giorno          = giorno;
        this.ora             = ora;
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %.2f€ - %s (%s)",
                id, tipoServizio, totaleCosto, statoNome, giorno);
    }

    // -------------------------------------------------------------------------
    // DAO
    // -------------------------------------------------------------------------
    public static final class DAO {

        // Storico ordini dell'utente con join su STATO_SERVIZIO
        private static final String STORICO_QUERY = """
                SELECT s.id_servizio,
                       CASE
                           WHEN d.id_delivery       IS NOT NULL THEN 'DELIVERY'
                           WHEN a.id_asporto        IS NOT NULL THEN 'ASPORTO'
                           WHEN pt.id_prenotazione  IS NOT NULL THEN 'PRENOTAZIONE'
                           ELSE 'SCONOSCIUTO'
                       END AS tipo,
                       s.totale_costo, s.sconto_applicato,
                       ss.nome_stato, s.giorno_creazione, s.ora_creazione
                FROM   SERVIZIO s
                JOIN   STATO_SERVIZIO ss ON s.id_stato = ss.id_stato
                LEFT JOIN DELIVERY          d  ON s.id_servizio = d.id_servizio
                LEFT JOIN ASPORTO           a  ON s.id_servizio = a.id_servizio
                LEFT JOIN PRENOTAZIONE_TAVOLO pt ON s.id_servizio = pt.id_servizio
                WHERE  s.id_utente = ?
                ORDER BY s.giorno_creazione DESC, s.ora_creazione DESC
                """;

        // Crea il record base in SERVIZIO (stato = 1 = 'preso in carico')
        private static final String INSERT_SERVIZIO = """
                INSERT INTO SERVIZIO
                    (sconto_applicato, totale_costo, ora_creazione,
                     giorno_creazione, id_utente, id_stato)
                VALUES (?, ?, TIME(NOW()), DATE(NOW()), ?, 1)
                """;

        // Aggiunge la riga in DELIVERY
        private static final String INSERT_DELIVERY = """
                INSERT INTO DELIVERY
                    (indirizzo_consegna, costo_consegna, id_servizio)
                VALUES (?, 2.50, ?)
                """;

        // Aggiunge la riga in ASPORTO
        private static final String INSERT_ASPORTO = """
                INSERT INTO ASPORTO (orario_ritiro, id_servizio)
                VALUES (ADDTIME(TIME(NOW()), '00:20:00'), ?)
                """;

        private static final String CREATE_PRENOTAZIONE = """
                CREATE TABLE IF NOT EXISTS PRENOTAZIONE_TAVOLO (
                    id_prenotazione INT NOT NULL AUTO_INCREMENT,
                    numero_persone INT NOT NULL,
                    data_prenotazione DATE NOT NULL,
                    ora_prenotazione TIME NOT NULL,
                    id_servizio INT NOT NULL,
                    CONSTRAINT prenotazione_tavolo_pk PRIMARY KEY (id_prenotazione)
                )
                """;

        private static final String INSERT_PRENOTAZIONE = """
                INSERT INTO PRENOTAZIONE_TAVOLO
                    (numero_persone, data_prenotazione, ora_prenotazione, id_servizio)
                VALUES (?, ?, ?, ?)
                """;

        private static final String CREATE_FEEDBACK = """
                CREATE TABLE IF NOT EXISTS FEEDBACK (
                    id_feedback INT NOT NULL AUTO_INCREMENT,
                    voto INT NOT NULL,
                    commento VARCHAR(500),
                    id_servizio INT NOT NULL,
                    CONSTRAINT feedback_pk PRIMARY KEY (id_feedback)
                )
                """;

        private static final String INSERT_FEEDBACK = """
                INSERT INTO FEEDBACK (voto, commento, id_servizio)
                VALUES (?, ?, ?)
                """;

        // Collega un prodotto al servizio (tabella CONTIENE)
        private static final String INSERT_CONTIENE = """
                INSERT INTO CONTIENE (id_servizio, id_prodotto, quantita)
                VALUES (?, ?, ?)
                """;

        // Registra la transizione di stato iniziale
        private static final String INSERT_TRANSIZIONE = """
                INSERT INTO TRANSIZIONE_STATO (id_servizio, id_stato, data_transizione)
                VALUES (?, 1, NOW())
                """;

        // Aggiorna il contatore della tessera fedeltà
        private static final String UPDATE_TESSERA = """
                UPDATE TESSERA_FEDELTA
                SET    ordini_effettuati = ordini_effettuati + 1,
                       data_ultimo_ordine = DATE(NOW())
                WHERE  id_utente = ?
                """;

        private static final String DELIVERY_QUERY = """
                SELECT s.id_servizio,
                       'DELIVERY' AS tipo,
                       s.totale_costo, s.sconto_applicato,
                       ss.nome_stato, s.giorno_creazione, s.ora_creazione
                FROM   SERVIZIO s
                JOIN   STATO_SERVIZIO ss ON s.id_stato = ss.id_stato
                JOIN   DELIVERY d ON s.id_servizio = d.id_servizio
                ORDER BY s.giorno_creazione DESC, s.ora_creazione DESC
                """;

        /**
         * Restituisce lo storico degli ordini di un utente.
         */
        public static List<Servizio> storicoUtente(Connection connection, int idUtente) {
            var lista = new ArrayList<Servizio>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, STORICO_QUERY, idUtente);
                var rs   = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    lista.add(new Servizio(
                        rs.getInt("id_servizio"),
                        rs.getString("tipo"),
                        rs.getDouble("totale_costo"),
                        rs.getDouble("sconto_applicato"),
                        rs.getString("nome_stato"),
                        rs.getDate("giorno_creazione").toLocalDate(),
                        rs.getTime("ora_creazione").toLocalTime()
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return lista;
        }

        public static List<Servizio> delivery(Connection connection) {
            var lista = new ArrayList<Servizio>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, DELIVERY_QUERY);
                var rs   = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    lista.add(new Servizio(
                        rs.getInt("id_servizio"),
                        rs.getString("tipo"),
                        rs.getDouble("totale_costo"),
                        rs.getDouble("sconto_applicato"),
                        rs.getString("nome_stato"),
                        rs.getDate("giorno_creazione").toLocalDate(),
                        rs.getTime("ora_creazione").toLocalTime()
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return lista;
        }

        /**
         * Crea un nuovo ordine (delivery o asporto) con i prodotti scelti.
         * map prodotti: id_prodotto -> quantita
         * tipo: "DELIVERY" o "ASPORTO"
         * indirizzo: usato solo per DELIVERY
         * Restituisce l'id del servizio creato.
         */
        public static int creaOrdine(Connection connection,
                                     int idUtente,
                                     String tipo,
                                     String indirizzoConsegna,
                                     Map<Integer, Integer> prodotti) {
            try {
                // Calcola il totale sommando prezzi dei prodotti
                double totale = calcolaTotale(connection, prodotti);
                double sconto = calcolaSconto(connection, idUtente, totale);
                double totaleScontato = totale * (1 - sconto / 100.0);

                // 1. Inserisci il servizio base
                var stmt = connection.prepareStatement(
                    INSERT_SERVIZIO, java.sql.Statement.RETURN_GENERATED_KEYS);
                stmt.setDouble(1, sconto);
                stmt.setDouble(2, totaleScontato);
                stmt.setInt(3, idUtente);
                stmt.executeUpdate();

                var keys = stmt.getGeneratedKeys();
                if (!keys.next()) throw new DAOException("Creazione servizio fallita");
                int idServizio = keys.getInt(1);

                // 2. Inserisci il tipo specifico
                if ("DELIVERY".equals(tipo)) {
                    try (var s = DAOUtils.prepareStatement(
                            connection, INSERT_DELIVERY, indirizzoConsegna, idServizio)) {
                        s.executeUpdate();
                    }
                } else {
                    try (var s = DAOUtils.prepareStatement(
                            connection, INSERT_ASPORTO, idServizio)) {
                        s.executeUpdate();
                    }
                }

                // 3. Inserisci i prodotti nella tabella CONTIENE
                for (var entry : prodotti.entrySet()) {
                    try (var s = DAOUtils.prepareStatement(
                            connection, INSERT_CONTIENE,
                            idServizio, entry.getKey(), entry.getValue())) {
                        s.executeUpdate();
                    }
                }

                // 4. Registra la transizione di stato iniziale
                try (var s = DAOUtils.prepareStatement(
                        connection, INSERT_TRANSIZIONE, idServizio)) {
                    s.executeUpdate();
                }

                // 5. Aggiorna la tessera fedeltà
                try (var s = DAOUtils.prepareStatement(
                        connection, UPDATE_TESSERA, idUtente)) {
                    s.executeUpdate();
                }

                return idServizio;

            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static int prenotaTavolo(Connection connection,
                                        int idUtente,
                                        LocalDate giorno,
                                        LocalTime ora,
                                        int persone) {
            try {
                preparaPrenotazioni(connection);
                int idServizio = creaServizioBase(connection, idUtente, 0, 0);

                try (var s = DAOUtils.prepareStatement(
                        connection, INSERT_PRENOTAZIONE,
                        persone, Date.valueOf(giorno), Time.valueOf(ora), idServizio)) {
                    s.executeUpdate();
                }

                inserisciTransizioneIniziale(connection, idServizio);
                return idServizio;
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void lasciaFeedback(Connection connection,
                                          int idServizio,
                                          int voto,
                                          String commento) {
            try {
                preparaFeedback(connection);
                try (var s = DAOUtils.prepareStatement(
                        connection, INSERT_FEEDBACK, voto, commento, idServizio)) {
                    s.executeUpdate();
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        // Calcola il totale dell'ordine leggendo i prezzi dal DB
        private static double calcolaTotale(Connection connection,
                                            Map<Integer, Integer> prodotti) throws SQLException {
            double totale = 0;
            String query  = "SELECT costo_prodotto FROM PRODOTTO WHERE id_prodotto = ?";
            for (var entry : prodotti.entrySet()) {
                try (var s  = DAOUtils.prepareStatement(connection, query, entry.getKey());
                     var rs = s.executeQuery()) {
                    if (rs.next()) totale += rs.getDouble(1) * entry.getValue();
                }
            }
            return totale;
        }

        // Controlla se spetta lo sconto fedeltà (ogni 5 ordini il 6° è scontato 20%)
        private static double calcolaSconto(Connection connection,
                                            int idUtente, double totale) throws SQLException {
            String query = """
                    SELECT ordini_effettuati FROM TESSERA_FEDELTA WHERE id_utente = ?
                    """;
            try (var s  = DAOUtils.prepareStatement(connection, query, idUtente);
                 var rs = s.executeQuery()) {
                if (rs.next()) {
                    int ordini = rs.getInt("ordini_effettuati");
                    // Il 6°, 11°, 16°, ... ordine è scontato
                    if (ordini > 0 && ordini % 5 == 0) return 20.0;
                }
            }
            return 0.0;
        }

        private static int creaServizioBase(Connection connection, int idUtente,
                                            double totale, double sconto)
                throws SQLException {
            var stmt = connection.prepareStatement(
                INSERT_SERVIZIO, java.sql.Statement.RETURN_GENERATED_KEYS);
            stmt.setDouble(1, sconto);
            stmt.setDouble(2, totale);
            stmt.setInt(3, idUtente);
            stmt.executeUpdate();

            var keys = stmt.getGeneratedKeys();
            if (!keys.next()) throw new DAOException("Creazione servizio fallita");
            return keys.getInt(1);
        }

        private static void inserisciTransizioneIniziale(Connection connection,
                                                        int idServizio)
                throws SQLException {
            try (var s = DAOUtils.prepareStatement(
                    connection, INSERT_TRANSIZIONE, idServizio)) {
                s.executeUpdate();
            }
        }

        private static void preparaPrenotazioni(Connection connection)
                throws SQLException {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_PRENOTAZIONE);
            }
            aggiungiColonnaSeManca(connection, "PRENOTAZIONE_TAVOLO",
                "numero_persone", "INT NOT NULL DEFAULT 1");
            aggiungiColonnaSeManca(connection, "PRENOTAZIONE_TAVOLO",
                "data_prenotazione", "DATE NULL");
            aggiungiColonnaSeManca(connection, "PRENOTAZIONE_TAVOLO",
                "ora_prenotazione", "TIME NULL");
            rendiNullableSeEsiste(connection, "PRENOTAZIONE_TAVOLO",
                "dat_giorno", "DATE");
            rendiNullableSeEsiste(connection, "PRENOTAZIONE_TAVOLO",
                "ora_prenotazione", "TIME");
            rendiNullableSeEsiste(connection, "PRENOTAZIONE_TAVOLO",
                "id_tavolo", "INT");
        }

        private static void preparaFeedback(Connection connection)
                throws SQLException {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_FEEDBACK);
            }
            aggiungiColonnaSeManca(connection, "FEEDBACK",
                "voto", "INT NOT NULL DEFAULT 5");
            aggiungiColonnaSeManca(connection, "FEEDBACK",
                "commento", "VARCHAR(500) NULL");
        }

        private static void aggiungiColonnaSeManca(Connection connection,
                                                  String tabella,
                                                  String colonna,
                                                  String definizione)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, tabella, colonna)) {
                if (columns.next()) return;
            }
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + tabella + " ADD COLUMN "
                    + colonna + " " + definizione);
            }
        }

        private static void rendiNullableSeEsiste(Connection connection,
                                                  String tabella,
                                                  String colonna,
                                                  String definizione)
                throws SQLException {
            if (!colonnaEsiste(connection, tabella, colonna)) return;
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + tabella + " MODIFY COLUMN "
                    + colonna + " " + definizione + " NULL");
            }
        }

        private static boolean colonnaEsiste(Connection connection,
                                             String tabella,
                                             String colonna)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, tabella, colonna)) {
                if (columns.next()) return true;
            }
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, tabella.toLowerCase(), colonna)) {
                return columns.next();
            }
        }
    }
}
