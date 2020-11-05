package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class ParaglidingPosition {

    private int altitude_gps;
    private Location location;
    private String time_sent;

    public ParaglidingPosition(int altitude_gps, Location location, String time_sent) {
        this.altitude_gps = altitude_gps;
        this.location = location;
        this.time_sent = time_sent;
    }
}
