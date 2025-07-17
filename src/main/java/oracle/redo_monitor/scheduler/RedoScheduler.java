package oracle.redo_monitor.scheduler;

import oracle.redo_monitor.model.OracleCredentials;
import oracle.redo_monitor.model.RedoSession;
import oracle.redo_monitor.service.OracleCredentialsService;
import oracle.redo_monitor.service.OracleService;
import oracle.redo_monitor.service.SQLiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RedoScheduler.class);

    private final OracleService oracleService;
    private final SQLiteService sqliteService;
    private final OracleCredentialsService oracleCredentialsService;

    public RedoScheduler(OracleService oracleService,
                         SQLiteService sqliteService,
                         OracleCredentialsService oracleCredentialsService) {
        this.oracleService = oracleService;
        this.sqliteService = sqliteService;
        this.oracleCredentialsService = oracleCredentialsService;
    }

    @Scheduled(fixedRateString = "${redo.check.fixedRate}")
    public void checkRedoUsage() {
        logger.info("Starting checkRedoUsage task...");

        try {
            OracleCredentials creds = oracleCredentialsService.loadCredentials();

            if (creds == null) {
                logger.warn("No Oracle credentials found. Skipping redo usage check.");
                return;
            }

            logger.info("Oracle credentials found, fetching redo usage...");

            List<RedoSession> sesiones = oracleService.getCurrentRedoUsage(
                creds.getUrl(),
                creds.getUsername(),
                creds.getPassword(),
                creds.getRole()
            );

            logger.info("Fetched {} redo sessions. Storing in SQLite...", sesiones.size());

            sqliteService.storeOrUpdateSessions(sesiones);

            logger.info("Successfully stored redo sessions.");
        } catch (Exception e) {
            logger.error("Error during checkRedoUsage execution", e);
        }
    }
}
