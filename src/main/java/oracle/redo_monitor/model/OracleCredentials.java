package oracle.redo_monitor.model;

public class OracleCredentials {
    private final String url;
    private final String username;
    private final String password;
    private final String role;
    private final String state;
    private final String litinc;

    public OracleCredentials(String url, String username, String password, String role, String state, String litinc) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.role = role;
        this.state = state;
        this.litinc = litinc;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getState() { return state; }
    public String getLitinc() { return litinc; }
}
