package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
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

        private static void creaTabellaSeServe(Connection connection) {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }
    }
}
