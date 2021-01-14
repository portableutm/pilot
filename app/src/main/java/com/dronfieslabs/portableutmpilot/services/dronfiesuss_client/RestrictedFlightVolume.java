package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class RestrictedFlightVolume {

    private String id;
    private List<LatLng> polygon;
    private int minAltitude;
    private int maxAltitude;
    private String comments;

    //------------------------------------------------------------------------------------
    //----------------------------------- CONSTRUCTORS -----------------------------------
    //------------------------------------------------------------------------------------

    public RestrictedFlightVolume(String id, List<LatLng> polygon, int minAltitude, int maxAltitude, String comments) {
        this.id = id;
        this.polygon = polygon;
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.comments = comments;
    }


    //------------------------------------------------------------------------------------
    //------------------------------- GETTERS AND SETTERS  -------------------------------
    //------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<LatLng> getPolygon() {
        return polygon;
    }

    public void setPolygon(List<LatLng> polygon) {
        this.polygon = polygon;
    }

    public int getMinAltitude() {
        return minAltitude;
    }

    public void setMinAltitude(int minAltitude) {
        this.minAltitude = minAltitude;
    }

    public int getMaxAltitude() {
        return maxAltitude;
    }

    public void setMaxAltitude(int maxAltitude) {
        this.maxAltitude = maxAltitude;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

}
