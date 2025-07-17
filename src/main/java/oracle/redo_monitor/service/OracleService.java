package oracle.redo_monitor.service;

import oracle.redo_monitor.model.OracleCredentials;
import oracle.redo_monitor.model.RedoSession;
import oracle.redo_monitor.utils.Constants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OracleService {

    @Autowired
    private OracleCredentialsService credentialsService;

    private static final Logger log = LoggerFactory.getLogger(OracleService.class);

    public List<RedoSession> getCurrentRedoUsage(String url, String user, String password, String role) {
        List<RedoSession> result = new ArrayList<>();

        String sql = "SELECT s.sid, s.serial#, s.username, s.program, s.logon_time, " +
                     "r.value AS redo_bytes, SYSDATE AS actual_date " +
                     "FROM v$session s " +
                     "JOIN v$sesstat r ON s.sid = r.sid " +
                     "JOIN v$statname n ON r.statistic# = n.statistic# " +
                     "WHERE n.name = 'redo size' " +
                     "AND s.username IS NOT NULL " +
                     "AND s.sid != SYS_CONTEXT('USERENV', 'SID')";
        
        Properties props = new Properties();
        props.put("user", user);
        props.put("password", password);
        if (role != null && !role.isEmpty()) {
            props.put("internal_logon", role.toLowerCase());
        }

        try (Connection conn = DriverManager.getConnection(url, props);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            credentialsService.updateConnectionStatus("Connected", null);
            log.info("{}", sql);
            while (rs.next()) {
                Timestamp logonTs = rs.getTimestamp("logon_time");
                Timestamp actualTs = rs.getTimestamp("actual_date");

                LocalDate logonDate = logonTs.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate actualDate = actualTs.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                boolean sameDay = logonDate.isEqual(actualDate);

                result.add(new RedoSession(
                    rs.getInt("sid"),
                    rs.getInt("serial#"),
                    rs.getString("username"),
                    rs.getString("program"),
                    rs.getTimestamp("logon_time"),
                    rs.getLong("redo_bytes"),
                    rs.getTimestamp("actual_date"),
                    sameDay
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            String litinc = e.getMessage();
            if (litinc != null && litinc.length() > 100) {
                litinc = litinc.substring(0, 100);
            }
            credentialsService.updateConnectionStatus("Error", litinc);
        }

        return result;
    }

    public long getRedoBytesByDay(String dayDateStr) throws SQLException {
        OracleCredentials creds = credentialsService.loadCredentials();

        Properties props = new Properties();
        props.put("user", creds.getUsername());
        props.put("password", creds.getPassword());
        if (creds.getRole() != null && !creds.getRole().isEmpty()) {
            props.put("internal_logon", creds.getRole().toLowerCase());
        }

        try (Connection conn = DriverManager.getConnection(creds.getUrl(), props)) {
            String logMode = getLogMode(conn);
            log.info("dayDateStr = {}", dayDateStr);
            log.info("logMode = {}", logMode);
            if ("NOARCHIVELOG".equalsIgnoreCase(logMode)) {
                return -1L;
            }

            String sql = "SELECT NVL(SUM(BLOCKS*BLOCK_SIZE),0) AS redo_bytes " +
                         "FROM v$archived_log " +
                         "WHERE TRUNC(COMPLETION_TIME, 'DD') = TO_DATE(?, '" + Constants.DATE_PATTERN_ORACLE + "')";
            log.info("{}", sql);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dayDateStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("redo_bytes");
                    }
                    return 0L;
                }
            }
        }
    }

    private String getLogMode(Connection conn) throws SQLException {
        String sql = "SELECT log_mode FROM v$database";
        log.info("{}", sql);

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("log_mode");
            }
            throw new SQLException("No se pudo obtener log_mode de v$database");
        }
    }
}
