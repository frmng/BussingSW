package com.example.bussingsw.ui.setup_account;

public class DUser {

    String userId;
    String name;
    String profile;
    String email;

    public DUser(String userId, String profile, String name, String email) {
        this.userId = userId;
        this.profile = profile;
        this.name = name;
        this.email = email;
    }

    public DUser() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
