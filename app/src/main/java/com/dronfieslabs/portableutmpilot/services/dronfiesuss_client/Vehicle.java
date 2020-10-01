package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import java.util.Date;

public class Vehicle {

    public enum EnumVehicleClass{ MULTIROTOR, FIXEDWING };

    private String uvin;
    private Date date;
    private String nNumber;
    private String faaNumber;
    private String vehicleName;
    private String manufacturer;
    private String model;
    private EnumVehicleClass vehicleClass;
    private String registeredBy;
    private String owner;

    //------------------------------------------------------------------------------------
    //----------------------------------- CONSTRUCTORS -----------------------------------
    //------------------------------------------------------------------------------------

    public Vehicle(String uvin, Date date, String nNumber, String faaNumber, String vehicleName, String manufacturer, String model, EnumVehicleClass vehicleClass, String registeredBy, String owner) {
        this.uvin = uvin;
        this.date = date;
        this.nNumber = nNumber;
        this.faaNumber = faaNumber;
        this.vehicleName = vehicleName;
        this.manufacturer = manufacturer;
        this.model = model;
        this.vehicleClass = vehicleClass;
        this.registeredBy = registeredBy;
        this.owner = owner;
    }

    //------------------------------------------------------------------------------------
    //-------------------------------- GETTERS Y SETTERS  --------------------------------
    //------------------------------------------------------------------------------------


    public String getUvin() {
        return uvin;
    }

    public void setUvin(String uvin) {
        this.uvin = uvin;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getnNumber() {
        return nNumber;
    }

    public void setnNumber(String nNumber) {
        this.nNumber = nNumber;
    }

    public String getFaaNumber() {
        return faaNumber;
    }

    public void setFaaNumber(String faaNumber) {
        this.faaNumber = faaNumber;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public EnumVehicleClass getVehicleClass() {
        return vehicleClass;
    }

    public void setVehicleClass(EnumVehicleClass vehicleClass) {
        this.vehicleClass = vehicleClass;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
