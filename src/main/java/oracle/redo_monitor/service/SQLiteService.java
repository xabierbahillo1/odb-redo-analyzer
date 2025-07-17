package oracle.redo_monitor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import oracle.redo_monitor.model.DailyRedoSession;
import oracle.redo_monitor.model.RedoSession;

import oracle.redo_monitor.model.SessionRedoUsage;
import oracle.redo_monitor.utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SQLiteService {
    
     @Autowired
    private OracleService oracleService;


    private static final Logger logger = LoggerFactory.getLogger(SQLiteService.class);

    @Value("${sqlite.url}")
    private String sqliteUrl;

    public SQLiteService() {
        // Constructor vacío
    }

    @PostConstruct
    public void init() {
        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS daily_redo_session (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT UNIQUE NOT NULL, " +
                "oracle_redo_size INTEGER, " +
                "calculated_redo_size INTEGER, " +
                "state TEXT NOT NULL DEFAULT 'OPEN'" + 
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS session_redo_usage (" +
                "daily_id INTEGER NOT NULL, " +
                "sid INTEGER, " +
                "serial INTEGER, " +
                "logon_time TEXT, " +
                "username TEXT, " +
                "program TEXT, " +
                "redo_bytes INTEGER DEFAULT 0, " +
                "initial_redo_bytes INTEGER, " +
                "actual_redo_bytes INTEGER, " +
                "last_updated TEXT, " +
                "PRIMARY KEY (daily_id, sid, serial, logon_time), " +
                "FOREIGN KEY (daily_id) REFERENCES daily_redo_session(id))"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getOrCreateDailyId(Connection conn, String dateStr) throws Exception {
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT id FROM daily_redo_session WHERE date = ?")) {
            sel.setString(1, dateStr);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // Close previous sessions that are still OPEN
        try (PreparedStatement selectStmt = conn.prepareStatement(
            "SELECT id, date FROM daily_redo_session WHERE state = 'OPEN'");
            ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                int openId = rs.getInt("id");
                String openDate = rs.getString("date");
                logger.info("Processing session with id: {}", openId);

                long totalRedoSize = 0;

                // Calculate the sum of redo_size for this id
                try (PreparedStatement sumStmt = conn.prepareStatement(
                        "SELECT SUM(redo_bytes) FROM session_redo_usage WHERE daily_id = ?")) {
                    sumStmt.setInt(1, openId);
                    try (ResultSet sumRs = sumStmt.executeQuery()) {
                        if (sumRs.next()) {
                            totalRedoSize = sumRs.getLong(1);
                        }
                    }
                }
                logger.info("Calculated total redo_size for session {}: {}", openId, totalRedoSize);

                // Calculate redo_size in Oracle archivelogs
                long oracleRedoSize = oracleService.getRedoBytesByDay(openDate);
                logger.info("Oracle archivelogs size for date {}: {}", openDate, oracleRedoSize);

                // Close the session and save the redo_size data
                try (PreparedStatement closeStmt = conn.prepareStatement(
                        "UPDATE daily_redo_session SET state = 'CLOSED', oracle_redo_size = ?, calculated_redo_size = ? WHERE id = ?")) {
                    closeStmt.setLong(1, oracleRedoSize);
                    closeStmt.setLong(2, totalRedoSize);
                    closeStmt.setInt(3, openId);
                    int rowsUpdated = closeStmt.executeUpdate();
                    logger.info("Session {} closed, rows updated: {}", openId, rowsUpdated);
                }
            }

        } catch (SQLException e) {
            logger.error("Error closing open sessions: ", e);
        }

        // Insert the new session with OPEN status
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO daily_redo_session(date, state) VALUES (?, 'OPEN')")) {
            ins.setString(1, dateStr);
            int rowsInserted = ins.executeUpdate();
            logger.info("Inserted new daily_redo_session for date {} with state OPEN, rows inserted: {}", dateStr, rowsInserted);
        }

        try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                int newId = rs.getInt(1);
                logger.info("New daily_redo_session id generated: {}", newId);
                return newId;
            }
        }
        throw new Exception("Could not get or create daily_id for date: " + dateStr);
    }

    public void storeOrUpdateSessions(List<RedoSession> sessions) {
        try (Connection conn = DriverManager.getConnection(sqliteUrl)) {

            for (RedoSession s : sessions) {
                SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_PATTERN_JAVA);
                String sessionDate = formatter.format(s.getActual_date());
                int dailyId = getOrCreateDailyId(conn, sessionDate);

                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT initial_redo_bytes FROM session_redo_usage " +
                        "WHERE daily_id = ? AND sid = ? AND serial = ? AND logon_time = ?")) {
                    sel.setInt(1, dailyId);
                    sel.setInt(2, s.getSid());
                    sel.setInt(3, s.getSerial());
                    sel.setString(4, s.getLogonTime().toString());

                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            // The session exists, update its redo_logs
                            long initialBytes = rs.getLong(1);
                            long increment = s.getRedoBytes() - initialBytes;

                            try (PreparedStatement upd = conn.prepareStatement(
                                    "UPDATE session_redo_usage " +
                                    "SET redo_bytes = ?, actual_redo_bytes = ?, last_updated = ? " +
                                    "WHERE daily_id = ? AND sid = ? AND serial = ? AND logon_time = ?")) {
                                upd.setLong(1, increment);
                                upd.setLong(2, s.getRedoBytes());
                                upd.setString(3, s.getActual_date().toString());
                                upd.setInt(4, dailyId);
                                upd.setInt(5, s.getSid());
                                upd.setInt(6, s.getSerial());
                                upd.setString(7, s.getLogonTime().toString());
                                upd.executeUpdate();
                            }
                        } else {
                            // New session
                            try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO session_redo_usage " +
                                "(daily_id, sid, serial, logon_time, username, program, redo_bytes, initial_redo_bytes, actual_redo_bytes, last_updated) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                                ins.setInt(1, dailyId);
                                ins.setInt(2, s.getSid());
                                ins.setInt(3, s.getSerial());
                                ins.setString(4, s.getLogonTime().toString());
                                ins.setString(5, s.getUsername());
                                ins.setString(6, s.getProgram());

                                if (s.isSameDay()) {
                                    // If sameDay is true, redo_bytes = current bytes, initial_redo_bytes = 0,
                                    ins.setLong(7, s.getRedoBytes());
                                    ins.setLong(8, 0L);
                                } else {
                                    // If sameDay is false, redo_bytes = 0, initial_redo_bytes = current bytes
                                    ins.setLong(7,0L);
                                    ins.setLong(8, s.getRedoBytes());
                                }

                                ins.setLong(9, s.getRedoBytes());
                                ins.setString(10, s.getActual_date().toString());

                                ins.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<DailyRedoSession> getAllDailySessions() {
        List<DailyRedoSession> sessions = new ArrayList<>();

        String query = "SELECT id, date, oracle_redo_size, calculated_redo_size, state FROM daily_redo_session ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                DailyRedoSession session = new DailyRedoSession();
                session.setId(rs.getInt("id"));
                session.setDate(rs.getString("date"));

                long oracleRedo = rs.getLong("oracle_redo_size");
                if (rs.wasNull()) {
                    session.setOracleRedoSize(null);
                } else {
                    session.setOracleRedoSize(oracleRedo);
                }

                long calculatedRedo = rs.getLong("calculated_redo_size");
                if (rs.wasNull()) {
                    session.setCalculatedRedoSize(null);
                } else {
                    session.setCalculatedRedoSize(calculatedRedo);
                }
                session.setState(rs.getString("state"));

                sessions.add(session);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessions;
    }

     public List<DailyRedoSession> getDailySessionsBetweenDates(LocalDate from, LocalDate to) {
            List<DailyRedoSession> sessions = new ArrayList<>();

            String query = 
                "SELECT id, date, oracle_redo_size, calculated_redo_size, state " +
                "FROM daily_redo_session " +
                "WHERE date BETWEEN ? AND ? " +
                "ORDER BY date DESC";

            try (Connection conn = DriverManager.getConnection(sqliteUrl);
                PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, from.toString());
                pstmt.setString(2, to.toString());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DailyRedoSession session = new DailyRedoSession();
                        session.setId(rs.getInt("id"));
                        session.setDate(rs.getString("date"));

                        long oracleRedo = rs.getLong("oracle_redo_size");
                        session.setOracleRedoSize(rs.wasNull() ? null : oracleRedo);

                        long calculatedRedo = rs.getLong("calculated_redo_size");
                        session.setCalculatedRedoSize(rs.wasNull() ? null : calculatedRedo);

                        session.setState(rs.getString("state"));

                        sessions.add(session);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return sessions;
        }



    public List<SessionRedoUsage> getSessionsByDailyId(int dailyId) {
        List<SessionRedoUsage> sessions = new ArrayList<>();

        String query = "SELECT sid, serial, logon_time, username, program, redo_bytes, initial_redo_bytes, actual_redo_bytes, last_updated " +
                       "FROM session_redo_usage WHERE daily_id = ? ORDER BY redo_bytes DESC";

        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, dailyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SessionRedoUsage session = new SessionRedoUsage();
                    session.setSid(rs.getInt("sid"));
                    session.setSerial(rs.getInt("serial"));
                    session.setLogonTime(rs.getString("logon_time"));
                    session.setUsername(rs.getString("username"));
                    session.setProgram(rs.getString("program"));

                    long redoBytes = rs.getLong("redo_bytes");
                    if (rs.wasNull()) {
                        session.setRedoBytes(null);
                    } else {
                        session.setRedoBytes(redoBytes);
                    }

                    long initialBytes = rs.getLong("initial_redo_bytes");
                    if (rs.wasNull()) {
                        session.setInitialRedoBytes(null);
                    } else {
                        session.setInitialRedoBytes(initialBytes);
                    }

                    long actualBytes = rs.getLong("actual_redo_bytes");
                    if (rs.wasNull()) {
                        session.setActualRedoBytes(null);
                    } else {
                        session.setActualRedoBytes(actualBytes);
                    }

                    session.setLastUpdated(rs.getString("last_updated"));

                    sessions.add(session);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessions;
    }

    public String getDateByDailyId(int dailyId) {
        String date = null;

        String query = "SELECT date FROM daily_redo_session WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, dailyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    date = rs.getString("date");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return date;
    }

    public void updateDailyRedoSizes(DailyRedoSession daily) {
        String query = "UPDATE daily_redo_session SET oracle_redo_size = ?, calculated_redo_size = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(sqliteUrl);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (daily.getOracleRedoSize() != null) {
                pstmt.setLong(1, daily.getOracleRedoSize());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            if (daily.getCalculatedRedoSize() != null) {
                pstmt.setLong(2, daily.getCalculatedRedoSize());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }

            pstmt.setInt(3, daily.getId());

            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
