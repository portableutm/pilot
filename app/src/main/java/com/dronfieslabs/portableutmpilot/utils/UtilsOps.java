package com.dronfieslabs.portableutmpilot.utils;

import android.location.Location;
import android.view.View;
import android.view.Window;

public class UtilsOps {

    public static View setFullscreenModeFlags(Window window){
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        return decorView;
    }

    public static float getLocationDistanceInMeters(double fromLat, double fromLong,
                                                    double toLat, double toLong) {
        Location fromLocation = new Location("");
        fromLocation.setLatitude(fromLat);
        fromLocation.setLongitude(fromLong);

        Location toLocation = new Location("");
        toLocation.setLatitude(toLat);
        toLocation.setLongitude(toLong);

        return toLocation.distanceTo(fromLocation);
    }
}
