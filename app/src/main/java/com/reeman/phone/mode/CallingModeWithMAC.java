package com.reeman.phone.mode;

public class CallingModeWithMAC {
    private String token;
    private String body;

    public CallingModeWithMAC(String token, String body) {
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

    public void setBody(Body body) {
        this.body = String.valueOf(body);
    }

    public static class Body {
        private String map;
        private String point;

        public Body(String map, String point) {
            this.map = map;
            this.point = point;
        }

        public String getMap() {
            return map;
        }

        public void setMap(String map) {
            this.map = map;
        }

        public String getPoint() {
            return point;
        }

        public void setPoint(String point) {
            this.point = point;
        }
    }
}
