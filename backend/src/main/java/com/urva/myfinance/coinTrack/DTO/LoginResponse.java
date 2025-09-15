package com.urva.myfinance.coinTrack.DTO;

import com.urva.myfinance.coinTrack.Model.User;

public class LoginResponse {
    private String token;
    private User user;

    public LoginResponse() {
    }

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
        // Remove password from user for security
        if (this.user != null) {
            this.user.setPassword(null);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        // Remove password from user for security
        if (this.user != null) {
            this.user.setPassword(null);
        }
    }
}
