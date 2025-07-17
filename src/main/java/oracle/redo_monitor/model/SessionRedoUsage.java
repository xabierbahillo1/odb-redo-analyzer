package oracle.redo_monitor.model;

import oracle.redo_monitor.utils.SizeFormatter;

public class SessionRedoUsage {
    private int sid;
    private int serial;
    private String logonTime;
    private String username;
    private String program;
    private Long redoBytes;
    private Long initialRedoBytes;
    private Long actualRedoBytes;
    private String lastUpdated;

    public SessionRedoUsage() {}

    public int getSid() { return sid; }
    public void setSid(int sid) { this.sid = sid; }

    public int getSerial() { return serial; }
    public void setSerial(int serial) { this.serial = serial; }

    public String getLogonTime() { return logonTime; }
    public void setLogonTime(String logonTime) { this.logonTime = logonTime; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public Long getRedoBytes() { return redoBytes; }
    public void setRedoBytes(Long redoBytes) { this.redoBytes = redoBytes; }

    public Long getInitialRedoBytes() { return initialRedoBytes; }
    public void setInitialRedoBytes(Long initialRedoBytes) { this.initialRedoBytes = initialRedoBytes; }

    public Long getActualRedoBytes() { return actualRedoBytes; }
    public void setActualRedoBytes(Long actualRedoBytes) {
        this.actualRedoBytes = actualRedoBytes;
    }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getRedoBytesFormatted() {
        return SizeFormatter.formatBytes(redoBytes);
    }

    public String getInitialRedoBytesFormatted() {
        return SizeFormatter.formatBytes(initialRedoBytes);
    }

    public String getActualRedoBytesFormatted() {
        return SizeFormatter.formatBytes(actualRedoBytes);
    }
}
