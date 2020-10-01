package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities;

import java.util.Date;
import java.util.List;

public class Operation {

    public enum EnumOperationState{
        PROPOSED, NOT_ACCEPTED, ACCEPTED, NONCONFORMING, ACTIVATED, ROGUE, CLOSED
    }

    private String id;
    private String description;
    private List<GPSCoordinates> polygon;
    private Date startDatetime;
    private Date endDatetime;
    private int maxAltitude;
    private String pilotName;
    private String droneId;
    private String droneDescription;
    private EnumOperationState state;
    private String owner;

    //--------------------------------------------------------------------------------------------------------
    //--------------------------------------------- CONSTRUCTORS ---------------------------------------------
    //--------------------------------------------------------------------------------------------------------

    public Operation(String id, String description, List<GPSCoordinates> polygon, Date startDatetime, Date endDatetime, int maxAltitude, String pilotName, String droneId, String droneDescription, EnumOperationState state, String owner) {
        this.id = id;
        this.description = description;
        this.polygon = polygon;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.maxAltitude = maxAltitude;
        this.pilotName = pilotName;
        this.droneId = droneId;
        this.droneDescription = droneDescription;
        this.state = state;
        this.owner = owner;
    }

    //--------------------------------------------------------------------------------------------------------
    //------------------------------------------ GETTERS Y SETTERS  ------------------------------------------
    //--------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GPSCoordinates> getPolygon() {
        return polygon;
    }

    public void setPolygon(List<GPSCoordinates> polygon) {
        this.polygon = polygon;
    }

    public Date getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Date startDatetime) {
        this.startDatetime = startDatetime;
    }

    public Date getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Date endDatetime) {
        this.endDatetime = endDatetime;
    }

    public int getMaxAltitude() {
        return maxAltitude;
    }

    public void setMaxAltitude(int maxAltitude) {
        this.maxAltitude = maxAltitude;
    }

    public String getPilotName() {
        return pilotName;
    }

    public void setPilotName(String pilotName) {
        this.pilotName = pilotName;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public String getDroneDescription() {
        return droneDescription;
    }

    public void setDroneDescription(String droneDescription) {
        this.droneDescription = droneDescription;
    }

    public EnumOperationState getState() {
        return state;
    }

    public void setState(EnumOperationState state) {
        this.state = state;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getDurationInHours(){
        long durationInMillis = (getEndDatetime().getTime() - getStartDatetime().getTime());
        long durationInSeconds = durationInMillis/1000l;
        long durationInMinutes = durationInSeconds/60l;
        long durationInHours = durationInMinutes/60l;
        return (int)durationInHours;
    }
}
