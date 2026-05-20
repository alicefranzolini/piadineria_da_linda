package com.piadineria.data;

/**
 * Eccezione unchecked che wrappa le SQLException di JDBC.
 * Come spiegato nel README: non vogliamo che le SQLException
 * "bubblino up" in tutte le firme dei metodi, quindi le convertiamo
 * in una RuntimeException che non richiede di essere dichiarata.
 */
public class DAOException extends RuntimeException {
    public DAOException(Exception cause) {
        super(cause);
    }

    public DAOException(String message) {
        super(message);
    }
}
