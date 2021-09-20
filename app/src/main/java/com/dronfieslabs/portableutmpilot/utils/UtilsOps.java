package com.dronfieslabs.portableutmpilot.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.BuildConfig;
import com.dronfieslabs.portableutmpilot.IGenericCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class UtilsOps {

    private static FusedLocationProviderClient fusedLocationClient;

    public static View setFullscreenModeFlags(Window window) {
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

    public static void getLocation(Context context, IGenericCallback<LatLng> callback) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        Task taskLocation = fusedLocationClient.getLastLocation();
        taskLocation.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    final Location currentLocation = (Location)task.getResult();
                    if(currentLocation != null){
                        LatLng res = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        callback.onResult(res,null);
                    }else{
                        callback.onResult(null,"Could not find location");
                    }
                }
            }
        });
    }

    public static DronfiesUssServices getDronfiesUssServices(String endpoint) {
        if(BuildConfig.BUILD_TYPE.equals("debug")){
            return DronfiesUssServices.getUnsafeInstanceDONOTUSE(endpoint);
        } else {
            return DronfiesUssServices.getInstance(endpoint);
        }
    }
}
