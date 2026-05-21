package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class IndirizzoConsegna {
    public final int id;
    public final String nome;
    public final String indirizzo;

    public IndirizzoConsegna(int id, String nome, String indirizzo) {
        this.id = id;
        this.nome = nome;
        this.indirizzo = indirizzo;
    }

    @Override
    public String toString() {
        return nome + " - " + indirizzo;
    }

    public static final class DAO {
        private static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS INDIRIZZO_CONSEGNA_APP (
                    id_indirizzo INT NOT NULL AUTO_INCREMENT,
                    id_utente INT NOT NULL,
                    nome VARCHAR(64) NOT NULL,
                    indirizzo VARCHAR(255) NOT NULL,
                    CONSTRAINT indirizzo_consegna_app_pk PRIMARY KEY (id_indirizzo)
                )
                """;

        private static final String LIST_QUERY = """
                SELECT id_indirizzo, nome, indirizzo
                FROM INDIRIZZO_CONSEGNA_APP
                WHERE id_utente = ?
                ORDER BY nome
                """;

        private static final String INSERT_QUERY = """
                INSERT INTO INDIRIZZO_CONSEGNA_APP (id_utente, nome, indirizzo)
                VALUES (?, ?, ?)
                """;

        public static List<IndirizzoConsegna> lista(Connection connection, int idUtente) {
            prepara(connection);
            var indirizzi = new ArrayList<IndirizzoConsegna>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY, idUtente);
                var rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    indirizzi.add(new IndirizzoConsegna(
                        rs.getInt("id_indirizzo"),
                        rs.getString("nome"),
                        rs.getString("indirizzo")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return indirizzi;
        }

        public static void salva(Connection connection, int idUtente,
                                 String nome, String indirizzo) {
            prepara(connection);
            try (var stmt = DAOUtils.prepareStatement(
                    connection, INSERT_QUERY, idUtente, nome, indirizzo)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static void prepara(Connection connection) {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }
    }
}
