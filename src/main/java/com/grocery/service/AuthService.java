package com.grocery.service;

import com.grocery.app.SessionManager;
import com.grocery.dao.DatabaseManager;
import com.grocery.dao.UserDAO;
import com.grocery.model.User;

import java.sql.SQLException;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Attempt login. Returns the User on success, null on failure.
     */
    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank()) return null;
        if (password == null || password.isBlank()) return null;
        String hash = DatabaseManager.hashPassword(password);
        User user = userDAO.findByUsernameAndPassword(username.trim(), hash);
        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
        }
        return user;
    }

    /**
     * Change the current user's password.
     * @return true on success, false if the old password did not match.
     */
    public boolean changePassword(String oldPassword, String newPassword) throws SQLException {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return false;
        String oldHash = DatabaseManager.hashPassword(oldPassword);
        if (!oldHash.equals(current.getPasswordHash())) return false;
        String newHash = DatabaseManager.hashPassword(newPassword);
        boolean updated = userDAO.updatePassword(current.getId(), newHash);
        if (updated) {
            current.setPasswordHash(newHash);
        }
        return updated;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
