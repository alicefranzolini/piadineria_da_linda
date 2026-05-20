package com.piadineria.view;

import com.piadineria.controller.Controller;
import com.piadineria.data.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
    private JLabel         labelCarrello;
    private JList<Servizio> listaStorico;
    private DefaultListModel<Servizio> modelStorico;
    private JList<StatisticaProdotto> listaStats;
    private DefaultListModel<StatisticaProdotto> modelStats;
    private JList<Servizio> listaDelivery;
    private DefaultListModel<Servizio> modelDelivery;

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
        modelProdotti.clear();
        prodotti.forEach(modelProdotti::addElement);
    }

    public void mostraStorico(List<Servizio> storico) {
        modelStorico.clear();
        storico.forEach(modelStorico::addElement);
    }

    public void aggiornaCarrello(int numeroProdotti) {
        labelCarrello.setText("🛒 Carrello: " + numeroProdotti + " prodotti");
    }

    public void mostraStatistiche(List<StatisticaProdotto> stats) {
        modelStats.clear();
        stats.forEach(modelStats::addElement);
        cardLayout.show(cards, "STATISTICHE");
    }

    public void mostraFattorino(String nome) {
        cardLayout.show(cards, "FATTORINO");
    }

    public void mostraOrdiniDelivery(List<Servizio> ordini) {
        modelDelivery.clear();
        ordini.forEach(modelDelivery::addElement);
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
        var centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.setOpaque(false);

        // Lista prodotti
        modelProdotti = new DefaultListModel<>();
        listaProdotti  = new JList<>(modelProdotti);
        listaProdotti.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var panelProdotti = new JPanel(new BorderLayout(5, 5));
        panelProdotti.setOpaque(false);
        panelProdotti.add(new JLabel("📋 Menù"), BorderLayout.NORTH);
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

        btnSvuota.addActionListener(e -> controller.svuotaCarrello());

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

        var titolo = new JLabel("Area fattorino");
        titolo.setFont(new Font("Serif", Font.BOLD, 18));
        titolo.setForeground(Color.WHITE);
        header.add(titolo, BorderLayout.WEST);

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
        var btnAggiorna = creaBottone("Aggiorna");
        azioni.add(btnAggiorna);
        panel.add(azioni, BorderLayout.SOUTH);

        btnLogout.addActionListener(e -> controller.logout());
        btnAggiorna.addActionListener(e -> controller.caricaOrdiniDelivery());

        return panel;
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
