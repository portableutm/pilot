package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import java.util.List;

class OperationGeography {
    private String type;
    private List<List<List<Double>>> coordinates;

    public OperationGeography(String type, List<List<List<Double>>> coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }
}
