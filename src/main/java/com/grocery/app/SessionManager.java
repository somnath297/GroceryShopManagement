package com.grocery.app;

import com.grocery.model.User;

/**
 * Singleton that holds the currently logged-in user session.
 * Used across all controllers to determine the authenticated user.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        currentUser = null;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }

    public String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : "";
    }
}
