package com.quantasip.plotpoint;
import java.util.HashMap;
public class UserData {
    private static final HashMap<String, User> userDatabase = new HashMap<>();
    // Add a user to the database
    public static void addUser(String username, String password, UserRole role) {
        userDatabase.put(username, new User(username, password, role));
    }
    // Get a user from the database
    public static User getUser(String username) {
        return userDatabase.get(username);
    }
    // Inner class to represent a user
    public static class User {
        private final String username;
        private final String password;
        private final UserRole role;
        public User(String username, String password, UserRole role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
        public String getUsername() {
            return username;
        }
        public String getPassword() {
            return password;
        }
        public UserRole getRole() {
            return role;
        }
    }
}
