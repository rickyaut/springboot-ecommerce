package com.ecommerce.order.controller;

public class ValidationResponse {
    private boolean valid;
    private String message;
    private String username;

    public ValidationResponse(boolean valid, String message, String username) {
        this.valid = valid;
        this.message = message;
        this.username = username;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
