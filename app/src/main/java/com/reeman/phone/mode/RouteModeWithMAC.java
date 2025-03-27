package com.reeman.phone.mode;

public class RouteModeWithMAC {
    private String token;
    private String body;

    public RouteModeWithMAC(String token, String body) {
        this.token = token;
        this.body = body;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
