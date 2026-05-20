package com.piadineria.view;

import com.piadineria.controller.Controller;
import com.piadineria.data.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * View nel pattern MVC: gestisce tutta l'interfaccia grafica con Swing.
 *
 * Usa un JFrame con CardLayout per passare tra le schermate senza aprire
 * nuove finestre. Ogni "scheda" è un JPanel con un nome univoco.
 *
 * La View NON contiene logica: ogni azione dell'utente viene delegata
 * al Controller tramite chiamate dirette.
 */
public final class View {

    // Colori del brand della piadineria
    private static final Color COLORE_PRIMARIO   = new Color(139, 69, 19);   // marrone
    private static final Color COLORE_SECONDARIO = new Color(255, 245, 230); // crema
    private static final Color COLORE_ACCENTO    = new Color(200, 100, 50);  // arancione

    // Frame principale e layout a schede
    private final JFrame     frame;
    private final JPanel     cards;
    private final CardLayout cardLayout;

    // Controller: viene impostato dopo la costruzione per evitare dipendenza circolare
    private Controller controller;

    // Componenti della Home che devono essere aggiornati dal controller
    private JLabel         labelBenvenuto;
    private JList<Prodotto> listaProdotti;
    private DefaultListModel<Prodotto> modelProdotti;
    private final List<Prodotto> prodottiDisponibili = new ArrayList<>();
    private String filtroProdotti = "CIBO";
    private JLabel         labelCarrello;
    private JList<RigaCarrello> listaCarrello;
    private DefaultListModel<RigaCarrello> modelCarrello;
    private JLabel labelTotaleCarrello;
    private JList<Servizio> listaStorico;
    private DefaultListModel<Servizio> modelStorico;
    private JList<StatisticaProdotto> listaStats;
    private DefaultListModel<StatisticaProdotto> modelStats;
    private JList<StatisticaProdotto> listaAdminStats;
    private DefaultListModel<StatisticaProdotto> modelAdminStats;
    private JList<Servizio> listaPrenotazioniTavoli;
    private DefaultListModel<Servizio> modelPrenotazioniTavoli;
    private JList<Servizio> listaDelivery;
    private DefaultListModel<Servizio> modelDelivery;
    private JLabel labelFattorino;
    private JTextField campoNomeFattorino;
    private JTextField campoCognomeFattorino;
    private JTextField campoEmailFattorino;
    private JPasswordField campoPasswordFattorino;
    private JList<Fattorino> listaFattorini;
    private DefaultListModel<Fattorino> modelFattorini;
    private JTextField campoNomeProdotto;
    private JTextField campoDescrizioneProdotto;
    private JTextField campoPrezzoProdotto;
    private JComboBox<String> comboCategoriaProdotto;

    public View() {
        frame = new JFrame("Piadineria Da Linda 🫓");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null); // centrata nello schermo

        cardLayout = new CardLayout();
        cards      = new JPanel(cardLayout);
        frame.add(cards);

        // Aggiungi tutte le schermate
        cards.add(creaSchermatLogin(),       "LOGIN");
        cards.add(creaSchermatRegistrazione(), "REGISTRAZIONE");
        cards.add(creaSchermatHome(),        "HOME");
        cards.add(creaSchermatStatistiche(), "STATISTICHE");
        cards.add(creaSchermatFattorino(),   "FATTORINO");
        cards.add(creaSchermatAdmin(),       "ADMIN");
    }

    /** Collega il controller alla view (chiamato dopo la creazione). */
    public void setController(Controller controller) {
        this.controller = controller;
    }

    /** Rende visibile la finestra. */
    public void show() {
        frame.setVisible(true);
    }

    // =========================================================================
    // Metodi chiamati dal Controller per aggiornare la View
    // =========================================================================

    public void mostraLogin() {
        cardLayout.show(cards, "LOGIN");
    }

    public void mostraHome(String nomeUtente) {
        labelBenvenuto.setText("Ciao, " + nomeUtente + "! 👋");
        cardLayout.show(cards, "HOME");
    }

    public void mostraProdotti(List<Prodotto> prodotti) {
        prodottiDisponibili.clear();
        prodottiDisponibili.addAll(prodotti);
        filtraProdotti();
    }

    public void mostraStorico(List<Servizio> storico) {
        modelStorico.clear();
        storico.forEach(modelStorico::addElement);
    }

    public void aggiornaCarrello(int numeroProdotti) {
        labelCarrello.setText("🛒 Carrello: " + numeroProdotti + " prodotti");
    }

    public void mostraCarrello(Map<Integer, Integer> carrello,
                               Map<Integer, Prodotto> prodotti) {
        modelCarrello.clear();
        double totale = 0;

        for (var entry : carrello.entrySet()) {
            var prodotto = prodotti.get(entry.getKey());
            if (prodotto == null) continue;
            int quantita = entry.getValue();
            double subtotale = prodotto.prezzo * quantita;
            totale += subtotale;
            modelCarrello.addElement(new RigaCarrello(
                prodotto.id,
                String.format("%dx %s - %.2f euro",
                    quantita, prodotto.nome, subtotale)
            ));
        }

        labelTotaleCarrello.setText(String.format("Totale: %.2f euro", totale));
    }

    public void mostraStatistiche(List<StatisticaProdotto> stats) {
        modelStats.clear();
        stats.forEach(modelStats::addElement);
        cardLayout.show(cards, "STATISTICHE");
    }

    public void mostraFattorino(String nome) {
        labelFattorino.setText("Area fattorino - " + nome);
        cardLayout.show(cards, "FATTORINO");
    }

    public void mostraAdmin(List<StatisticaProdotto> stats) {
        modelAdminStats.clear();
        stats.forEach(modelAdminStats::addElement);
        cardLayout.show(cards, "ADMIN");
    }

    public void mostraOrdiniDelivery(List<Servizio> ordini) {
        modelDelivery.clear();
        ordini.forEach(modelDelivery::addElement);
    }

    public void mostraDettaglioOrdine(DettaglioOrdine dettaglio) {
        var area = new JTextArea(dettaglio.testo(), 10, 35);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(frame, new JScrollPane(area),
            "Dettagli ordine", JOptionPane.INFORMATION_MESSAGE);
    }

    public void pulisciFormFattorino() {
        campoNomeFattorino.setText("");
        campoCognomeFattorino.setText("");
        campoEmailFattorino.setText("");
        campoPasswordFattorino.setText("");
    }

    public void pulisciFormProdotto() {
        campoNomeProdotto.setText("");
        campoDescrizioneProdotto.setText("");
        campoPrezzoProdotto.setText("");
        comboCategoriaProdotto.setSelectedItem("Cibo");
    }

    public void mostraFattorini(List<Fattorino> fattorini) {
        modelFattorini.clear();
        fattorini.forEach(modelFattorini::addElement);
    }

    public void mostraPrenotazioniTavoli(List<Servizio> prenotazioni) {
        modelPrenotazioniTavoli.clear();
        prenotazioni.forEach(modelPrenotazioniTavoli::addElement);
    }

    public void mostraErrore(String messaggio) {
        JOptionPane.showMessageDialog(frame, messaggio, "Errore",
                JOptionPane.ERROR_MESSAGE);
    }

    public void mostraMessaggio(String messaggio) {
        JOptionPane.showMessageDialog(frame, messaggio, "Piadineria Da Linda",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // Costruzione delle schermate (private)
    // =========================================================================

    private JPanel creaSchermatLogin() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLORE_SECONDARIO);
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Titolo
        var titolo = new JLabel("🫓 Piadineria Da Linda", SwingConstants.CENTER);
        titolo.setFont(new Font("Serif", Font.BOLD, 28));
        titolo.setForeground(COLORE_PRIMARIO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titolo, gbc);

        gbc.gridwidth = 1;

        // Email
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Email:"), gbc);
        var campoEmail = new JTextField(20);
        gbc.gridx = 1; panel.add(campoEmail, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc);
        var campoPassword = new JPasswordField(20);
        gbc.gridx = 1; panel.add(campoPassword, gbc);

        // Bottone login
        var btnLogin = creaBottone("Accedi");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        var panelRuoli = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        panelRuoli.setOpaque(false);
        var ruoloUtente = new JRadioButton("Utente");
        var ruoloFattorino = new JRadioButton("Fattorino");
        var ruoloAdmin = new JRadioButton("Admin");
        ruoloUtente.setSelected(true);
        for (var radio : List.of(ruoloUtente, ruoloFattorino, ruoloAdmin)) {
            radio.setOpaque(false);
            radio.setForeground(COLORE_PRIMARIO);
        }
        var gruppoRuoli = new ButtonGroup();
        gruppoRuoli.add(ruoloUtente);
        gruppoRuoli.add(ruoloFattorino);
        gruppoRuoli.add(ruoloAdmin);
        panelRuoli.add(new JLabel("Accesso come:"));
        panelRuoli.add(ruoloUtente);
        panelRuoli.add(ruoloFattorino);
        panelRuoli.add(ruoloAdmin);
        gbc.gridy = 4;
        panel.add(panelRuoli, gbc);

        // Link registrazione
        var btnRegistrati = new JButton("Non hai un account? Registrati");
        btnRegistrati.setBorderPainted(false);
        btnRegistrati.setContentAreaFilled(false);
        btnRegistrati.setForeground(COLORE_ACCENTO);
        gbc.gridy = 5;
        panel.add(btnRegistrati, gbc);

        // Azioni
        btnLogin.addActionListener(e -> {
            var ruolo = "UTENTE";
            if (ruoloFattorino.isSelected()) ruolo = "FATTORINO";
            if (ruoloAdmin.isSelected()) ruolo = "ADMIN";
            controller.login(
                campoEmail.getText(),
                new String(campoPassword.getPassword()),
                ruolo
            );
        });
        btnRegistrati.addActionListener(e -> cardLayout.show(cards, "REGISTRAZIONE"));

        return panel;
    }

    private JPanel creaSchermatRegistrazione() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLORE_SECONDARIO);
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        var titolo = new JLabel("Registrazione", SwingConstants.CENTER);
        titolo.setFont(new Font("Serif", Font.BOLD, 22));
        titolo.setForeground(COLORE_PRIMARIO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titolo, gbc);

        gbc.gridwidth = 1;
        String[] etichette = {"Nome:", "Cognome:", "Email:", "Password:"};
        JTextField[] campi = {
            new JTextField(20), new JTextField(20),
            new JTextField(20), new JPasswordField(20)
        };

        for (int i = 0; i < etichette.length; i++) {
            gbc.gridx = 0; gbc.gridy = i + 1; panel.add(new JLabel(etichette[i]), gbc);
            gbc.gridx = 1; panel.add(campi[i], gbc);
        }

        var btnRegistra = creaBottone("Registrati");
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(btnRegistra, gbc);

        var btnTornaLogin = new JButton("← Torna al login");
        btnTornaLogin.setBorderPainted(false);
        btnTornaLogin.setContentAreaFilled(false);
        btnTornaLogin.setForeground(COLORE_ACCENTO);
        gbc.gridy = 6;
        panel.add(btnTornaLogin, gbc);

        btnRegistra.addActionListener(e ->
            controller.registra(
                campi[0].getText(), campi[1].getText(),
                campi[2].getText(),
                new String(((JPasswordField) campi[3]).getPassword())
            )
        );
        btnTornaLogin.addActionListener(e -> cardLayout.show(cards, "LOGIN"));

        return panel;
    }

    private JPanel creaSchermatHome() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLORE_SECONDARIO);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- HEADER ---
        var header = new JPanel(new BorderLayout());
        header.setBackground(COLORE_PRIMARIO);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        labelBenvenuto = new JLabel("Ciao!");
        labelBenvenuto.setFont(new Font("Serif", Font.BOLD, 18));
        labelBenvenuto.setForeground(Color.WHITE);
        header.add(labelBenvenuto, BorderLayout.WEST);

        var headerDestra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerDestra.setOpaque(false);
        labelCarrello = new JLabel("🛒 Carrello: 0 prodotti");
        labelCarrello.setForeground(Color.WHITE);
        var btnLogout = new JButton("Esci");
        btnLogout.setForeground(COLORE_PRIMARIO);
        headerDestra.add(labelCarrello);
        headerDestra.add(btnLogout);
        header.add(headerDestra, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // --- CENTRO: menù a sinistra, storico a destra ---
        var centro = new JPanel(new GridLayout(1, 3, 10, 0));
        centro.setOpaque(false);

        // Lista prodotti
        modelProdotti = new DefaultListModel<>();
        listaProdotti  = new JList<>(modelProdotti);
        listaProdotti.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProdotti.setCellRenderer(new ProdottoCellRenderer());
        var panelProdotti = new JPanel(new BorderLayout(5, 5));
        panelProdotti.setOpaque(false);
        var headerProdotti = new JPanel(new BorderLayout());
        headerProdotti.setOpaque(false);
        headerProdotti.add(new JLabel("Menu"), BorderLayout.WEST);
        var filtriProdotti = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filtriProdotti.setOpaque(false);
        var filtroCibo = new JRadioButton("Cibo");
        var filtroBevande = new JRadioButton("Bevande");
        filtroCibo.setSelected(true);
        for (var radio : List.of(filtroCibo, filtroBevande)) {
            radio.setOpaque(false);
            radio.setForeground(COLORE_PRIMARIO);
        }
        var gruppoFiltri = new ButtonGroup();
        gruppoFiltri.add(filtroCibo);
        gruppoFiltri.add(filtroBevande);
        filtriProdotti.add(filtroCibo);
        filtriProdotti.add(filtroBevande);
        headerProdotti.add(filtriProdotti, BorderLayout.EAST);
        panelProdotti.add(headerProdotti, BorderLayout.NORTH);
        panelProdotti.add(new JScrollPane(listaProdotti), BorderLayout.CENTER);

        // Bottoni ordine
        var panelBottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panelBottoni.setOpaque(false);
        var btnAggiungi  = creaBottone("+ Aggiungi al carrello");
        var btnDelivery  = creaBottone("🛵 Delivery");
        var btnAsporto   = creaBottone("🥡 Asporto");
        var btnSvuota    = new JButton("Svuota carrello");
        panelBottoni.add(btnAggiungi);
        panelBottoni.add(btnDelivery);
        panelBottoni.add(btnAsporto);
        panelBottoni.add(btnSvuota);
        panelProdotti.add(panelBottoni, BorderLayout.SOUTH);
        centro.add(panelProdotti);

        modelCarrello = new DefaultListModel<>();
        listaCarrello = new JList<>(modelCarrello);
        var panelCarrello = new JPanel(new BorderLayout(5, 5));
        panelCarrello.setOpaque(false);
        panelCarrello.add(new JLabel("Il tuo carrello"), BorderLayout.NORTH);
        panelCarrello.add(new JScrollPane(listaCarrello), BorderLayout.CENTER);
        labelTotaleCarrello = new JLabel("Totale: 0.00 euro");
        labelTotaleCarrello.setFont(labelTotaleCarrello.getFont().deriveFont(Font.BOLD));
        var panelAzioniCarrello = new JPanel(new BorderLayout(5, 5));
        panelAzioniCarrello.setOpaque(false);
        panelAzioniCarrello.add(labelTotaleCarrello, BorderLayout.NORTH);
        var bottoniCarrello = new JPanel(new GridLayout(2, 2, 5, 5));
        bottoniCarrello.setOpaque(false);
        var btnPrenotaTavolo = creaBottone("Prenota tavolo");
        var btnFeedback = creaBottone("Feedback");
        bottoniCarrello.add(btnDelivery);
        bottoniCarrello.add(btnAsporto);
        bottoniCarrello.add(btnPrenotaTavolo);
        bottoniCarrello.add(btnFeedback);
        panelAzioniCarrello.add(bottoniCarrello, BorderLayout.CENTER);
        var gestioneCarrello = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        gestioneCarrello.setOpaque(false);
        var btnRimuoviCarrello = new JButton("Elimina selezionato");
        gestioneCarrello.add(btnSvuota);
        gestioneCarrello.add(btnRimuoviCarrello);
        panelAzioniCarrello.add(gestioneCarrello, BorderLayout.SOUTH);
        panelCarrello.add(panelAzioniCarrello, BorderLayout.SOUTH);
        centro.add(panelCarrello);

        // Storico ordini
        modelStorico = new DefaultListModel<>();
        listaStorico  = new JList<>(modelStorico);
        var panelStorico = new JPanel(new BorderLayout(5, 5));
        panelStorico.setOpaque(false);

        var headerStorico = new JPanel(new BorderLayout());
        headerStorico.setOpaque(false);
        headerStorico.add(new JLabel("📦 I tuoi ordini"), BorderLayout.WEST);
        var btnStatistiche = creaBottone("📊 Statistiche");
        headerStorico.add(btnStatistiche, BorderLayout.EAST);
        panelStorico.add(headerStorico, BorderLayout.NORTH);
        panelStorico.add(new JScrollPane(listaStorico), BorderLayout.CENTER);
        centro.add(panelStorico);

        panel.add(centro, BorderLayout.CENTER);

        // --- AZIONI ---
        btnLogout.addActionListener(e -> controller.logout());

        filtroCibo.addActionListener(e -> {
            filtroProdotti = "CIBO";
            filtraProdotti();
        });
        filtroBevande.addActionListener(e -> {
            filtroProdotti = "BEVANDE";
            filtraProdotti();
        });

        btnAggiungi.addActionListener(e -> {
            var sel = listaProdotti.getSelectedValue();
            if (sel == null) { mostraErrore("Seleziona un prodotto dal menù."); return; }
            controller.aggiungiAlCarrello(sel);
        });

        btnDelivery.addActionListener(e -> {
            String indirizzo = JOptionPane.showInputDialog(frame,
                "Inserisci l'indirizzo di consegna:", "Delivery", JOptionPane.QUESTION_MESSAGE);
            if (indirizzo != null && !indirizzo.isBlank())
                controller.confermaOrdineDelivery(indirizzo);
        });

        btnAsporto.addActionListener(e -> {
            int risposta = JOptionPane.showConfirmDialog(frame,
                "Confermi l'ordine da asporto?", "Asporto", JOptionPane.YES_NO_OPTION);
            if (risposta == JOptionPane.YES_OPTION)
                controller.confermaOrdineAsporto();
        });

        btnPrenotaTavolo.addActionListener(e -> mostraDialogPrenotazioneTavolo());

        btnFeedback.addActionListener(e -> mostraDialogFeedback());

        btnSvuota.addActionListener(e -> controller.svuotaCarrello());
        btnRimuoviCarrello.addActionListener(e -> {
            var selezionata = listaCarrello.getSelectedValue();
            if (selezionata == null) {
                mostraErrore("Seleziona un prodotto dal carrello.");
                return;
            }
            controller.rimuoviDalCarrello(selezionata.idProdotto);
        });

        btnStatistiche.addActionListener(e -> controller.caricaStatistiche());

        return panel;
    }

    private JPanel creaSchermatStatistiche() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLORE_SECONDARIO);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        var titolo = new JLabel("📊 Statistiche di vendita", SwingConstants.CENTER);
        titolo.setFont(new Font("Serif", Font.BOLD, 20));
        titolo.setForeground(COLORE_PRIMARIO);
        panel.add(titolo, BorderLayout.NORTH);

        modelStats  = new DefaultListModel<>();
        listaStats  = new JList<>(modelStats);
        listaStats.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(listaStats), BorderLayout.CENTER);

        var btnTorna = creaBottone("← Torna alla home");
        panel.add(btnTorna, BorderLayout.SOUTH);
        btnTorna.addActionListener(e -> controller.tornaDaStatistiche());

        return panel;
    }

    private JPanel creaSchermatFattorino() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLORE_SECONDARIO);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        var header = new JPanel(new BorderLayout());
        header.setBackground(COLORE_PRIMARIO);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        labelFattorino = new JLabel("Area fattorino");
        labelFattorino.setFont(new Font("Serif", Font.BOLD, 18));
        labelFattorino.setForeground(Color.WHITE);
        header.add(labelFattorino, BorderLayout.WEST);

        var btnLogout = new JButton("Esci");
        btnLogout.setForeground(COLORE_PRIMARIO);
        header.add(btnLogout, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modelDelivery = new DefaultListModel<>();
        listaDelivery = new JList<>(modelDelivery);

        var centro = new JPanel(new BorderLayout(5, 5));
        centro.setOpaque(false);
        centro.add(new JLabel("Ordini delivery"), BorderLayout.NORTH);
        centro.add(new JScrollPane(listaDelivery), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        var azioni = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        azioni.setOpaque(false);
        var btnPresaInCarico = creaBottone("Presa in carico");
        var btnRitirato = creaBottone("Ritirato");
        var btnInConsegna = creaBottone("In consegna");
        var btnConsegnato = creaBottone("Consegnato");
        var btnDettagliOrdine = creaBottone("Dettagli ordine");
        var btnAggiorna = creaBottone("Aggiorna");
        azioni.add(btnDettagliOrdine);
        azioni.add(btnPresaInCarico);
        azioni.add(btnRitirato);
        azioni.add(btnInConsegna);
        azioni.add(btnConsegnato);
        azioni.add(btnAggiorna);
        panel.add(azioni, BorderLayout.SOUTH);

        btnLogout.addActionListener(e -> controller.logout());
        btnDettagliOrdine.addActionListener(e ->
            controller.mostraDettagliOrdineDelivery(listaDelivery.getSelectedValue()));
        btnPresaInCarico.addActionListener(e -> controller.aggiornaStatoOrdineDelivery(
            listaDelivery.getSelectedValue(), "presa in carico"));
        btnRitirato.addActionListener(e -> controller.aggiornaStatoOrdineDelivery(
            listaDelivery.getSelectedValue(), "ritirato"));
        btnInConsegna.addActionListener(e -> controller.aggiornaStatoOrdineDelivery(
            listaDelivery.getSelectedValue(), "in consegna"));
        btnConsegnato.addActionListener(e -> controller.aggiornaStatoOrdineDelivery(
            listaDelivery.getSelectedValue(), "consegnato"));
        btnAggiorna.addActionListener(e -> controller.caricaOrdiniDelivery());

        return panel;
    }

    private JPanel creaSchermatAdmin() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLORE_SECONDARIO);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        var header = new JPanel(new BorderLayout());
        header.setBackground(COLORE_PRIMARIO);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        var titolo = new JLabel("Area admin");
        titolo.setFont(new Font("Serif", Font.BOLD, 18));
        titolo.setForeground(Color.WHITE);
        header.add(titolo, BorderLayout.WEST);

        var btnLogout = new JButton("Esci");
        btnLogout.setForeground(COLORE_PRIMARIO);
        header.add(btnLogout, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        var corpo = new JPanel(new BorderLayout(10, 10));
        corpo.setOpaque(false);

        var menuAdmin = new JPanel(new GridLayout(1, 4, 10, 0));
        menuAdmin.setOpaque(false);
        var btnGestioneFattorini = creaBottone("Gestione fattorini");
        var btnStatisticheAdmin = creaBottone("Statistiche");
        var btnGestioneTavoli = creaBottone("Gestione tavoli");
        var btnModificaMenu = creaBottone("Modifica menu");
        menuAdmin.add(btnGestioneFattorini);
        menuAdmin.add(btnStatisticheAdmin);
        menuAdmin.add(btnGestioneTavoli);
        menuAdmin.add(btnModificaMenu);
        corpo.add(menuAdmin, BorderLayout.NORTH);

        var adminCards = new JPanel(new CardLayout());
        adminCards.setOpaque(false);
        var adminCardLayout = (CardLayout) adminCards.getLayout();

        modelAdminStats = new DefaultListModel<>();
        listaAdminStats = new JList<>(modelAdminStats);
        listaAdminStats.setFont(new Font("Monospaced", Font.PLAIN, 13));
        var panelStats = new JPanel(new BorderLayout(5, 5));
        panelStats.setOpaque(false);
        panelStats.add(new JLabel("Statistiche di vendita"), BorderLayout.NORTH);
        panelStats.add(new JScrollPane(listaAdminStats), BorderLayout.CENTER);
        var footerStats = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerStats.setOpaque(false);
        var btnAggiorna = creaBottone("Aggiorna statistiche");
        footerStats.add(btnAggiorna);
        panelStats.add(footerStats, BorderLayout.SOUTH);

        var panelFattorini = new JPanel(new GridLayout(1, 2, 10, 0));
        panelFattorini.setOpaque(false);

        var panelNuovoFattorino = new JPanel(new GridBagLayout());
        panelNuovoFattorino.setOpaque(false);
        panelNuovoFattorino.setBorder(BorderFactory.createTitledBorder("Nuovo fattorino"));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        campoNomeFattorino = new JTextField(24);
        campoCognomeFattorino = new JTextField(24);
        campoEmailFattorino = new JTextField(24);
        campoPasswordFattorino = new JPasswordField(24);

        aggiungiCampo(panelNuovoFattorino, gbc, 0, "Nome:", campoNomeFattorino);
        aggiungiCampo(panelNuovoFattorino, gbc, 1, "Cognome:", campoCognomeFattorino);
        aggiungiCampo(panelNuovoFattorino, gbc, 2, "Email:", campoEmailFattorino);
        aggiungiCampo(panelNuovoFattorino, gbc, 3, "Password:", campoPasswordFattorino);

        var btnCreaFattorino = creaBottone("Crea fattorino");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panelNuovoFattorino.add(btnCreaFattorino, gbc);
        panelFattorini.add(panelNuovoFattorino);

        var panelListaFattorini = new JPanel(new BorderLayout(5, 5));
        panelListaFattorini.setOpaque(false);
        panelListaFattorini.setBorder(BorderFactory.createTitledBorder("Lista fattorini"));
        modelFattorini = new DefaultListModel<>();
        listaFattorini = new JList<>(modelFattorini);
        panelListaFattorini.add(new JScrollPane(listaFattorini), BorderLayout.CENTER);
        var azioniFattorini = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        azioniFattorini.setOpaque(false);
        var btnListaFattorini = creaBottone("Lista fattorini");
        var btnEliminaFattorino = new JButton("Elimina");
        azioniFattorini.add(btnListaFattorini);
        azioniFattorini.add(btnEliminaFattorino);
        panelListaFattorini.add(azioniFattorini, BorderLayout.SOUTH);
        panelFattorini.add(panelListaFattorini);

        var panelTavoli = new JPanel(new BorderLayout(5, 5));
        panelTavoli.setOpaque(false);
        panelTavoli.setBorder(BorderFactory.createTitledBorder("Prenotazioni tavoli"));
        modelPrenotazioniTavoli = new DefaultListModel<>();
        listaPrenotazioniTavoli = new JList<>(modelPrenotazioniTavoli);
        panelTavoli.add(new JScrollPane(listaPrenotazioniTavoli), BorderLayout.CENTER);
        var azioniTavoli = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        azioniTavoli.setOpaque(false);
        var btnAggiornaTavoli = creaBottone("Aggiorna");
        var btnConfermaTavolo = creaBottone("Conferma");
        var btnRifiutaTavolo = new JButton("Rifiuta");
        azioniTavoli.add(btnAggiornaTavoli);
        azioniTavoli.add(btnConfermaTavolo);
        azioniTavoli.add(btnRifiutaTavolo);
        panelTavoli.add(azioniTavoli, BorderLayout.SOUTH);

        var panelProdotto = new JPanel(new GridBagLayout());
        panelProdotto.setOpaque(false);
        panelProdotto.setBorder(BorderFactory.createTitledBorder("Nuovo prodotto"));
        var gbcProdotto = new GridBagConstraints();
        gbcProdotto.insets = new Insets(8, 8, 8, 8);
        gbcProdotto.fill = GridBagConstraints.HORIZONTAL;

        campoNomeProdotto = new JTextField(30);
        campoDescrizioneProdotto = new JTextField(30);
        campoPrezzoProdotto = new JTextField(30);
        comboCategoriaProdotto = new JComboBox<>(new String[] {"Cibo", "Bevande"});

        aggiungiCampo(panelProdotto, gbcProdotto, 0, "Nome:", campoNomeProdotto);
        aggiungiCampo(panelProdotto, gbcProdotto, 1, "Descrizione:", campoDescrizioneProdotto);
        aggiungiCampo(panelProdotto, gbcProdotto, 2, "Prezzo:", campoPrezzoProdotto);
        aggiungiCampo(panelProdotto, gbcProdotto, 3, "Categoria:", comboCategoriaProdotto);

        var btnCreaProdotto = creaBottone("Crea prodotto");
        gbcProdotto.gridx = 0; gbcProdotto.gridy = 4; gbcProdotto.gridwidth = 2;
        panelProdotto.add(btnCreaProdotto, gbcProdotto);

        adminCards.add(panelFattorini, "FATTORINI");
        adminCards.add(panelStats, "STATISTICHE");
        adminCards.add(panelTavoli, "TAVOLI");
        adminCards.add(panelProdotto, "MENU");
        corpo.add(adminCards, BorderLayout.CENTER);
        panel.add(corpo, BorderLayout.CENTER);

        btnLogout.addActionListener(e -> controller.logout());
        btnGestioneFattorini.addActionListener(e -> {
            adminCardLayout.show(adminCards, "FATTORINI");
            controller.caricaFattorini();
        });
        btnStatisticheAdmin.addActionListener(e -> {
            adminCardLayout.show(adminCards, "STATISTICHE");
            controller.caricaAdmin();
        });
        btnGestioneTavoli.addActionListener(e -> {
            adminCardLayout.show(adminCards, "TAVOLI");
            controller.caricaPrenotazioniTavoli();
        });
        btnModificaMenu.addActionListener(e -> adminCardLayout.show(adminCards, "MENU"));
        btnAggiorna.addActionListener(e -> controller.caricaAdmin());
        btnCreaFattorino.addActionListener(e -> controller.registraFattorino(
            campoNomeFattorino.getText(),
            campoCognomeFattorino.getText(),
            campoEmailFattorino.getText(),
            new String(campoPasswordFattorino.getPassword())
        ));
        btnListaFattorini.addActionListener(e -> controller.caricaFattorini());
        btnEliminaFattorino.addActionListener(e ->
            controller.eliminaFattorino(listaFattorini.getSelectedValue()));
        btnAggiornaTavoli.addActionListener(e -> controller.caricaPrenotazioniTavoli());
        btnConfermaTavolo.addActionListener(e ->
            controller.confermaPrenotazioneTavolo(listaPrenotazioniTavoli.getSelectedValue()));
        btnRifiutaTavolo.addActionListener(e ->
            controller.rifiutaPrenotazioneTavolo(listaPrenotazioniTavoli.getSelectedValue()));
        btnCreaProdotto.addActionListener(e -> controller.registraProdotto(
            campoNomeProdotto.getText(),
            campoDescrizioneProdotto.getText(),
            campoPrezzoProdotto.getText(),
            (String) comboCategoriaProdotto.getSelectedItem()
        ));

        adminCardLayout.show(adminCards, "FATTORINI");
        return panel;
    }

    private void aggiungiCampo(JPanel panel, GridBagConstraints gbc,
                               int riga, String etichetta, JComponent campo) {
        gbc.gridx = 0; gbc.gridy = riga; gbc.gridwidth = 1;
        panel.add(new JLabel(etichetta), gbc);
        gbc.gridx = 1;
        panel.add(campo, gbc);
    }

    private void mostraDialogPrenotazioneTavolo() {
        var panel = new JPanel(new GridLayout(3, 2, 8, 8));
        var campoGiorno = new JTextField("2026-05-20");
        var campoOra = new JTextField("20:30");
        var campoPersone = new JTextField("2");
        panel.add(new JLabel("Data (AAAA-MM-GG):"));
        panel.add(campoGiorno);
        panel.add(new JLabel("Ora (HH:MM):"));
        panel.add(campoOra);
        panel.add(new JLabel("Persone:"));
        panel.add(campoPersone);

        int risposta = JOptionPane.showConfirmDialog(frame, panel,
            "Prenota tavolo", JOptionPane.OK_CANCEL_OPTION);
        if (risposta == JOptionPane.OK_OPTION) {
            controller.prenotaTavolo(
                campoGiorno.getText(),
                campoOra.getText(),
                campoPersone.getText()
            );
        }
    }

    private void mostraDialogFeedback() {
        var servizio = listaStorico.getSelectedValue();
        if (servizio == null) {
            mostraErrore("Seleziona un ordine dallo storico.");
            return;
        }

        var panel = new JPanel(new BorderLayout(8, 8));
        var voto = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        var rigaVoto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rigaVoto.add(new JLabel("Voto:"));
        rigaVoto.add(voto);
        var commento = new JTextArea(4, 25);
        panel.add(rigaVoto, BorderLayout.NORTH);
        panel.add(new JScrollPane(commento), BorderLayout.CENTER);

        int risposta = JOptionPane.showConfirmDialog(frame, panel,
            "Feedback ordine #" + servizio.id, JOptionPane.OK_CANCEL_OPTION);
        if (risposta == JOptionPane.OK_OPTION) {
            controller.lasciaFeedback(
                servizio,
                (Integer) voto.getValue(),
                commento.getText()
            );
        }
    }

    private void filtraProdotti() {
        modelProdotti.clear();
        prodottiDisponibili.stream()
            .filter(prodotto -> "BEVANDE".equals(filtroProdotti)
                ? isBevanda(prodotto)
                : !isBevanda(prodotto))
            .forEach(modelProdotti::addElement);
    }

    private boolean isBevanda(Prodotto prodotto) {
        var categoria = prodotto.nomeCategoria.toLowerCase();
        var nome = prodotto.nome.toLowerCase();
        return categoria.contains("bev")
            || categoria.contains("drink")
            || nome.contains("acqua")
            || nome.contains("coca")
            || nome.contains("fanta")
            || nome.contains("sprite")
            || nome.contains("birra")
            || nome.contains("te ");
    }

    private record RigaCarrello(int idProdotto, String testo) {
        @Override
        public String toString() {
            return testo;
        }
    }

    private final class ProdottoCellRenderer extends JPanel
            implements ListCellRenderer<Prodotto> {

        private final JLabel titolo = new JLabel();
        private final JLabel descrizione = new JLabel();

        private ProdottoCellRenderer() {
            setLayout(new BorderLayout(4, 2));
            setBorder(new EmptyBorder(7, 8, 7, 8));
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 13f));
            descrizione.setFont(descrizione.getFont().deriveFont(Font.PLAIN, 12f));
            add(titolo, BorderLayout.NORTH);
            add(descrizione, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends Prodotto> list,
                Prodotto prodotto,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            titolo.setText(String.format("%s - %.2f euro  [%s]",
                prodotto.nome, prodotto.prezzo, prodotto.nomeCategoria));
            descrizione.setText(prodotto.descrizione);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                titolo.setForeground(list.getSelectionForeground());
                descrizione.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                titolo.setForeground(list.getForeground());
                descrizione.setForeground(Color.DARK_GRAY);
            }
            setOpaque(true);
            return this;
        }
    }

    // Helper per creare bottoni stilizzati
    private JButton creaBottone(String testo) {
        var btn = new JButton(testo);
        btn.setBackground(COLORE_PRIMARIO);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return btn;
    }
}
