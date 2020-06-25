package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class Location {

    private String type;
    private double[] coordinates;

    public Location(String type, double[] coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }
}
