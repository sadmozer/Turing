public class Utente {
    private String username;
    private String password;


    Utente(String username, String password) {
        this.username = username;
        this.password = password;
    }

    String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "username: " + username + "\npassword: " + password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        else if (!(obj instanceof Utente)) {
            return false;
        }
        else {
            return this.username.equals(((Utente) obj).getUsername());
        }
    }
}
