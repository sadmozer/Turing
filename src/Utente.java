public class Utente {
    private String username;
    private String password;
    public Utente(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "username: " + username + "\npassword: " + password;
    }
}
