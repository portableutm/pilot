package com.dronfieslabs.portableutmpilot.ui.utils;

import com.dronfies.portableutmandroidclienttest.entities.Operation;
import com.dronfieslabs.portableutmpilot.services.geometry.GeometryUtils;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.services.geometry.Segment;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;
import com.google.android.gms.maps.model.LatLng;

public class Utils {

    public static double getMinDistanceBetweenPointAndVolumeSides(double lat, double lng, double alt, Polygon polygon, double volumeMaxAlt){
        Point point = Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat));
        double min = Double.MAX_VALUE;
        for(Segment side : polygon.getSides()){
            min = Math.min(min, GeometryUtils.distance(point, side));
        }
        // convert mercator distance to meters
        Point point2 = Point.newPoint(point.getX(), point.getY() + min);
        LatLng point2LatLng = new LatLng(MercatorProjection.convertYToLat(point2.getY()), MercatorProjection.convertXToLng(point2.getX()));
        double minDistanceToSides = UtilsOps.getLocationDistanceInMeters(lat, lng, point2LatLng.latitude, point2LatLng.longitude);
        double distanceToCeil = Math.abs(volumeMaxAlt - alt);
        return Math.min(minDistanceToSides, distanceToCeil);
    }

    public static boolean pointIsInsideOperationVolume(double lat, double lng, double alt, Polygon operationPolygonMercator, double operationMaxAltitude){
        Point point = Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat));
        return GeometryUtils.isInside(operationPolygonMercator, point) && alt <= operationMaxAltitude;
    }
}
