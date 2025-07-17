package oracle.redo_monitor.utils;

public class SizeFormatter {

    /**
     * Converts a byte value to a readable string (B, KB, MB, GB).
     *
     * @param bytes number of bytes
     * @return string
     */
    public static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024)
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        else if (bytes >= 1024L * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        else if (bytes >= 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        else
            return bytes + " B";
    }
}
