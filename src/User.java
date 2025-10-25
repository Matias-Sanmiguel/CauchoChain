import java.util.UUID;

public class User {

    private String name;
    private UUID userId;
    // Alias de la wallet asociada a este usuario (puede ser null si no tiene wallet)
    private String alias;

    public User(String name) {
        this.name = name;
        this.userId = UUID.randomUUID();
    }

    // Constructor opcional que crea un usuario y le asigna un alias de wallet
    public User(String name, String alias) {
        this.name = name;
        this.userId = UUID.randomUUID();
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return "User[name=" + name + ", userId=" + userId + ", alias=" + alias + "]";
    }
}