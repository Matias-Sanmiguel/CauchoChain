import java.util.UUID;

public class User {

    private String name;
    private UUID userId;

    public User(String name) {
        this.name = name;
        this.userId = UUID.randomUUID();
    }

    public String getName() {
        return name;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "User[name=" + name + ", userId=" + userId + "]";
    }
}