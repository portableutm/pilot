package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class Position {

    private int altitude_gps;
    private Location location;
    private double heading;
    private String time_sent;
    private String gufi;

    public Position(int altitude_gps, Location location, double heading, String time_sent, String gufi) {
        this.altitude_gps = altitude_gps;
        this.location = location;
        this.heading = heading;
        this.time_sent = time_sent;
        this.gufi = gufi;
    }
}
