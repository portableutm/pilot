package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import java.util.List;

class ContingencyPlan {
    private List<String> contingency_cause;
    private String contingency_location_description;
    private ContingencyPolygon contingency_polygon;
    private String contingency_response;
    private String free_text;
    private int loiter_altitude;
    private int relative_preference;
    private List<Integer> relevant_operation_volumes;
    private String valid_time_begin;
    private String valid_time_end;

    public ContingencyPlan(List<String> contingency_cause, String contingency_location_description, ContingencyPolygon contingency_polygon, String contingency_response, String free_text, int loiter_altitude, int relative_preference, List<Integer> relevant_operation_volumes, String valid_time_begin, String valid_time_end) {
        this.contingency_cause = contingency_cause;
        this.contingency_location_description = contingency_location_description;
        this.contingency_polygon = contingency_polygon;
        this.contingency_response = contingency_response;
        this.free_text = free_text;
        this.loiter_altitude = loiter_altitude;
        this.relative_preference = relative_preference;
        this.relevant_operation_volumes = relevant_operation_volumes;
        this.valid_time_begin = valid_time_begin;
        this.valid_time_end = valid_time_end;
    }
}
