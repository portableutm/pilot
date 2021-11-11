package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlyWithTrackerActivity extends AppCompatActivity {

    private enum EnumTrackerStatus {DISCONNECTED, CONNECTED, SENDING_POSITION}

    private List<LatLng> TRACKER_POSITIONS = Arrays.asList(
            new LatLng(-34.911982, -56.159723),
            new LatLng(-34.912039, -56.159430),
            new LatLng(-34.911835, -56.159377)
    );

    // state
    private EnumTrackerStatus mTrackerStatus;
    private List<LatLng> mOperationPolygon;

    // views
    private GoogleMap mMap;
    private Marker mMarkerTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fly_with_tracker);

        // init state
        mTrackerStatus = EnumTrackerStatus.DISCONNECTED;
        mOperationPolygon = new ArrayList<>();
        List<Point> listVertices = new ArrayList<>();
        for(String str : getIntent().getStringArrayExtra(Constants.OPERATION_POLYGON_KEY)){
            double lat = Double.parseDouble(str.split(";")[0]);
            double lng = Double.parseDouble(str.split(";")[1]);
            mOperationPolygon.add(new LatLng(lat, lng));
            listVertices.add(Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat)));
        }

        // views binding
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                onMapReady2(googleMap);
            }
        });
    }


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onMapReady2(GoogleMap googleMap){
        mMap = googleMap;

        // we draw the operation
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeWidth(5);
        polygonOptions.strokeColor(Color.rgb(255, 162, 0));
        polygonOptions.fillColor(Color.argb(64, 255, 162, 0));
        polygonOptions.zIndex(-2);
        for(LatLng latLng : mOperationPolygon){
            polygonOptions.add(latLng);
        }
        mMap.addPolygon(polygonOptions);

        // center the map on the operation
        mMap.setOnMapLoadedCallback(() -> {
            final LatLngBounds latLngBounds = getPolygonLatLngBounds(mOperationPolygon);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        });
    }

    private LatLngBounds getPolygonLatLngBounds(final List<LatLng> polygon) {
        final LatLngBounds.Builder centerBuilder = LatLngBounds.builder();
        for (LatLng point : polygon) {
            centerBuilder.include(point);
        }
        return centerBuilder.build();
    }

    private void startShowingTrackerPosition(){
        new Thread(() -> {
            final int[] index = {0};
            while(true){
                runOnUiThread(() -> {
                    if(mMarkerTracker != null) mMarkerTracker.remove();
                    MarkerOptions markerOptions = new MarkerOptions().position(TRACKER_POSITIONS.get(index[0]));
                    mMarkerTracker = mMap.addMarker(markerOptions);
                });
                try{
                    Thread.sleep(5000);
                }catch (Exception ex){}
                index[0] = (index[0]+1)%TRACKER_POSITIONS.size();
            }
        }).start();
    }
}