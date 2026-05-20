package com.piadineria;

import com.piadineria.controller.Controller;
import com.piadineria.model.ModelImpl;
import com.piadineria.view.View;

import javax.swing.*;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto di ingresso dell'applicazione.
 *
 * Come descritto nel README:
 * "The App class works as the entry point: it establishes a connection
 * to the database and creates the initial model, view and controller,
 * wiring them all together."
 *
 * ============================================================
 * CONFIGURAZIONE DATABASE - MODIFICA QUI I TUOI DATI
 * ============================================================
 * Cambia DB_USER e DB_PASSWORD con le credenziali di MySQL Workbench.
 * Se MySQL gira sulla porta standard (3306) non devi cambiare altro.
 */
public final class App {

    // -----------------------------------------------------------------------
    // ⚙️  CONFIGURAZIONE - modifica questi valori con i tuoi dati MySQL
    // -----------------------------------------------------------------------
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/PiadineriaLinda";
    private static final String DB_USER     = "root";        // ← il tuo utente MySQL
    private static final String DB_PASSWORD = "Sabrin@05!";    // ← la tua password MySQL
    // -----------------------------------------------------------------------

    public static void main(String[] args) {

        // Tutti gli aggiornamenti alla GUI devono avvenire sull'Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {

            // 1. Stabilisci la connessione al database
            var connection = connetti();
            if (connection == null) return; // l'errore è già stato mostrato

            // 2. Crea Model, View e Controller
            var model      = new ModelImpl(connection);
            var view       = new View();
            var controller = new Controller(model, view);

            // 3. Collega View e Controller tra loro
            view.setController(controller);

            // 4. Rendi visibile la finestra e avvia il flusso
            view.show();
            controller.start();
        });
    }

    /**
     * Tenta la connessione al database MySQL.
     * Mostra un messaggio di errore chiaro se qualcosa va storto.
     */
    private static java.sql.Connection connetti() {
        try {
            var conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ Connessione al database riuscita!");
            return conn;
        } catch (SQLException e) {
            // Messaggio di errore dettagliato per aiutare a capire cosa non va
            String messaggio = """
                    Impossibile connettersi al database.
                    
                    Controlla che:
                    1. MySQL sia avviato (icona verde in MySQL Workbench)
                    2. DB_USER e DB_PASSWORD in App.java siano corretti
                    3. Il database 'PiadineriaLinda' esista
                       (esegui prima piadineria.ddl in MySQL Workbench)
                    
                    Errore tecnico: %s
                    """.formatted(e.getMessage());
            JOptionPane.showMessageDialog(null, messaggio,
                    "Errore di connessione", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
