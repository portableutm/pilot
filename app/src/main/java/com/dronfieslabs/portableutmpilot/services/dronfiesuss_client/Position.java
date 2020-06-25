package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class Position {

    private int altitude_gps;
    private Location location;
    private String time_sent;
    private String gufi;

    public Position(int altitude_gps, Location location, String time_sent, String gufi) {
        this.altitude_gps = altitude_gps;
        this.location = location;
        this.time_sent = time_sent;
        this.gufi = gufi;
    }
}
