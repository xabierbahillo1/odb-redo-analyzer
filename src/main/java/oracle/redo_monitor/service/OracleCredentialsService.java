package oracle.redo_monitor.service;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import oracle.redo_monitor.model.OracleCredentials;

import java.sql.*;

@Service
public class OracleCredentialsService {

    @Value("${sqlite.url}")
    private String sqliteUrl;

    private final EncryptService encryptService;

    public OracleCredentialsService(EncryptService encryptService) {
        this.encryptService = encryptService;
    }
    
    @PostConstruct
    public void init() {
        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS oracle_credentials (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "url TEXT NOT NULL," +
                "username TEXT NOT NULL," +
                "password TEXT NOT NULL," +
                "role TEXT," +
                "state TEXT DEFAULT 'Disconnected'," +
                "litinc TEXT" +
                ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating oracle_credentials table", e);
        }
    }

    public void saveCredentials(String url, String username, String password, String role) throws Exception {
        OracleCredentials currentCreds = loadCredentialsEncrypted();
        String passwordToSave;
        if (currentCreds == null) {
            // If there are no credentials, I save it directly
            passwordToSave = encryptService.encrypt(password);
        } else {
            String currentPassword = currentCreds.getPassword();
            if (password.equals(currentPassword)) {
                passwordToSave = currentPassword; // If the key is the same, dont encrypt again
            } else {
                passwordToSave = encryptService.encrypt(password);
            }
        }

        try (Connection conn = DriverManager.getConnection(sqliteUrl);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO oracle_credentials (id, url, username, password, role, state, litinc) " +
                "VALUES (1, ?, ?, ?, ?, 'Disconnected', NULL) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "url = excluded.url, " +
                "username = excluded.username, " +
                "password = excluded.password, " +
                "role = excluded.role, " +
                "state = 'Disconnected', " +
                "litinc = NULL"
            )) {
            ps.setString(1, url);
            ps.setString(2, username);
            ps.setString(3, passwordToSave);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }

    public OracleCredentials loadCredentials() throws SQLException {
        OracleCredentials creds = null;
        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement ps = conn.prepareStatement(
                "SELECT url, username, password, role, state, litinc FROM oracle_credentials WHERE id = 1"
             );
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String decryptedPassword = null;
                try {
                    decryptedPassword = encryptService.decrypt(storedPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                creds = new OracleCredentials(
                    rs.getString("url"),
                    rs.getString("username"),
                    decryptedPassword,
                    rs.getString("role"),
                    rs.getString("state"),
                    rs.getString("litinc")
                );
            }
        }
        return creds;
    }
    
    public OracleCredentials loadCredentialsEncrypted() throws SQLException {
        OracleCredentials creds = null;
        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement ps = conn.prepareStatement(
                "SELECT url, username, password, role, state, litinc FROM oracle_credentials WHERE id = 1"
             );
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                creds = new OracleCredentials(
                    rs.getString("url"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getString("state"),
                    rs.getString("litinc")
                );
            }
        }
        return creds;
    }

    public void updateConnectionStatus(String state, String litinc) {
        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE oracle_credentials SET state = ?, litinc = ? WHERE id = 1"
             )) {
            ps.setString(1, state);
            ps.setString(2, litinc);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
