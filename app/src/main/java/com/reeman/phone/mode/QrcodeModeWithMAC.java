package com.reeman.phone.mode;

public class QrcodeModeWithMAC {
    private String token;
    private String body;

    public QrcodeModeWithMAC(String token, String body) {
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

    public static class ManualBody {
        private String first;
        private String second;

        public ManualBody(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }
    }

    public static class AGVManualBody {
        private String map;
        private String point;

        public AGVManualBody(String map, String point) {
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
