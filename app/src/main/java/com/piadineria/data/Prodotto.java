package com.piadineria.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta un prodotto del menù della piadineria.
 * Come spiega il README: l'oggetto Java non deve corrispondere
 * esattamente alla tabella del DB. Qui aggreghiamo anche il nome
 * della categoria per comodità di visualizzazione.
 */
public final class Prodotto {

    public final int id;
    public final String nome;
    public final String descrizione;
    public final double prezzo;
    public final String nomeCategoria;
    public final boolean disponibile;

    public Prodotto(int id, String nome, String descrizione,
                    double prezzo, String nomeCategoria, boolean disponibile) {
        this.id            = id;
        this.nome          = nome;
        this.descrizione   = descrizione;
        this.prezzo        = prezzo;
        this.nomeCategoria = nomeCategoria;
        this.disponibile   = disponibile;
    }

    // toString usato dalla JList di Swing per mostrare il nome nella lista
    @Override
    public String toString() {
        return String.format("%s - %.2f€  [%s]", nome, prezzo, nomeCategoria);
    }

    // -------------------------------------------------------------------------
    // DAO interno: unico punto di accesso alle query su PRODOTTO
    // -------------------------------------------------------------------------
    public static final class DAO {

        // Query che lista tutti i prodotti disponibili con il nome della categoria
        private static final String LIST_QUERY = """
                SELECT p.id_prodotto, p.nome, p.descrizione,
                       p.costo_prodotto, c.nome_categoria, p.disponibilita
                FROM   PRODOTTO p
                JOIN   CATEGORIA_PRODOTTO c ON p.id_categoria = c.id_categoria
                WHERE  p.disponibilita = TRUE
                ORDER BY
                    CASE WHEN LOWER(p.nome) = 'piadina componibile' THEN 0 ELSE 1 END,
                    c.nome_categoria, p.nome
                """;

        private static final String FIND_CATEGORY = """
                SELECT id_categoria
                FROM   CATEGORIA_PRODOTTO
                WHERE  LOWER(nome_categoria) = LOWER(?)
                LIMIT  1
                """;

        private static final String INSERT_CATEGORY = """
                INSERT INTO CATEGORIA_PRODOTTO (nome_categoria)
                VALUES (?)
                """;

        private static final String COUNT_PRODUCT = """
                SELECT COUNT(*)
                FROM   PRODOTTO
                WHERE  LOWER(nome) = LOWER(?)
                """;

        private static final String INSERT_PRODUCT = """
                INSERT INTO PRODOTTO
                    (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
                VALUES (?, ?, ?, TRUE, ?)
                """;

        private static final String INSERT_CUSTOM_PRODUCT = """
                INSERT INTO PRODOTTO
                    (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
                VALUES (?, ?, ?, FALSE, ?)
                """;

        private static final String DISABLE_PRODUCT = """
                UPDATE PRODOTTO
                SET disponibilita = FALSE
                WHERE id_prodotto = ?
                """;

        /**
         * Restituisce la lista di tutti i prodotti disponibili.
         * Usato nella schermata principale per mostrare il menù.
         */
        public static List<Prodotto> listDisponibili(Connection connection) {
            seedMenu(connection);
            var prodotti = new ArrayList<Prodotto>();

            try (
                var stmt = DAOUtils.prepareStatement(connection, LIST_QUERY);
                var rs   = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    prodotti.add(new Prodotto(
                        rs.getInt("id_prodotto"),
                        rs.getString("nome"),
                        rs.getString("descrizione"),
                        rs.getDouble("costo_prodotto"),
                        rs.getString("nome_categoria"),
                        rs.getBoolean("disponibilita")
                    ));
                }
            } catch (SQLException e) {
                throw new DAOException(e);
            }

            return prodotti;
        }

        public static int registra(Connection connection, String nome,
                                   String descrizione, double prezzo,
                                   String categoria) {
            try {
                int idCategoria = getOrCreateCategoria(connection, categoria);
                if (prodottoEsiste(connection, nome)) {
                    throw new DAOException("Prodotto gia presente");
                }

                var stmt = connection.prepareStatement(
                    INSERT_PRODUCT, java.sql.Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nome);
                stmt.setString(2, descrizione);
                stmt.setDouble(3, prezzo);
                stmt.setInt(4, idCategoria);
                stmt.executeUpdate();

                var keys = stmt.getGeneratedKeys();
                if (!keys.next()) throw new DAOException("Creazione prodotto fallita");
                return keys.getInt(1);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static Prodotto creaComponibile(Connection connection,
                                               String nomeVisualizzato,
                                               String descrizione,
                                               double prezzo) {
            try {
                int idCategoria = getOrCreateCategoria(connection, "Cibo");
                String nomeDb = nomeVisualizzato + " " + System.nanoTime();
                var stmt = connection.prepareStatement(
                    INSERT_CUSTOM_PRODUCT, java.sql.Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nomeDb);
                stmt.setString(2, descrizione);
                stmt.setDouble(3, prezzo);
                stmt.setInt(4, idCategoria);
                stmt.executeUpdate();

                var keys = stmt.getGeneratedKeys();
                if (!keys.next()) throw new DAOException("Creazione piadina componibile fallita");
                return new Prodotto(keys.getInt(1), nomeVisualizzato, descrizione, prezzo, "Cibo", true);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        public static void rimuoviDalMenu(Connection connection, int idProdotto) {
            try (var stmt = DAOUtils.prepareStatement(connection, DISABLE_PRODUCT, idProdotto)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static void seedMenu(Connection connection) {
            try {
                int cibo = getOrCreateCategoria(connection, "Cibo");
                int bevande = getOrCreateCategoria(connection, "Bevande");

                inserisciSeManca(connection, "Piadina Classica",
                    "Crudo, squacquerone e rucola", 6.50, cibo);
                inserisciSeManca(connection, "Piadina Vegetariana",
                    "Verdure grigliate, squacquerone e rucola", 6.00, cibo);
                inserisciSeManca(connection, "Piadina Romagnola",
                    "Prosciutto crudo, squacquerone e rucola", 7.00, cibo);
                inserisciSeManca(connection, "Piadina Cotto e Fontina",
                    "Prosciutto cotto e fontina", 6.20, cibo);
                inserisciSeManca(connection, "Piadina Speck e Brie",
                    "Speck, brie e funghi", 7.20, cibo);
                inserisciSeManca(connection, "Piadina Salsiccia e Cipolla",
                    "Salsiccia, cipolla caramellata e salsa Linda", 7.50, cibo);
                inserisciSeManca(connection, "Piadina Pollo",
                    "Pollo grigliato, insalata, pomodoro e maionese", 7.30, cibo);
                inserisciSeManca(connection, "Piadina Tonno",
                    "Tonno, pomodoro, insalata e maionese", 6.80, cibo);
                inserisciSeManca(connection, "Piadina Bresaola",
                    "Bresaola, rucola e grana", 7.80, cibo);
                inserisciSeManca(connection, "Piadina Piccante",
                    "Salame piccante, scamorza e peperoni", 7.10, cibo);
                inserisciSeManca(connection, "Piadina Caprese",
                    "Mozzarella, pomodoro, basilico e origano", 6.40, cibo);
                inserisciSeManca(connection, "Piadina Nutella",
                    "Piadina dolce con Nutella", 4.50, cibo);
                inserisciSeManca(connection, "Piadina componibile",
                    "Base piadina 5 euro, ingredienti a scelta", 5.00, cibo);
                inserisciSeManca(connection, "Rotolo Kebab",
                    "Carne kebab, insalata, pomodoro e salsa yogurt", 7.90, cibo);
                inserisciSeManca(connection, "Crescione Erbe",
                    "Crescione con erbe e mozzarella", 5.80, cibo);
                inserisciSeManca(connection, "Patatine Fritte",
                    "Porzione di patatine fritte", 3.50, cibo);

                inserisciSeManca(connection, "Acqua Naturale",
                    "Bottiglietta 50 cl", 1.20, bevande);
                inserisciSeManca(connection, "Acqua Frizzante",
                    "Bottiglietta 50 cl", 1.20, bevande);
                inserisciSeManca(connection, "Coca-Cola",
                    "Lattina 33 cl", 2.50, bevande);
                inserisciSeManca(connection, "Coca-Cola Zero",
                    "Lattina 33 cl", 2.50, bevande);
                inserisciSeManca(connection, "Fanta",
                    "Lattina 33 cl", 2.50, bevande);
                inserisciSeManca(connection, "Sprite",
                    "Lattina 33 cl", 2.50, bevande);
                inserisciSeManca(connection, "Te Limone",
                    "Bottiglietta 50 cl", 2.30, bevande);
                inserisciSeManca(connection, "Te Pesca",
                    "Bottiglietta 50 cl", 2.30, bevande);
                inserisciSeManca(connection, "Birra Chiara",
                    "Bottiglia 33 cl", 3.50, bevande);
                inserisciSeManca(connection, "Birra Artigianale",
                    "Bottiglia 33 cl", 4.50, bevande);
            } catch (SQLException e) {
                throw new DAOException(e);
            }
        }

        private static int getOrCreateCategoria(Connection connection, String nome)
                throws SQLException {
            try (var stmt = DAOUtils.prepareStatement(connection, FIND_CATEGORY, nome);
                 var rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id_categoria");
            }

            var stmt = connection.prepareStatement(
                INSERT_CATEGORY, java.sql.Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, nome);
            stmt.executeUpdate();
            var keys = stmt.getGeneratedKeys();
            if (!keys.next()) throw new DAOException("Creazione categoria fallita");
            return keys.getInt(1);
        }

        private static void inserisciSeManca(Connection connection, String nome,
                                            String descrizione, double prezzo,
                                            int idCategoria) throws SQLException {
            if (prodottoEsiste(connection, nome)) return;

            try (var stmt = DAOUtils.prepareStatement(
                    connection, INSERT_PRODUCT, nome, descrizione, prezzo, idCategoria)) {
                stmt.executeUpdate();
            }
        }

        private static boolean prodottoEsiste(Connection connection, String nome)
                throws SQLException {
            try (var stmt = DAOUtils.prepareStatement(connection, COUNT_PRODUCT, nome);
                 var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
