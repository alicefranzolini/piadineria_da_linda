package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Rappresenta un cliente registrato della piadineria.
 */
public final class Utente {

    public final int    id;
    public final String nome;
    public final String cognome;
    public final String email;

    public Utente(int id, String nome, String cognome, String email) {
        this.id      = id;
        this.nome    = nome;
        this.cognome = cognome;
        this.email   = email;
    }

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + email + ")";
    }

    // -------------------------------------------------------------------------
    // DAO
    // -------------------------------------------------------------------------
    public static final class DAO {

        private static final String LOGIN_QUERY = """
                SELECT id_utente, nome, cognome, email
                FROM   UTENTE
                WHERE  email    = ?
                AND    password = ?
                """;

        private static final String REGISTER_QUERY = """
                INSERT INTO UTENTE (nome, cognome, email, password)
                VALUES (?, ?, ?, ?)
                """;

        private static final String TESSERA_QUERY = """
                INSERT INTO TESSERA_FEDELTA
                    (numero_tessera, ordini_effettuati, id_utente)
                VALUES (CONCAT('TF', LPAD(?, 6, '0')), 0, ?)
                """;

        /**
         * Tenta il login. Restituisce Optional.empty() se le credenziali
         * non corrispondono, altrimenti l'utente trovato.
         */
        public static Optional<Utente> login(Connection connection,
                                             String email, String password) {
            try (
                var stmt = DAOUtils.prepareStatement(connection, LOGIN_QUERY, email, password);
                var rs   = stmt.executeQuery()
            ) {
                if (rs.next()) {
                    return Optional.of(new Utente(
                        rs.getInt("id_utente"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email")
                    ));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        /**
         * Registra un nuovo utente e crea automaticamente la sua tessera fedeltà.
         * Restituisce l'id generato.
         */
        public static int registra(Connection connection,
                                   String nome, String cognome,
                                   String email, String password) {
            try {
                // Passo 1: inserisci utente - chiediamo l'id generato automaticamente
                var stmt = connection.prepareStatement(
                    REGISTER_QUERY,
                    java.sql.Statement.RETURN_GENERATED_KEYS
                );
                stmt.setString(1, nome);
                stmt.setString(2, cognome);
                stmt.setString(3, email);
                stmt.setString(4, password);
                stmt.executeUpdate();

                var keys = stmt.getGeneratedKeys();
                if (!keys.next()) throw new DAOException("Inserimento utente fallito");
                int nuovoId = keys.getInt(1);

                // Passo 2: crea tessera fedeltà collegata
                try (var stmtT = DAOUtils.prepareStatement(
                        connection, TESSERA_QUERY, nuovoId, nuovoId)) {
                    stmtT.executeUpdate();
                }

                return nuovoId;
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }
    }
}
