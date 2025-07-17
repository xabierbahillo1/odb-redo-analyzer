package oracle.redo_monitor.model;

import java.sql.Timestamp;
import java.util.Objects;

import oracle.redo_monitor.utils.SizeFormatter;

public class RedoSession {
    private final int sid;
    private final int serial;
    private final String username;
    private final String program;
    private final Timestamp logonTime;
    private final long redoBytes;
    private final Timestamp actual_date;
    private final boolean sameDay;

    public RedoSession(int sid, int serial, String username, String program,
                       Timestamp logonTime, long redoBytes, Timestamp actual_date,
                       boolean sameDay) {
        this.sid = sid;
        this.serial = serial;
        this.username = username;
        this.program = program;
        this.logonTime = logonTime;
        this.redoBytes = redoBytes;
        this.actual_date = actual_date;
        this.sameDay = sameDay;
    }

    public int getSid() {
        return sid;
    }

    public int getSerial() {
        return serial;
    }

    public String getUsername() {
        return username;
    }

    public String getProgram() {
        return program;
    }

    public Timestamp getLogonTime() {
        return logonTime;
    }

    public long getRedoBytes() {
        return redoBytes;
    }

    public Timestamp getActual_date() {
        return actual_date;
    }

    public boolean isSameDay() {
        return sameDay;
    }

    public String getFormattedRedoSize() {
        return SizeFormatter.formatBytes(redoBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RedoSession)) return false;
        RedoSession that = (RedoSession) o;
        return sid == that.sid &&
                serial == that.serial &&
                redoBytes == that.redoBytes &&
                sameDay == that.sameDay &&
                Objects.equals(username, that.username) &&
                Objects.equals(program, that.program) &&
                Objects.equals(logonTime, that.logonTime) &&
                Objects.equals(actual_date, that.actual_date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sid, serial, username, program, logonTime, redoBytes, actual_date, sameDay);
    }

    @Override
    public String toString() {
        return "RedoSession{" +
                "sid=" + sid +
                ", serial=" + serial +
                ", username='" + username + '\'' +
                ", program='" + program + '\'' +
                ", logonTime=" + logonTime +
                ", redoBytes=" + redoBytes +
                ", formatted='" + getFormattedRedoSize() + '\'' +
                ", actual_date=" + actual_date +
                ", sameDay=" + sameDay +
                '}';
    }
}
