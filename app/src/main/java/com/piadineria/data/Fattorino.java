package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Account dei fattorini creati dall'amministratore.
 */
public final class Fattorino {

    public final int id;
    public final String nome;
    public final String cognome;
    public final String email;

    public Fattorino(int id, String nome, String cognome, String email) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
    }

    public String nomeCompleto() {
        return nome + " " + cognome;
    }

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + email + ")";
    }

    public static final class DAO {

        private static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS FATTORINO (
                    id_fattorino INT NOT NULL AUTO_INCREMENT,
                    nome VARCHAR(64) NOT NULL,
                    cognome VARCHAR(64) NOT NULL,
                    email VARCHAR(128) NOT NULL,
                    password VARCHAR(128) NOT NULL,
                    CONSTRAINT fattorino_pk PRIMARY KEY (id_fattorino),
                    CONSTRAINT fattorino_email_unique UNIQUE (email)
                )
                """;

        private static final String LOGIN_QUERY = """
                SELECT id_fattorino, nome, cognome, email
                FROM   FATTORINO
                WHERE  email = ?
                AND    password = ?
                """;

        private static final String REGISTER_QUERY = """
                INSERT INTO FATTORINO (nome, cognome, email, password)
                VALUES (?, ?, ?, ?)
                """;

        private static final String COUNT_EMAIL_QUERY = """
                SELECT COUNT(*)
                FROM   FATTORINO
                WHERE  LOWER(email) = LOWER(?)
                """;

        private static final String LIST_QUERY = """
                SELECT id_fattorino, nome, cognome, email
                FROM   FATTORINO
                ORDER BY cognome, nome, email
                """;

        private static final String DELETE_QUERY = """
                DELETE FROM FATTORINO
                WHERE id_fattorino = ?
                """;

        public static void preparaTabella(Connection connection) {
            creaTabellaSeServe(connection);
        }

        public static Optional<Fattorino> login(Connection connection,
                                                String email,
                                                String password) {
            creaTabellaSeServe(connection);
            try (
                var stmt = DAOUtils.prepareStatement(connection, LOGIN_QUERY, email, password);
                var rs = stmt.executeQuery()
            ) {
                if (rs.next()) {
                    return Optional.of(new Fattorino(
                        rs.getInt("id_fattorino"),
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

        public static int registra(Connection connection,
                                   String nome,
                                   String cognome,
                                   String email,
                                   String password) {
            creaTabellaSeServe(connection);
            try {
                if (emailEsiste(connection, email)) {
                    throw new DAOException("Email fattorino gia registrata");
                }

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
                if (!keys.next()) throw new DAOException("Inserimento fattorino fallito");
                return keys.getInt(1);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static List<Fattorino> lista(Connection connection) {
            creaTabellaSeServe(connection);
            var fattorini = new ArrayList<Fattorino>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY);
                var rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    fattorini.add(new Fattorino(
                        rs.getInt("id_fattorino"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return fattorini;
        }

        public static void elimina(Connection connection, int idFattorino) {
            creaTabellaSeServe(connection);
            try (var stmt = DAOUtils.prepareStatement(connection, DELETE_QUERY, idFattorino)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static void creaTabellaSeServe(Connection connection) {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE);
                aggiungiColonnaSeManca(connection, "nome",
                    "VARCHAR(64) NOT NULL DEFAULT ''");
                aggiungiColonnaSeManca(connection, "cognome",
                    "VARCHAR(64) NOT NULL DEFAULT ''");
                aggiungiColonnaSeManca(connection, "email",
                    "VARCHAR(128) NOT NULL DEFAULT ''");
                aggiungiColonnaSeManca(connection, "password",
                    "VARCHAR(128) NOT NULL DEFAULT ''");
                rendiNullableSeEsiste(connection, "CF", "VARCHAR(16)");
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static boolean emailEsiste(Connection connection, String email)
                throws SQLException {
            try (var stmt = DAOUtils.prepareStatement(connection, COUNT_EMAIL_QUERY, email);
                 var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }

        private static void aggiungiColonnaSeManca(Connection connection,
                                                  String colonna,
                                                  String definizione)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "FATTORINO", colonna)) {
                if (columns.next()) return;
            }
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "fattorino", colonna)) {
                if (columns.next()) return;
            }
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE FATTORINO ADD COLUMN "
                    + colonna + " " + definizione);
            }
        }

        private static void rendiNullableSeEsiste(Connection connection,
                                                  String colonna,
                                                  String definizione)
                throws SQLException {
            if (!colonnaEsiste(connection, colonna)) return;
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE FATTORINO MODIFY COLUMN "
                    + colonna + " " + definizione + " NULL");
            }
        }

        private static boolean colonnaEsiste(Connection connection, String colonna)
                throws SQLException {
            var metaData = connection.getMetaData();
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "FATTORINO", colonna)) {
                if (columns.next()) return true;
            }
            try (var columns = metaData.getColumns(
                    connection.getCatalog(), null, "fattorino", colonna)) {
                return columns.next();
            }
        }
    }
}
