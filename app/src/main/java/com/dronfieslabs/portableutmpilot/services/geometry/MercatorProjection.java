package com.dronfieslabs.portableutmpilot.services.geometry;

// implementation obtained from the following source:
// https://www.baeldung.com/java-convert-latitude-longitude
public class MercatorProjection {

    final static double EARTH_RADIUS = 6378137.0;

    //--------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------
    //----------------------------------------- PUBLIC METHODS -----------------------------------------
    //--------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------

    public static double convertLngToX(double longitude) {
        return round(Math.toRadians(longitude) * EARTH_RADIUS);
    }

    public static double convertLatToY(double latitude) {
        return round(Math.log(Math.tan(Math.PI / 4d + Math.toRadians(latitude) / 2d)) * EARTH_RADIUS);
    }

    public static double convertXToLng(double x){
        return round(Math.toDegrees(x/EARTH_RADIUS));
    }

    public static double convertYToLat(double y){
        return round(Math.toDegrees(( 4d * Math.atan(Math.exp(y/EARTH_RADIUS)) - Math.PI)/2d));
    }

    //--------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------
    //---------------------------------------- PRIVATE METHODS  ----------------------------------------
    //--------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------

    private static double round(double value){
        double roundingFactor = 1000000d;
        return Math.round(value*roundingFactor)/roundingFactor;
    }
}
