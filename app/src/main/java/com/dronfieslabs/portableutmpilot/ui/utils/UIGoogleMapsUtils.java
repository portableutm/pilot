package com.dronfieslabs.portableutmpilot.ui.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class UIGoogleMapsUtils {

    public static void CenterMapOnDeviceLocation(Context context, final GoogleMap map) {
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task taskLocation = fusedLocationProviderClient.getLastLocation();
        taskLocation.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    final Location currentLocation = (Location)task.getResult();
                    if(currentLocation != null){
                        float zoom = 15f;
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                zoom
                        ));
                    }
                }
            }
        });
    }

    public static Bitmap GetMarkerBitmap(int[] rgbColor) {
        int circleRadius = 30;
        Bitmap bitmap = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(rgbColor[0], rgbColor[1], rgbColor[2]));
        canvas.drawCircle(circleRadius, circleRadius, circleRadius, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(circleRadius, circleRadius, circleRadius - 3, paint);
        return bitmap;
    }
}
