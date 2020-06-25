package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class OperationVolume {
    private String effective_time_begin;
    private String effective_time_end;
    private double min_altitude;
    private double max_altitude;
    private OperationGeography operation_geography;
    private boolean beyond_visual_line_of_sight;

    public OperationVolume(String effective_time_begin, String effective_time_end, double min_altitude, double max_altitude, OperationGeography operation_geography, boolean beyond_visual_line_of_sight) {
        this.effective_time_begin = effective_time_begin;
        this.effective_time_end = effective_time_end;
        this.min_altitude = min_altitude;
        this.max_altitude = max_altitude;
        this.operation_geography = operation_geography;
        this.beyond_visual_line_of_sight = beyond_visual_line_of_sight;
    }
}
