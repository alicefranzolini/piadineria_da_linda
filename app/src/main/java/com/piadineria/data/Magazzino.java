package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Magazzino {

    public final int idProdotto;
    public final String nomeProdotto;
    public final int quantita;

    public Magazzino(int idProdotto, String nomeProdotto, int quantita) {
        this.idProdotto = idProdotto;
        this.nomeProdotto = nomeProdotto;
        this.quantita = quantita;
    }

    @Override
    public String toString() {
        return nomeProdotto + " - " + quantita + " pezzi";
    }

    public static final class DAO {
        private static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS MAGAZZINO (
                    id_prodotto INT NOT NULL,
                    quantita INT NOT NULL DEFAULT 200,
                    CONSTRAINT magazzino_pk PRIMARY KEY (id_prodotto)
                )
                """;

        private static final String SYNC_PRODUCTS = """
                INSERT INTO MAGAZZINO (id_prodotto, quantita)
                SELECT p.id_prodotto, 200
                FROM PRODOTTO p
                WHERE NOT EXISTS (
                    SELECT 1 FROM MAGAZZINO m WHERE m.id_prodotto = p.id_prodotto
                )
                """;

        private static final String LIST_QUERY = """
                SELECT p.id_prodotto, p.nome, m.quantita
                FROM MAGAZZINO m
                JOIN PRODOTTO p ON m.id_prodotto = p.id_prodotto
                WHERE p.disponibilita = TRUE
                ORDER BY p.nome
                """;

        private static final String UPDATE_QUERY = """
                UPDATE MAGAZZINO
                SET quantita = ?
                WHERE id_prodotto = ?
                """;

        private static final String DECREMENT_QUERY = """
                UPDATE MAGAZZINO
                SET quantita = GREATEST(quantita - ?, 0)
                WHERE id_prodotto = ?
                """;

        public static List<Magazzino> lista(Connection connection) {
            prepara(connection);
            var righe = new ArrayList<Magazzino>();
            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY);
                var rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    righe.add(new Magazzino(
                        rs.getInt("id_prodotto"),
                        rs.getString("nome"),
                        rs.getInt("quantita")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return righe;
        }

        public static void aggiorna(Connection connection, int idProdotto, int quantita) {
            prepara(connection);
            try (var stmt = DAOUtils.prepareStatement(
                    connection, UPDATE_QUERY, quantita, idProdotto)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void decrementa(Connection connection, Map<Integer, Integer> prodotti) {
            prepara(connection);
            try {
                for (var entry : prodotti.entrySet()) {
                    try (var stmt = DAOUtils.prepareStatement(
                            connection, DECREMENT_QUERY, entry.getValue(), entry.getKey())) {
                        stmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void prepara(Connection connection) {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(CREATE_TABLE);
                stmt.executeUpdate(SYNC_PRODUCTS);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }
    }
}
