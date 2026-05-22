package KPNAutomation;

import java.sql.*;
import java.util.*;

public class OracleDB {

    private final String jdbcUrl;
    private final String user;
    private final String pass;

    public OracleDB(String jdbcUrl, String user, String pass) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.pass = pass;
    }

    public Connection connect() {
        try {
            return DriverManager.getConnection(jdbcUrl, user, pass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Oracle DB: " + e.getMessage(), e);
        }
    }

    /** Returns first column of first row as String */
    public String getSingleValue(String sql, Object... params) {
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object val = rs.getObject(1);
                    return val == null ? null : val.toString();
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }

    /** Returns first column of first row as Long */
    public Long getSingleLong(String sql, Object... params) {
        String val = getSingleValue(sql, params);
        if (val == null || val.trim().isEmpty()) return null;
        return Long.parseLong(val.trim());
    }

    /** Returns all rows as List of Maps (columnLabel->value) */
    public List<Map<String, Object>> getRows(String sql, Object... params) {
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();

                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= cols; c++) {
                        String colName = md.getColumnLabel(c);
                        row.put(colName, rs.getObject(c));
                    }
                    rows.add(row);
                }
                return rows;
            }

        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }
}