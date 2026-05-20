package com.piadineria.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Classe di utilità per la preparazione degli statement SQL.
 * Centralizza la creazione dei PreparedStatement così ogni DAO
 * non deve gestire questa parte ripetitiva.
 */
public class DAOUtils {

    /**
     * Prepara uno statement SQL con i parametri forniti.
     * I parametri sostituiscono i '?' nella query nell'ordine in cui vengono passati.
     */
    public static PreparedStatement prepareStatement(
            Connection connection, String query, Object... params) throws SQLException {

        var statement = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            // setObject funziona per tutti i tipi: String, Int, Boolean, Date ecc.
            statement.setObject(i + 1, params[i]);
        }
        return statement;
    }
}
