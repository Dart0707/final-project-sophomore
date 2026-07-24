package myhelper;
 
/**
 * Model class representing a user account.
 */
public class User {
 
    private int    id;
    private String username;
    private String role;      // "Admin" | "Instructor" | "Student"
 
    public User() {}
 
    public User(int id, String username, String role) {
        this.id       = id;
        this.username = username;
        this.role     = role;
    }
 
    // ── Getters ──────────────────────────────────────────────────────────────────
 
    public int    getId()       { return id; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
 
    // ── Setters ──────────────────────────────────────────────────────────────────
 
    public void setId(int id)             { this.id = id; }
    public void setUsername(String u)     { this.username = u; }
    public void setRole(String role)      { this.role = role; }
 
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}