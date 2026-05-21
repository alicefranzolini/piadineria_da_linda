package com.piadineria.view;

import com.piadineria.controller.Controller;
import com.piadineria.data.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final String LOGO_PATH =
        "C:\\Users\\Sabrina\\Downloads\\MDP\\logo piadineria.png";

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
    private GraficoVenditePanel graficoVendite;
    private JList<DettaglioFeedback> listaFeedback;
    private DefaultListModel<DettaglioFeedback> modelFeedback;
    private JLabel labelMediaFeedback;
    private GraficoFeedbackPanel graficoFeedback;
    private JList<Magazzino> listaMagazzino;
    private DefaultListModel<Magazzino> modelMagazzino;
    private JList<Servizio> listaTavoliAttesa;
    private JList<Servizio> listaTavoliConfermate;
    private JList<Servizio> listaTavoliRifiutate;
    private DefaultListModel<Servizio> modelTavoliAttesa;
    private DefaultListModel<Servizio> modelTavoliConfermate;
    private DefaultListModel<Servizio> modelTavoliRifiutate;
    private JList<Servizio> listaDelivery;
    private DefaultListModel<Servizio> modelDelivery;
    private JList<Servizio> listaDeliveryConsegnati;
    private DefaultListModel<Servizio> modelDeliveryConsegnati;
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
    private JList<Prodotto> listaProdottiAdmin;
    private DefaultListModel<Prodotto> modelProdottiAdmin;
    private JList<Magazzino> listaIngredientiProdotto;
    private DefaultListModel<Magazzino> modelIngredientiProdotto;

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

    public void mostraStoricoOrdiniPopup() {
        var panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(listaStorico), BorderLayout.CENTER);
        var btnFeedback = creaBottone("Lascia feedback");
        panel.add(btnFeedback, BorderLayout.SOUTH);
        var dialog = new JDialog(frame, "Storico ordini e feedback", true);
        btnFeedback.addActionListener(e -> mostraDialogFeedback());
        dialog.add(panel);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
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
        if (graficoVendite != null) graficoVendite.setStatistiche(stats);
        cardLayout.show(cards, "ADMIN");
    }

    public void mostraOrdiniDelivery(List<Servizio> ordini) {
        modelDelivery.clear();
        ordini.forEach(modelDelivery::addElement);
    }

    public void mostraOrdiniDeliveryConsegnati(List<Servizio> ordini) {
        modelDeliveryConsegnati.clear();
        ordini.forEach(modelDeliveryConsegnati::addElement);
    }

    public void mostraDettaglioOrdine(DettaglioOrdine dettaglio) {
        var area = new JTextArea(dettaglio.testo(), 10, 35);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(frame, new JScrollPane(area),
            "Dettagli ordine", JOptionPane.INFORMATION_MESSAGE);
    }

    public void mostraTesseraFedelta(TesseraFedelta tessera) {
        var area = new JTextArea(tessera.testo(), 9, 38);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(frame, new JScrollPane(area),
            "Tessera fedelta", JOptionPane.INFORMATION_MESSAGE);
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
        if (listaIngredientiProdotto != null) listaIngredientiProdotto.clearSelection();
    }

    public void mostraProdottiMenuAdmin(List<Prodotto> prodotti) {
        modelProdottiAdmin.clear();
        prodotti.forEach(modelProdottiAdmin::addElement);
    }

    public void mostraIngredientiSelezionabili(List<Magazzino> ingredienti) {
        modelIngredientiProdotto.clear();
        ingredienti.forEach(modelIngredientiProdotto::addElement);
    }

    public void mostraFattorini(List<Fattorino> fattorini) {
        modelFattorini.clear();
        fattorini.forEach(modelFattorini::addElement);
    }

    public void mostraPrenotazioniTavoli(List<Servizio> prenotazioni) {
        modelTavoliAttesa.clear();
        modelTavoliConfermate.clear();
        modelTavoliRifiutate.clear();
        for (var p : prenotazioni) {
            var stato = p.statoNome.toLowerCase();
            if (stato.contains("confermata")) modelTavoliConfermate.addElement(p);
            else if (stato.contains("rifiutata")) modelTavoliRifiutate.addElement(p);
            else modelTavoliAttesa.addElement(p);
        }
    }

    public void mostraFeedback(List<DettaglioFeedback> feedback) {
        modelFeedback.clear();
        feedback.forEach(modelFeedback::addElement);
        double media = feedback.stream().mapToInt(f -> f.voto).average().orElse(0);
        long prodotto = feedback.stream().filter(f -> f.categoria.equalsIgnoreCase("prodotto")).count();
        long fattorino = feedback.stream().filter(f -> f.categoria.equalsIgnoreCase("fattorino")).count();
        long tavolo = feedback.stream().filter(f -> f.categoria.toLowerCase().contains("tavolo")).count();
        if (graficoFeedback != null) graficoFeedback.setFeedback(feedback);
        labelMediaFeedback.setText(String.format(
            "Gradimento medio: %.0f%% | prodotto: %d | fattorino: %d | tavolo: %d",
            media / 5.0 * 100.0, prodotto, fattorino, tavolo));
    }

    public void mostraMagazzino(List<Magazzino> righe) {
        modelMagazzino.clear();
        righe.forEach(modelMagazzino::addElement);
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

        var logo = creaLogoLabel(190, 160);
        if (logo != null) {
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            panel.add(logo, gbc);
        }

        // Titolo
        var titolo = new JLabel("🫓 Piadineria Da Linda", SwingConstants.CENTER);
        titolo.setFont(new Font("Serif", Font.BOLD, 28));
        titolo.setForeground(COLORE_PRIMARIO);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(titolo, gbc);

        gbc.gridwidth = 1;

        // Email
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Email:"), gbc);
        var campoEmail = new JTextField(20);
        gbc.gridx = 1; panel.add(campoEmail, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Password:"), gbc);
        var campoPassword = new JPasswordField(20);
        gbc.gridx = 1; panel.add(campoPassword, gbc);

        // Bottone login
        var btnLogin = creaBottone("Accedi");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
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
        gbc.gridy = 5;
        panel.add(panelRuoli, gbc);

        // Link registrazione
        var btnRegistrati = new JButton("Non hai un account? Registrati");
        btnRegistrati.setBorderPainted(false);
        btnRegistrati.setContentAreaFilled(false);
        btnRegistrati.setForeground(COLORE_ACCENTO);
        gbc.gridy = 6;
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
        var headerSinistra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerSinistra.setOpaque(false);
        var logoHeader = creaLogoLabel(54, 44);
        if (logoHeader != null) headerSinistra.add(logoHeader);
        headerSinistra.add(labelBenvenuto);
        header.add(headerSinistra, BorderLayout.WEST);

        var headerDestra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerDestra.setOpaque(false);
        labelCarrello = new JLabel("🛒 Carrello: 0 prodotti");
        labelCarrello.setForeground(Color.WHITE);
        var btnTessera = new JButton("Tessera fedelta");
        btnTessera.setForeground(COLORE_PRIMARIO);
        var btnLogout = new JButton("Esci");
        btnLogout.setForeground(COLORE_PRIMARIO);
        headerDestra.add(labelCarrello);
        headerDestra.add(btnTessera);
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
        var btnAggiungi  = creaBottone("+ Aggiungi al carrello");
        var btnModificaPiadina = creaBottone("Modifica piadina");
        var btnDelivery  = creaBottone("🛵 Delivery");
        var btnAsporto   = creaBottone("🥡 Asporto");
        var btnSvuota    = new JButton("Svuota carrello");
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
        var gestioneCarrello = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        gestioneCarrello.setOpaque(false);
        var btnRimuoviCarrello = new JButton("Elimina selezionato");
        gestioneCarrello.add(btnSvuota);
        gestioneCarrello.add(btnRimuoviCarrello);
        panelAzioniCarrello.add(gestioneCarrello, BorderLayout.SOUTH);
        panelCarrello.add(panelAzioniCarrello, BorderLayout.SOUTH);
        centro.add(panelCarrello);

        // Storico e feedback
        modelStorico = new DefaultListModel<>();
        listaStorico  = new JList<>(modelStorico);
        var panelStorico = new JPanel(new BorderLayout(5, 5));
        panelStorico.setOpaque(false);

        var headerStorico = new JPanel(new BorderLayout());
        headerStorico.setOpaque(false);
        headerStorico.add(new JLabel("📦 I tuoi ordini"), BorderLayout.WEST);
        panelStorico.add(headerStorico, BorderLayout.NORTH);
        var panelAzioniUtente = new JPanel(new GridLayout(0, 1, 6, 6));
        panelAzioniUtente.setOpaque(false);
        var btnPrenotaTavolo = creaBottone("Prenota tavolo");
        var btnStoricoOrdini = creaBottone("Storico e feedback");
        panelAzioniUtente.add(btnAggiungi);
        panelAzioniUtente.add(btnModificaPiadina);
        panelAzioniUtente.add(btnDelivery);
        panelAzioniUtente.add(btnAsporto);
        panelAzioniUtente.add(btnPrenotaTavolo);
        panelAzioniUtente.add(btnStoricoOrdini);
        panelStorico.add(panelAzioniUtente, BorderLayout.CENTER);
        centro.add(panelStorico);

        panel.add(centro, BorderLayout.CENTER);

        // --- AZIONI ---
        btnLogout.addActionListener(e -> controller.logout());
        btnTessera.addActionListener(e -> controller.mostraTesseraFedelta());

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
            if ("Piadina componibile".equalsIgnoreCase(sel.nome)) {
                mostraDialogPiadinaComponibile(null);
                return;
            }
            controller.aggiungiAlCarrello(sel);
        });

        btnModificaPiadina.addActionListener(e -> {
            var sel = listaProdotti.getSelectedValue();
            if (sel == null || !sel.nome.toLowerCase().contains("piadina")) {
                mostraErrore("Seleziona una piadina da modificare.");
                return;
            }
            mostraDialogPiadinaComponibile(sel);
        });

        btnDelivery.addActionListener(e -> mostraDialogDelivery());

        btnAsporto.addActionListener(e -> {
            int risposta = JOptionPane.showConfirmDialog(frame,
                "Confermi l'ordine da asporto?", "Asporto", JOptionPane.YES_NO_OPTION);
            if (risposta == JOptionPane.YES_OPTION)
                controller.confermaOrdineAsporto();
        });

        btnPrenotaTavolo.addActionListener(e -> mostraDialogPrenotazioneTavolo());

        btnSvuota.addActionListener(e -> controller.svuotaCarrello());
        btnRimuoviCarrello.addActionListener(e -> {
            var selezionata = listaCarrello.getSelectedValue();
            if (selezionata == null) {
                mostraErrore("Seleziona un prodotto dal carrello.");
                return;
            }
            controller.rimuoviDalCarrello(selezionata.idProdotto);
        });

        btnStoricoOrdini.addActionListener(e -> controller.mostraStoricoOrdini());

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

        var centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.setOpaque(false);
        var panelAttivi = new JPanel(new BorderLayout(5, 5));
        panelAttivi.setOpaque(false);
        panelAttivi.add(new JLabel("Ordini delivery"), BorderLayout.NORTH);
        panelAttivi.add(new JScrollPane(listaDelivery), BorderLayout.CENTER);
        centro.add(panelAttivi);
        modelDeliveryConsegnati = new DefaultListModel<>();
        listaDeliveryConsegnati = new JList<>(modelDeliveryConsegnati);
        var panelConsegnati = new JPanel(new BorderLayout(5, 5));
        panelConsegnati.setOpaque(false);
        panelConsegnati.add(new JLabel("Storico consegnati"), BorderLayout.NORTH);
        panelConsegnati.add(new JScrollPane(listaDeliveryConsegnati), BorderLayout.CENTER);
        centro.add(panelConsegnati);
        panel.add(centro, BorderLayout.CENTER);

        var azioni = new JPanel(new GridLayout(2, 3, 8, 6));
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

        var menuAdmin = new JPanel(new GridLayout(1, 6, 10, 0));
        menuAdmin.setOpaque(false);
        var btnGestioneFattorini = creaBottone("Gestione fattorini");
        var btnStatisticheAdmin = creaBottone("Statistiche");
        var btnGestioneTavoli = creaBottone("Gestione tavoli");
        var btnFeedbackAdmin = creaBottone("Feedback");
        var btnMagazzino = creaBottone("Magazzino");
        var btnModificaMenu = creaBottone("Modifica menu");
        menuAdmin.add(btnGestioneFattorini);
        menuAdmin.add(btnStatisticheAdmin);
        menuAdmin.add(btnGestioneTavoli);
        menuAdmin.add(btnFeedbackAdmin);
        menuAdmin.add(btnMagazzino);
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
        graficoVendite = new GraficoVenditePanel();
        var splitStats = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(listaAdminStats), graficoVendite);
        splitStats.setResizeWeight(0.45);
        panelStats.add(splitStats, BorderLayout.CENTER);
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
        modelTavoliAttesa = new DefaultListModel<>();
        modelTavoliConfermate = new DefaultListModel<>();
        modelTavoliRifiutate = new DefaultListModel<>();
        listaTavoliAttesa = new JList<>(modelTavoliAttesa);
        listaTavoliConfermate = new JList<>(modelTavoliConfermate);
        listaTavoliRifiutate = new JList<>(modelTavoliRifiutate);
        var grigliaTavoli = new JPanel(new GridLayout(1, 3, 8, 0));
        grigliaTavoli.setOpaque(false);
        grigliaTavoli.add(panelLista("In attesa", listaTavoliAttesa));
        grigliaTavoli.add(panelLista("Confermate", listaTavoliConfermate));
        grigliaTavoli.add(panelLista("Rifiutate", listaTavoliRifiutate));
        panelTavoli.add(grigliaTavoli, BorderLayout.CENTER);
        var azioniTavoli = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        azioniTavoli.setOpaque(false);
        var btnAggiornaTavoli = creaBottone("Aggiorna");
        var btnConfermaTavolo = creaBottone("Conferma");
        var btnPreordine = creaBottone("Visualizza preordine");
        var btnRifiutaTavolo = new JButton("Rifiuta");
        azioniTavoli.add(btnAggiornaTavoli);
        azioniTavoli.add(btnConfermaTavolo);
        azioniTavoli.add(btnPreordine);
        azioniTavoli.add(btnRifiutaTavolo);
        panelTavoli.add(azioniTavoli, BorderLayout.SOUTH);

        var panelProdotto = new JPanel(new BorderLayout(10, 10));
        panelProdotto.setOpaque(false);
        panelProdotto.setBorder(BorderFactory.createTitledBorder("Modifica menu"));

        var formProdotto = new JPanel(new GridBagLayout());
        formProdotto.setOpaque(false);
        formProdotto.setBorder(BorderFactory.createTitledBorder("Nuovo prodotto"));
        var gbcProdotto = new GridBagConstraints();
        gbcProdotto.insets = new Insets(8, 8, 8, 8);
        gbcProdotto.fill = GridBagConstraints.HORIZONTAL;

        campoNomeProdotto = new JTextField(30);
        campoDescrizioneProdotto = new JTextField(30);
        campoPrezzoProdotto = new JTextField(30);
        comboCategoriaProdotto = new JComboBox<>(new String[] {"Cibo", "Bevande"});
        modelIngredientiProdotto = new DefaultListModel<>();
        listaIngredientiProdotto = new JList<>(modelIngredientiProdotto);
        listaIngredientiProdotto.setVisibleRowCount(8);
        listaIngredientiProdotto.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        aggiungiCampo(formProdotto, gbcProdotto, 0, "Nome:", campoNomeProdotto);
        aggiungiCampo(formProdotto, gbcProdotto, 1, "Descrizione:", campoDescrizioneProdotto);
        aggiungiCampo(formProdotto, gbcProdotto, 2, "Prezzo:", campoPrezzoProdotto);
        aggiungiCampo(formProdotto, gbcProdotto, 3, "Categoria:", comboCategoriaProdotto);
        gbcProdotto.gridx = 0; gbcProdotto.gridy = 4; gbcProdotto.gridwidth = 2;
        formProdotto.add(new JLabel("Ingredienti collegati al magazzino:"), gbcProdotto);
        gbcProdotto.gridy = 5;
        formProdotto.add(new JScrollPane(listaIngredientiProdotto), gbcProdotto);

        var btnCreaProdotto = creaBottone("Crea prodotto");
        gbcProdotto.gridy = 6;
        formProdotto.add(btnCreaProdotto, gbcProdotto);

        var panelListaMenu = new JPanel(new BorderLayout(5, 5));
        panelListaMenu.setOpaque(false);
        panelListaMenu.setBorder(BorderFactory.createTitledBorder("Prodotti nel menu"));
        modelProdottiAdmin = new DefaultListModel<>();
        listaProdottiAdmin = new JList<>(modelProdottiAdmin);
        listaProdottiAdmin.setCellRenderer(new ProdottoCellRenderer());
        panelListaMenu.add(new JScrollPane(listaProdottiAdmin), BorderLayout.CENTER);
        var azioniMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        azioniMenu.setOpaque(false);
        var btnAggiornaMenu = creaBottone("Aggiorna menu");
        var btnRimuoviProdotto = creaBottone("Rimuovi prodotto");
        azioniMenu.add(btnAggiornaMenu);
        azioniMenu.add(btnRimuoviProdotto);
        panelListaMenu.add(azioniMenu, BorderLayout.SOUTH);

        var splitMenu = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formProdotto, panelListaMenu);
        splitMenu.setResizeWeight(0.45);
        panelProdotto.add(splitMenu, BorderLayout.CENTER);

        var panelFeedback = new JPanel(new BorderLayout(5, 5));
        panelFeedback.setOpaque(false);
        panelFeedback.setBorder(BorderFactory.createTitledBorder("Feedback clienti"));
        modelFeedback = new DefaultListModel<>();
        listaFeedback = new JList<>(modelFeedback);
        graficoFeedback = new GraficoFeedbackPanel();
        var splitFeedback = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(listaFeedback), graficoFeedback);
        splitFeedback.setResizeWeight(0.55);
        panelFeedback.add(splitFeedback, BorderLayout.CENTER);
        var footerFeedback = new JPanel(new BorderLayout());
        footerFeedback.setOpaque(false);
        labelMediaFeedback = new JLabel("Gradimento medio: 0%");
        var btnAggiornaFeedback = creaBottone("Aggiorna feedback");
        footerFeedback.add(labelMediaFeedback, BorderLayout.WEST);
        footerFeedback.add(btnAggiornaFeedback, BorderLayout.EAST);
        panelFeedback.add(footerFeedback, BorderLayout.SOUTH);

        var panelMagazzino = new JPanel(new BorderLayout(5, 5));
        panelMagazzino.setOpaque(false);
        panelMagazzino.setBorder(BorderFactory.createTitledBorder("Magazzino prodotti"));
        modelMagazzino = new DefaultListModel<>();
        listaMagazzino = new JList<>(modelMagazzino);
        listaMagazzino.setCellRenderer(new MagazzinoCellRenderer());
        panelMagazzino.add(new JScrollPane(listaMagazzino), BorderLayout.CENTER);
        var footerMagazzino = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        footerMagazzino.setOpaque(false);
        var btnAggiornaMagazzino = creaBottone("Aggiorna");
        var btnModificaQuantita = creaBottone("Modifica quantita");
        var btnNuovoIngrediente = creaBottone("Nuovo ingrediente");
        footerMagazzino.add(btnAggiornaMagazzino);
        footerMagazzino.add(btnModificaQuantita);
        footerMagazzino.add(btnNuovoIngrediente);
        panelMagazzino.add(footerMagazzino, BorderLayout.SOUTH);

        adminCards.add(panelFattorini, "FATTORINI");
        adminCards.add(panelStats, "STATISTICHE");
        adminCards.add(panelTavoli, "TAVOLI");
        adminCards.add(panelFeedback, "FEEDBACK");
        adminCards.add(panelMagazzino, "MAGAZZINO");
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
        btnFeedbackAdmin.addActionListener(e -> {
            adminCardLayout.show(adminCards, "FEEDBACK");
            controller.caricaFeedbackAdmin();
        });
        btnMagazzino.addActionListener(e -> {
            adminCardLayout.show(adminCards, "MAGAZZINO");
            controller.caricaMagazzino();
        });
        btnModificaMenu.addActionListener(e -> {
            adminCardLayout.show(adminCards, "MENU");
            controller.caricaMenuAdmin();
        });
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
            controller.confermaPrenotazioneTavolo(prenotazioneTavoloSelezionata()));
        btnPreordine.addActionListener(e ->
            controller.mostraPreordineTavolo(prenotazioneTavoloSelezionata()));
        btnRifiutaTavolo.addActionListener(e ->
            controller.rifiutaPrenotazioneTavolo(prenotazioneTavoloSelezionata()));
        btnAggiornaFeedback.addActionListener(e -> controller.caricaFeedbackAdmin());
        btnAggiornaMagazzino.addActionListener(e -> controller.caricaMagazzino());
        btnModificaQuantita.addActionListener(e -> {
            var selezionata = listaMagazzino.getSelectedValue();
            if (selezionata == null) {
                mostraErrore("Seleziona un prodotto dal magazzino.");
                return;
            }
            var valore = JOptionPane.showInputDialog(frame,
                "Nuova quantita per " + selezionata.nome + ":",
                selezionata.quantita);
            if (valore != null) controller.aggiornaMagazzino(selezionata, valore);
        });
        btnNuovoIngrediente.addActionListener(e -> mostraDialogNuovoIngrediente());
        btnAggiornaMenu.addActionListener(e -> controller.caricaMenuAdmin());
        btnRimuoviProdotto.addActionListener(e ->
            controller.rimuoviProdottoDalMenu(listaProdottiAdmin.getSelectedValue()));
        btnCreaProdotto.addActionListener(e -> controller.registraProdotto(
            campoNomeProdotto.getText(),
            descrizioneProdottoConIngredienti(),
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

    private JPanel panelLista(String titolo, JList<?> lista) {
        var panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.add(new JLabel(titolo), BorderLayout.NORTH);
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    private Servizio prenotazioneTavoloSelezionata() {
        if (listaTavoliAttesa.getSelectedValue() != null) {
            return listaTavoliAttesa.getSelectedValue();
        }
        if (listaTavoliConfermate.getSelectedValue() != null) {
            return listaTavoliConfermate.getSelectedValue();
        }
        return listaTavoliRifiutate.getSelectedValue();
    }

    private String descrizioneProdottoConIngredienti() {
        var testoLibero = campoDescrizioneProdotto.getText().trim();
        var ingredienti = listaIngredientiProdotto.getSelectedValuesList().stream()
            .map(i -> i.nome)
            .toList();
        if (ingredienti.isEmpty()) return testoLibero;
        var descrizioneIngredienti = String.join(", ", ingredienti);
        if (testoLibero.isBlank()) return descrizioneIngredienti;
        return testoLibero + " - Ingredienti: " + descrizioneIngredienti;
    }

    private void mostraDialogPrenotazioneTavolo() {
        var panel = new JPanel(new GridLayout(4, 2, 8, 8));
        var campoGiorno = new JTextField("2026-05-20");
        var campoOra = new JTextField("20:30");
        var campoPersone = new JTextField("2");
        var preordine = new JCheckBox("Usa il carrello come preordine");
        panel.add(new JLabel("Data (AAAA-MM-GG):"));
        panel.add(campoGiorno);
        panel.add(new JLabel("Ora (HH:MM):"));
        panel.add(campoOra);
        panel.add(new JLabel("Persone:"));
        panel.add(campoPersone);
        panel.add(new JLabel("Preordine:"));
        panel.add(preordine);

        int risposta = JOptionPane.showConfirmDialog(frame, panel,
            "Prenota tavolo", JOptionPane.OK_CANCEL_OPTION);
        if (risposta == JOptionPane.OK_OPTION) {
            controller.prenotaTavolo(
                campoGiorno.getText(),
                campoOra.getText(),
                campoPersone.getText(),
                preordine.isSelected()
            );
        }
    }

    private void mostraDialogDelivery() {
        var indirizzi = controller.getIndirizziCorrenti();
        var opzioni = new ArrayList<String>();
        for (var indirizzo : indirizzi) opzioni.add(indirizzo.toString());
        opzioni.add("Aggiungi nuovo indirizzo");

        var scelta = (String) JOptionPane.showInputDialog(frame,
            "Scegli indirizzo:", "Delivery",
            JOptionPane.QUESTION_MESSAGE, null,
            opzioni.toArray(), opzioni.get(0));
        if (scelta == null) return;

        if ("Aggiungi nuovo indirizzo".equals(scelta)) {
            var panel = new JPanel(new GridLayout(2, 2, 8, 8));
            var nome = new JTextField("Casa");
            var indirizzo = new JTextField();
            panel.add(new JLabel("Nome:"));
            panel.add(nome);
            panel.add(new JLabel("Indirizzo:"));
            panel.add(indirizzo);
            int risposta = JOptionPane.showConfirmDialog(frame, panel,
                "Nuovo indirizzo", JOptionPane.OK_CANCEL_OPTION);
            if (risposta == JOptionPane.OK_OPTION && !indirizzo.getText().isBlank()) {
                controller.salvaIndirizzoCorrente(nome.getText(), indirizzo.getText());
                controller.confermaOrdineDelivery(indirizzo.getText());
            }
            return;
        }

        int index = opzioni.indexOf(scelta);
        if (index >= 0 && index < indirizzi.size()) {
            controller.confermaOrdineDelivery(indirizzi.get(index).indirizzo);
        }
    }

    private void mostraDialogNuovoIngrediente() {
        var panel = new JPanel(new GridLayout(4, 2, 8, 8));
        var nome = new JTextField();
        var quantita = new JTextField("200");
        var soglia = new JTextField("20");
        var fornitore = new JTextField("Fornitore generale");
        panel.add(new JLabel("Nome ingrediente:"));
        panel.add(nome);
        panel.add(new JLabel("Quantita:"));
        panel.add(quantita);
        panel.add(new JLabel("Soglia minima:"));
        panel.add(soglia);
        panel.add(new JLabel("Fornitore:"));
        panel.add(fornitore);
        int risposta = JOptionPane.showConfirmDialog(frame, panel,
            "Nuovo ingrediente", JOptionPane.OK_CANCEL_OPTION);
        if (risposta == JOptionPane.OK_OPTION) {
            controller.aggiungiIngredienteMagazzino(
                nome.getText(), quantita.getText(), soglia.getText(), fornitore.getText());
        }
    }

    private void mostraDialogFeedback() {
        var servizio = listaStorico.getSelectedValue();
        if (servizio == null) {
            mostraErrore("Seleziona un ordine dallo storico.");
            return;
        }

        var panel = new JPanel(new BorderLayout(8, 8));
        var categoria = new JComboBox<>(new String[] {
            "prodotto", "fattorino", "servizio al tavolo"
        });
        var voto = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        var rigaVoto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rigaVoto.add(new JLabel("Categoria:"));
        rigaVoto.add(categoria);
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
                (String) categoria.getSelectedItem(),
                (Integer) voto.getValue(),
                commento.getText()
            );
        }
    }

    private void mostraDialogPiadinaComponibile(Prodotto base) {
        String[] carni = {
            "Salsiccia", "Prosciutto crudo", "Prosciutto cotto", "Salame",
            "Speck", "Pollo", "Tonno", "Bresaola", "Kebab"
        };
        String[] extra = {
            "Squacquerone", "Mozzarella", "Fontina", "Brie", "Scamorza",
            "Rucola", "Pomodoro", "Verdure grigliate", "Funghi", "Cipolla",
            "Peperoni", "Basilico", "Origano", "Maionese", "Salsa yogurt",
            "Salsa Linda", "Nutella", "Patatine"
        };

        var testoBase = base == null ? "" : (base.nome + " " + base.descrizione).toLowerCase();
        var panel = new JPanel(new GridLayout(1, 2, 10, 0));
        var panelCarni = new JPanel(new GridLayout(0, 1));
        panelCarni.setBorder(BorderFactory.createTitledBorder("Carni (+1 euro)"));
        var panelExtra = new JPanel(new GridLayout(0, 1));
        panelExtra.setBorder(BorderFactory.createTitledBorder("Formaggi, verdure, salse (+0.50 euro)"));

        var checksCarni = new ArrayList<JCheckBox>();
        for (var ingrediente : carni) {
            var check = new JCheckBox(ingrediente);
            check.setSelected(ingredientePresente(testoBase, ingrediente));
            checksCarni.add(check);
            panelCarni.add(check);
        }

        var checksExtra = new ArrayList<JCheckBox>();
        for (var ingrediente : extra) {
            var check = new JCheckBox(ingrediente);
            check.setSelected(ingredientePresente(testoBase, ingrediente));
            checksExtra.add(check);
            panelExtra.add(check);
        }

        panel.add(panelCarni);
        panel.add(panelExtra);

        String titolo = base == null
            ? "Componi la tua piadina"
            : "Modifica " + base.nome;
        int risposta = JOptionPane.showConfirmDialog(frame, panel,
            titolo, JOptionPane.OK_CANCEL_OPTION);
        if (risposta != JOptionPane.OK_OPTION) return;

        var ingredienti = new ArrayList<String>();
        double prezzo = 5.00;
        for (var check : checksCarni) {
            if (check.isSelected()) {
                ingredienti.add(check.getText());
                prezzo += 1.00;
            }
        }
        for (var check : checksExtra) {
            if (check.isSelected()) {
                ingredienti.add(check.getText());
                prezzo += 0.50;
            }
        }
        if (base != null && ingredienti.isEmpty()) {
            ingredienti.add("Base piadina");
        }
        if (base == null) {
            controller.aggiungiPiadinaPersonalizzata("Piadina componibile", ingredienti, prezzo);
        } else {
            controller.aggiungiPiadinaPersonalizzata(base.nome + " - modificata", ingredienti, prezzo);
        }
    }

    private boolean ingredientePresente(String testo, String ingrediente) {
        var ing = ingrediente.toLowerCase();
        if (testo.contains(ing)) return true;
        return switch (ing) {
            case "prosciutto crudo" -> testo.contains("crudo");
            case "prosciutto cotto" -> testo.contains("cotto");
            case "salsa yogurt" -> testo.contains("yogurt");
            case "salsa linda" -> testo.contains("linda");
            case "verdure grigliate" -> testo.contains("verdure");
            default -> false;
        };
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

    private final class MagazzinoCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            var c = super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            if (!isSelected && value instanceof Magazzino riga
                    && riga.quantita <= riga.sogliaMinima) {
                c.setBackground(new Color(255, 210, 210));
                c.setForeground(new Color(130, 0, 0));
            }
            return c;
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
            } else if ("Piadina componibile".equalsIgnoreCase(prodotto.nome)) {
                setBackground(new Color(255, 235, 205));
                titolo.setForeground(COLORE_PRIMARIO);
                descrizione.setForeground(new Color(90, 55, 25));
            } else {
                setBackground(list.getBackground());
                titolo.setForeground(list.getForeground());
                descrizione.setForeground(Color.DARK_GRAY);
            }
            setOpaque(true);
            return this;
        }
    }

    private final class GraficoVenditePanel extends JPanel {
        private List<StatisticaProdotto> statistiche = List.of();

        private GraficoVenditePanel() {
            setPreferredSize(new Dimension(360, 260));
            setBackground(Color.WHITE);
        }

        void setStatistiche(List<StatisticaProdotto> statistiche) {
            this.statistiche = statistiche.stream()
                .filter(s -> s.totaleOrdinato > 0)
                .limit(8)
                .toList();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLORE_PRIMARIO);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Vendite prodotti", 16, 24);
            if (statistiche.isEmpty()) {
                g2.drawString("Nessun ordine completato.", 16, 56);
                g2.dispose();
                return;
            }
            int max = statistiche.stream().mapToInt(s -> s.totaleOrdinato).max().orElse(1);
            int y = 48;
            int barMax = Math.max(80, getWidth() - 185);
            for (var stat : statistiche) {
                int width = Math.max(6, (int) (barMax * (stat.totaleOrdinato / (double) max)));
                g2.setColor(new Color(225, 225, 225));
                g2.fillRect(150, y, barMax, 18);
                g2.setColor(COLORE_ACCENTO);
                g2.fillRect(150, y, width, 18);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(abbrevia(stat.nome, 18), 16, y + 14);
                g2.drawString(String.valueOf(stat.totaleOrdinato), 155 + barMax, y + 14);
                y += 30;
            }
            g2.dispose();
        }
    }

    private final class GraficoFeedbackPanel extends JPanel {
        private final Map<String, Double> medie = new LinkedHashMap<>();

        private GraficoFeedbackPanel() {
            setPreferredSize(new Dimension(330, 240));
            setBackground(Color.WHITE);
            medie.put("prodotto", 0.0);
            medie.put("fattorino", 0.0);
            medie.put("servizio al tavolo", 0.0);
        }

        void setFeedback(List<DettaglioFeedback> feedback) {
            for (var categoria : List.of("prodotto", "fattorino", "servizio al tavolo")) {
                medie.put(categoria, feedback.stream()
                    .filter(f -> f.categoria.equalsIgnoreCase(categoria)
                        || ("servizio al tavolo".equals(categoria)
                            && f.categoria.toLowerCase().contains("tavolo")))
                    .mapToInt(f -> f.voto)
                    .average()
                    .orElse(0));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLORE_PRIMARIO);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Feedback per categoria", 16, 24);
            int y = 56;
            int barMax = Math.max(80, getWidth() - 175);
            for (var entry : medie.entrySet()) {
                int width = (int) (barMax * (entry.getValue() / 5.0));
                g2.setColor(new Color(225, 225, 225));
                g2.fillRect(140, y, barMax, 20);
                g2.setColor(COLORE_ACCENTO);
                g2.fillRect(140, y, width, 20);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(entry.getKey(), 16, y + 15);
                g2.drawString(String.format("%.1f/5", entry.getValue()), 145 + barMax, y + 15);
                y += 38;
            }
            g2.dispose();
        }
    }

    private String abbrevia(String testo, int max) {
        if (testo.length() <= max) return testo;
        return testo.substring(0, Math.max(0, max - 3)) + "...";
    }

    private JLabel creaLogoLabel(int maxWidth, int maxHeight) {
        var icon = new ImageIcon(LOGO_PATH);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        double ratio = Math.min(
            maxWidth / (double) icon.getIconWidth(),
            maxHeight / (double) icon.getIconHeight());
        int width = Math.max(1, (int) Math.round(icon.getIconWidth() * ratio));
        int height = Math.max(1, (int) Math.round(icon.getIconHeight() * ratio));
        var scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        var label = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(maxWidth, maxHeight));
        return label;
    }

    // Helper per creare bottoni stilizzati
    private JButton creaBottone(String testo) {
        var btn = new JButton(testo);
        btn.setBackground(COLORE_PRIMARIO);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setPreferredSize(new Dimension(170, 40));
        btn.setMinimumSize(new Dimension(150, 38));
        return btn;
    }
}
