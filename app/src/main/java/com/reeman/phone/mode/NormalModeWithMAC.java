package com.reeman.phone.mode;

import java.util.List;

public class NormalModeWithMAC {
    private String token;
    private String body;

    public NormalModeWithMAC(String token, String body) {
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

    public static class Point {
        private String map;
        private String point;

        public Point(String map, String point) {
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

    public static class PointPair {
        private Point first;
        private Point second;

        public PointPair(Point first, Point second) {
            this.first = first;
            this.second = second;
        }

        public Point getFirst() {
            return first;
        }

        public void setFirst(Point first) {
            this.first = first;
        }

        public Point getSecond() {
            return second;
        }

        public void setSecond(Point second) {
            this.second = second;
        }
    }
}
