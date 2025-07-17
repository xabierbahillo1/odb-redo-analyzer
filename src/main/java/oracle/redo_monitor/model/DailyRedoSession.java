package oracle.redo_monitor.model;

import oracle.redo_monitor.utils.SizeFormatter;

public class DailyRedoSession {
    private int id;
    private String date;
    private Long oracleRedoSize;
    private Long calculatedRedoSize;
    private String state;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getOracleRedoSize() { return oracleRedoSize; }
    public void setOracleRedoSize(Long oracleRedoSize) { this.oracleRedoSize = oracleRedoSize; }

    public Long getCalculatedRedoSize() { return calculatedRedoSize; }
    public void setCalculatedRedoSize(Long calculatedRedoSize) { this.calculatedRedoSize = calculatedRedoSize; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getOracleRedoSizeFormatted() {
        if (oracleRedoSize == null) return "N/A";
        if (oracleRedoSize == -1L) return "NOARCHIVELOG";
        return SizeFormatter.formatBytes(oracleRedoSize);
    }

    public String getCalculatedRedoSizeFormatted() {
        if (calculatedRedoSize == null) return "N/A";
        return SizeFormatter.formatBytes(calculatedRedoSize);
    }
}
