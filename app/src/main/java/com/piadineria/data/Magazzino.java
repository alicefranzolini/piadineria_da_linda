package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Magazzino {

    public final int id;
    public final String nome;
    public final int quantita;
    public final int sogliaMinima;
    public final String fornitore;

    public Magazzino(int id, String nome, int quantita,
                     int sogliaMinima, String fornitore) {
        this.id = id;
        this.nome = nome;
        this.quantita = quantita;
        this.sogliaMinima = sogliaMinima;
        this.fornitore = fornitore;
    }

    @Override
    public String toString() {
        var stato = quantita <= sogliaMinima ? " SOTTO SOGLIA" : "";
        return String.format("%s - %d pezzi (soglia %d, %s)%s",
            nome, quantita, sogliaMinima, fornitore, stato);
    }

    public static final class DAO {
        private static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS MAGAZZINO_APP (
                    id_magazzino INT NOT NULL AUTO_INCREMENT,
                    nome VARCHAR(128) NOT NULL,
                    quantita INT NOT NULL DEFAULT 200,
                    soglia_minima INT NOT NULL DEFAULT 20,
                    fornitore VARCHAR(128) NOT NULL DEFAULT 'Fornitore generale',
                    CONSTRAINT magazzino_app_pk PRIMARY KEY (id_magazzino),
                    CONSTRAINT magazzino_app_nome_unique UNIQUE (nome)
                )
                """;

        private static final String INSERT_SEED = """
                INSERT INTO MAGAZZINO_APP (nome, quantita, soglia_minima, fornitore)
                VALUES (?, 200, ?, ?)
                ON DUPLICATE KEY UPDATE nome = nome
                """;

        private static final String LIST_QUERY = """
                SELECT id_magazzino, nome, quantita, soglia_minima, fornitore
                FROM MAGAZZINO_APP
                ORDER BY nome
                """;

        private static final String UPDATE_QUERY = """
                UPDATE MAGAZZINO_APP
                SET quantita = ?
                WHERE id_magazzino = ?
                """;

        private static final String ADD_QUERY = """
                INSERT INTO MAGAZZINO_APP (nome, quantita, soglia_minima, fornitore)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    quantita = VALUES(quantita),
                    soglia_minima = VALUES(soglia_minima),
                    fornitore = VALUES(fornitore)
                """;

        private static final String DECREMENT_QUERY = """
                UPDATE MAGAZZINO_APP
                SET quantita = GREATEST(quantita - ?, 0)
                WHERE LOWER(nome) = LOWER(?)
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
                        rs.getInt("id_magazzino"),
                        rs.getString("nome"),
                        rs.getInt("quantita"),
                        rs.getInt("soglia_minima"),
                        rs.getString("fornitore")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
            return righe;
        }

        public static void aggiorna(Connection connection, int idMagazzino, int quantita) {
            prepara(connection);
            try (var stmt = DAOUtils.prepareStatement(
                    connection, UPDATE_QUERY, quantita, idMagazzino)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void aggiungi(Connection connection, String nome, int quantita,
                                    int soglia, String fornitore) {
            prepara(connection);
            try (var stmt = DAOUtils.prepareStatement(
                    connection, ADD_QUERY, nome, quantita, soglia, fornitore)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void decrementa(Connection connection, Map<Integer, Integer> prodotti) {
            prepara(connection);
            try {
                for (var entry : prodotti.entrySet()) {
                    var ingredienti = ingredientiPerProdotto(connection, entry.getKey());
                    for (var ingrediente : ingredienti.entrySet()) {
                        try (var stmt = DAOUtils.prepareStatement(
                                connection, DECREMENT_QUERY,
                                ingrediente.getValue() * entry.getValue(),
                                ingrediente.getKey())) {
                            stmt.executeUpdate();
                        }
                    }
                }
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
            seed(connection);
        }

        private static void seed(Connection connection) {
            var ingredienti = List.of(
                "Base piadina", "Prosciutto crudo", "Prosciutto cotto", "Salame",
                "Speck", "Salsiccia", "Pollo", "Tonno", "Bresaola", "Kebab",
                "Squacquerone", "Mozzarella", "Fontina", "Brie", "Scamorza",
                "Rucola", "Pomodoro", "Verdure grigliate", "Funghi", "Cipolla",
                "Peperoni", "Basilico", "Origano", "Maionese", "Salsa yogurt",
                "Salsa Linda", "Nutella", "Patatine", "Acqua naturale",
                "Acqua frizzante", "Coca-Cola", "Coca-Cola Zero", "Fanta",
                "Sprite", "Te Limone", "Te Pesca", "Birra Chiara",
                "Birra Artigianale"
            );
            try {
                for (var nome : ingredienti) {
                    try (var stmt = DAOUtils.prepareStatement(
                            connection, INSERT_SEED, nome, 20, fornitore(nome))) {
                        stmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static String fornitore(String nome) {
            var n = nome.toLowerCase(Locale.ROOT);
            if (n.contains("acqua") || n.contains("coca") || n.contains("fanta")
                || n.contains("sprite") || n.contains("birra") || n.contains("te ")) {
                return "Fornitore bevande";
            }
            if (n.contains("prosciutto") || n.contains("salame") || n.contains("speck")
                || n.contains("salsiccia") || n.contains("pollo") || n.contains("tonno")
                || n.contains("bresaola") || n.contains("kebab")) {
                return "Fornitore carni";
            }
            if (n.contains("mozzarella") || n.contains("fontina")
                || n.contains("brie") || n.contains("squacquerone")
                || n.contains("scamorza")) {
                return "Fornitore formaggi";
            }
            return "Fornitore generale";
        }

        private static Map<String, Integer> ingredientiPerProdotto(Connection connection,
                                                                   int idProdotto)
                throws SQLException {
            String nome = "";
            String descrizione = "";
            try (var stmt = DAOUtils.prepareStatement(
                    connection,
                    "SELECT nome, descrizione FROM PRODOTTO WHERE id_prodotto = ?",
                    idProdotto);
                 var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nome = rs.getString("nome");
                    descrizione = rs.getString("descrizione");
                }
            }

            var ingredienti = new LinkedHashMap<String, Integer>();
            var testo = (nome + " " + descrizione).toLowerCase(Locale.ROOT);
            if (testo.contains("piadina")) ingredienti.put("Base piadina", 1);
            aggiungiSeContiene(ingredienti, testo, "crudo", "Prosciutto crudo");
            aggiungiSeContiene(ingredienti, testo, "cotto", "Prosciutto cotto");
            aggiungiSeContiene(ingredienti, testo, "salame", "Salame");
            aggiungiSeContiene(ingredienti, testo, "speck", "Speck");
            aggiungiSeContiene(ingredienti, testo, "salsiccia", "Salsiccia");
            aggiungiSeContiene(ingredienti, testo, "pollo", "Pollo");
            aggiungiSeContiene(ingredienti, testo, "tonno", "Tonno");
            aggiungiSeContiene(ingredienti, testo, "bresaola", "Bresaola");
            aggiungiSeContiene(ingredienti, testo, "kebab", "Kebab");
            aggiungiSeContiene(ingredienti, testo, "squacquerone", "Squacquerone");
            aggiungiSeContiene(ingredienti, testo, "mozzarella", "Mozzarella");
            aggiungiSeContiene(ingredienti, testo, "fontina", "Fontina");
            aggiungiSeContiene(ingredienti, testo, "brie", "Brie");
            aggiungiSeContiene(ingredienti, testo, "scamorza", "Scamorza");
            aggiungiSeContiene(ingredienti, testo, "rucola", "Rucola");
            aggiungiSeContiene(ingredienti, testo, "pomodoro", "Pomodoro");
            aggiungiSeContiene(ingredienti, testo, "verdure", "Verdure grigliate");
            aggiungiSeContiene(ingredienti, testo, "funghi", "Funghi");
            aggiungiSeContiene(ingredienti, testo, "cipolla", "Cipolla");
            aggiungiSeContiene(ingredienti, testo, "peperoni", "Peperoni");
            aggiungiSeContiene(ingredienti, testo, "basilico", "Basilico");
            aggiungiSeContiene(ingredienti, testo, "origano", "Origano");
            aggiungiSeContiene(ingredienti, testo, "maionese", "Maionese");
            aggiungiSeContiene(ingredienti, testo, "yogurt", "Salsa yogurt");
            aggiungiSeContiene(ingredienti, testo, "linda", "Salsa Linda");
            aggiungiSeContiene(ingredienti, testo, "nutella", "Nutella");
            aggiungiSeContiene(ingredienti, testo, "patatine", "Patatine");
            aggiungiSeContiene(ingredienti, testo, "acqua naturale", "Acqua naturale");
            aggiungiSeContiene(ingredienti, testo, "acqua frizzante", "Acqua frizzante");
            aggiungiSeContiene(ingredienti, testo, "coca-cola zero", "Coca-Cola Zero");
            aggiungiSeContiene(ingredienti, testo, "coca-cola", "Coca-Cola");
            aggiungiSeContiene(ingredienti, testo, "fanta", "Fanta");
            aggiungiSeContiene(ingredienti, testo, "sprite", "Sprite");
            aggiungiSeContiene(ingredienti, testo, "te limone", "Te Limone");
            aggiungiSeContiene(ingredienti, testo, "te pesca", "Te Pesca");
            aggiungiSeContiene(ingredienti, testo, "birra chiara", "Birra Chiara");
            aggiungiSeContiene(ingredienti, testo, "birra artigianale", "Birra Artigianale");

            if (ingredienti.isEmpty() && !nome.isBlank()) ingredienti.put(nome, 1);
            return ingredienti;
        }

        private static void aggiungiSeContiene(Map<String, Integer> ingredienti,
                                               String testo, String token,
                                               String nomeIngrediente) {
            if (testo.contains(token)) ingredienti.put(nomeIngrediente, 1);
        }
    }
}
