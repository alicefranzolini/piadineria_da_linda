CREATE DATABASE IF NOT EXISTS PiadineriaLinda
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE PiadineriaLinda;

CREATE TABLE IF NOT EXISTS UTENTE (
    id_utente INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(64) NOT NULL,
    cognome VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password VARCHAR(128) NOT NULL,
    CONSTRAINT utente_pk PRIMARY KEY (id_utente),
    CONSTRAINT utente_email_unique UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS FATTORINO (
    id_fattorino INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(64) NOT NULL,
    cognome VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password VARCHAR(128) NOT NULL,
    CONSTRAINT fattorino_pk PRIMARY KEY (id_fattorino),
    CONSTRAINT fattorino_email_unique UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS CATEGORIA_PRODOTTO (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    nome_categoria VARCHAR(64) NOT NULL,
    CONSTRAINT categoria_prodotto_pk PRIMARY KEY (id_categoria),
    CONSTRAINT categoria_prodotto_nome_unique UNIQUE (nome_categoria)
);

CREATE TABLE IF NOT EXISTS PRODOTTO (
    id_prodotto INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(128) NOT NULL,
    descrizione VARCHAR(500) NULL,
    costo_prodotto DOUBLE NOT NULL,
    disponibilita BOOLEAN NOT NULL DEFAULT TRUE,
    id_categoria INT NOT NULL,
    CONSTRAINT prodotto_pk PRIMARY KEY (id_prodotto),
    CONSTRAINT prodotto_categoria_fk FOREIGN KEY (id_categoria)
        REFERENCES CATEGORIA_PRODOTTO (id_categoria)
);

CREATE TABLE IF NOT EXISTS STATO_SERVIZIO (
    id_stato INT NOT NULL AUTO_INCREMENT,
    nome_stato VARCHAR(64) NOT NULL,
    CONSTRAINT stato_servizio_pk PRIMARY KEY (id_stato),
    CONSTRAINT stato_servizio_nome_unique UNIQUE (nome_stato)
);

CREATE TABLE IF NOT EXISTS SERVIZIO (
    id_servizio INT NOT NULL AUTO_INCREMENT,
    sconto_applicato DOUBLE NOT NULL DEFAULT 0,
    totale_costo DOUBLE NOT NULL DEFAULT 0,
    ora_creazione TIME NOT NULL,
    giorno_creazione DATE NOT NULL,
    id_utente INT NOT NULL,
    id_stato INT NOT NULL,
    CONSTRAINT servizio_pk PRIMARY KEY (id_servizio),
    CONSTRAINT servizio_utente_fk FOREIGN KEY (id_utente)
        REFERENCES UTENTE (id_utente),
    CONSTRAINT servizio_stato_fk FOREIGN KEY (id_stato)
        REFERENCES STATO_SERVIZIO (id_stato)
);

CREATE TABLE IF NOT EXISTS DELIVERY (
    id_delivery INT NOT NULL AUTO_INCREMENT,
    indirizzo_consegna VARCHAR(255) NOT NULL,
    costo_consegna DOUBLE NOT NULL DEFAULT 2.50,
    id_servizio INT NOT NULL,
    CONSTRAINT delivery_pk PRIMARY KEY (id_delivery),
    CONSTRAINT delivery_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio)
);

CREATE TABLE IF NOT EXISTS ASPORTO (
    id_asporto INT NOT NULL AUTO_INCREMENT,
    orario_ritiro TIME NOT NULL,
    id_servizio INT NOT NULL,
    CONSTRAINT asporto_pk PRIMARY KEY (id_asporto),
    CONSTRAINT asporto_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio)
);

CREATE TABLE IF NOT EXISTS PRENOTAZIONE_TAVOLO (
    id_prenotazione INT NOT NULL AUTO_INCREMENT,
    numero_persone INT NOT NULL DEFAULT 1,
    data_prenotazione DATE NULL,
    ora_prenotazione TIME NULL,
    id_servizio INT NOT NULL,
    CONSTRAINT prenotazione_tavolo_pk PRIMARY KEY (id_prenotazione),
    CONSTRAINT prenotazione_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio)
);

CREATE TABLE IF NOT EXISTS CONTIENE (
    id_servizio INT NOT NULL,
    id_prodotto INT NOT NULL,
    quantita INT NOT NULL DEFAULT 1,
    CONSTRAINT contiene_pk PRIMARY KEY (id_servizio, id_prodotto),
    CONSTRAINT contiene_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio),
    CONSTRAINT contiene_prodotto_fk FOREIGN KEY (id_prodotto)
        REFERENCES PRODOTTO (id_prodotto)
);

CREATE TABLE IF NOT EXISTS TRANSIZIONE_STATO (
    id_transizione INT NOT NULL AUTO_INCREMENT,
    id_servizio INT NOT NULL,
    id_stato INT NOT NULL,
    data_transizione DATETIME NOT NULL,
    CONSTRAINT transizione_stato_pk PRIMARY KEY (id_transizione),
    CONSTRAINT transizione_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio),
    CONSTRAINT transizione_stato_fk FOREIGN KEY (id_stato)
        REFERENCES STATO_SERVIZIO (id_stato)
);

CREATE TABLE IF NOT EXISTS FEEDBACK (
    id_feedback INT NOT NULL AUTO_INCREMENT,
    categoria VARCHAR(64) NOT NULL DEFAULT 'prodotto',
    voto INT NOT NULL,
    commento VARCHAR(500) NULL,
    id_servizio INT NOT NULL,
    CONSTRAINT feedback_pk PRIMARY KEY (id_feedback),
    CONSTRAINT feedback_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio),
    CONSTRAINT feedback_voto_check CHECK (voto BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS TESSERA_FEDELTA (
    id_tessera INT NOT NULL AUTO_INCREMENT,
    numero_tessera VARCHAR(32) NOT NULL,
    ordini_effettuati INT NOT NULL DEFAULT 0,
    data_ultimo_ordine DATE NULL,
    id_utente INT NOT NULL,
    CONSTRAINT tessera_fedelta_pk PRIMARY KEY (id_tessera),
    CONSTRAINT tessera_utente_fk FOREIGN KEY (id_utente)
        REFERENCES UTENTE (id_utente),
    CONSTRAINT tessera_utente_unique UNIQUE (id_utente),
    CONSTRAINT tessera_numero_unique UNIQUE (numero_tessera)
);

CREATE TABLE IF NOT EXISTS STORICO_SCONTO_APP (
    id_sconto INT NOT NULL AUTO_INCREMENT,
    id_servizio INT NOT NULL,
    id_utente INT NOT NULL,
    percentuale DOUBLE NOT NULL,
    importo_scontato DOUBLE NOT NULL,
    data_sconto DATE NOT NULL,
    CONSTRAINT storico_sconto_app_pk PRIMARY KEY (id_sconto),
    CONSTRAINT storico_sconto_servizio_fk FOREIGN KEY (id_servizio)
        REFERENCES SERVIZIO (id_servizio),
    CONSTRAINT storico_sconto_utente_fk FOREIGN KEY (id_utente)
        REFERENCES UTENTE (id_utente)
);

CREATE TABLE IF NOT EXISTS INDIRIZZO_CONSEGNA_APP (
    id_indirizzo INT NOT NULL AUTO_INCREMENT,
    id_utente INT NOT NULL,
    nome VARCHAR(64) NOT NULL,
    indirizzo VARCHAR(255) NOT NULL,
    CONSTRAINT indirizzo_consegna_app_pk PRIMARY KEY (id_indirizzo),
    CONSTRAINT indirizzo_utente_fk FOREIGN KEY (id_utente)
        REFERENCES UTENTE (id_utente)
);

CREATE TABLE IF NOT EXISTS MAGAZZINO_APP (
    id_magazzino INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(128) NOT NULL,
    quantita INT NOT NULL DEFAULT 200,
    soglia_minima INT NOT NULL DEFAULT 20,
    fornitore VARCHAR(128) NOT NULL DEFAULT 'Fornitore generale',
    CONSTRAINT magazzino_app_pk PRIMARY KEY (id_magazzino),
    CONSTRAINT magazzino_app_nome_unique UNIQUE (nome)
);

INSERT INTO CATEGORIA_PRODOTTO (nome_categoria)
VALUES ('Cibo'), ('Bevande')
ON DUPLICATE KEY UPDATE nome_categoria = VALUES(nome_categoria);

INSERT INTO STATO_SERVIZIO (nome_stato)
VALUES ('in attesa'), ('presa in carico'), ('ritirato'), ('in consegna'),
       ('consegnato'), ('confermata'), ('rifiutata')
ON DUPLICATE KEY UPDATE nome_stato = VALUES(nome_stato);

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Piadina Classica', 'Crudo, squacquerone e rucola', 6.50, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Cibo'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Piadina Vegetariana', 'Verdure grigliate, squacquerone e rucola', 6.00, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Cibo'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Piadina componibile', 'Base piadina 5 euro, ingredienti a scelta', 5.00, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Cibo'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Acqua Naturale', 'Bottiglietta 50 cl', 1.20, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Bevande'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Acqua Frizzante', 'Bottiglietta 50 cl', 1.20, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Bevande'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO PRODOTTO (nome, descrizione, costo_prodotto, disponibilita, id_categoria)
SELECT 'Coca-Cola', 'Lattina 33 cl', 2.50, TRUE, id_categoria
FROM CATEGORIA_PRODOTTO WHERE nome_categoria = 'Bevande'
ON DUPLICATE KEY UPDATE nome = nome;

INSERT INTO MAGAZZINO_APP (nome, quantita, soglia_minima, fornitore)
VALUES
('Base piadina', 200, 20, 'Fornitore generale'),
('Prosciutto crudo', 200, 20, 'Fornitore carni'),
('Prosciutto cotto', 200, 20, 'Fornitore carni'),
('Salame', 200, 20, 'Fornitore carni'),
('Speck', 200, 20, 'Fornitore carni'),
('Salsiccia', 200, 20, 'Fornitore carni'),
('Pollo', 200, 20, 'Fornitore carni'),
('Tonno', 200, 20, 'Fornitore carni'),
('Bresaola', 200, 20, 'Fornitore carni'),
('Squacquerone', 200, 20, 'Fornitore formaggi'),
('Mozzarella', 200, 20, 'Fornitore formaggi'),
('Fontina', 200, 20, 'Fornitore formaggi'),
('Brie', 200, 20, 'Fornitore formaggi'),
('Rucola', 200, 20, 'Fornitore generale'),
('Pomodoro', 200, 20, 'Fornitore generale'),
('Verdure grigliate', 200, 20, 'Fornitore generale'),
('Acqua naturale', 200, 20, 'Fornitore bevande'),
('Acqua frizzante', 200, 20, 'Fornitore bevande'),
('Coca-Cola', 200, 20, 'Fornitore bevande')
ON DUPLICATE KEY UPDATE nome = VALUES(nome);
