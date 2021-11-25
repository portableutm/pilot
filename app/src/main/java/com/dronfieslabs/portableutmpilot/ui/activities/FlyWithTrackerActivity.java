package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelper;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.ui.utils.Alarm;
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
import java.util.List;

public class FlyWithTrackerActivity extends AppCompatActivity {

    private enum EnumTrackerStatus {DISCONNECTED, CONNECTED, SENDING_POSITION}

    // state
    private EnumTrackerStatus mTrackerStatus;
    private int mOperationMaxAltitude;
    private List<LatLng> mOperationPolygon;
    private Polygon mOperationPolygonMercator;

    // views
    private GoogleMap mMap;
    private Marker mMarkerTracker;
    private RelativeLayout mRelativeLayoutAlarm;
    private Button mButtonDismissAlarm;

    // other variables
    private Alarm mAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fly_with_tracker);

        // init state
        mTrackerStatus = EnumTrackerStatus.DISCONNECTED;
        mOperationMaxAltitude = getIntent().getIntExtra(Constants.OPERATION_MAX_ALTITUDE_KEY, 0);
        mOperationPolygon = new ArrayList<>();
        List<Point> listVertices = new ArrayList<>();
        for(String str : getIntent().getStringArrayExtra(Constants.OPERATION_POLYGON_KEY)){
            double lat = Double.parseDouble(str.split(";")[0]);
            double lng = Double.parseDouble(str.split(";")[1]);
            mOperationPolygon.add(new LatLng(lat, lng));
            listVertices.add(Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat)));
        }
        mOperationPolygonMercator = Polygon.newPolygon(listVertices);

        // views binding
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                onMapReady2(googleMap);
            }
        });
        mRelativeLayoutAlarm = findViewById(R.id.relative_layout_alarm);
        mButtonDismissAlarm = findViewById(R.id.button_dismiss_alarm);
        mButtonDismissAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickDismissAlarm();
            }
        });
        ((Button)findViewById(R.id.button_right)).setOnClickListener(view -> onClickDirectionButton(0));
        ((Button)findViewById(R.id.button_down)).setOnClickListener(view -> onClickDirectionButton(1));
        ((Button)findViewById(R.id.button_left)).setOnClickListener(view -> onClickDirectionButton(2));
        ((Button)findViewById(R.id.button_up)).setOnClickListener(view -> onClickDirectionButton(3));

        // alarm
        mAlarm = new Alarm(this, mRelativeLayoutAlarm, mButtonDismissAlarm, mOperationPolygonMercator, mOperationMaxAltitude);
    }

    @Override
    protected void onPause() {
        // if playing, stop both beeps
        mAlarm.stopBeep();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // if playing, stop both beeps
        mAlarm.stopBeep();
        super.onDestroy();
    }


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickDismissAlarm(){
        mAlarm.dismiss();
    }

    private void onClickDirectionButton(int direction){
        switch (direction){
            case 0:
                mMarkerTracker.setPosition(new LatLng(mMarkerTracker.getPosition().latitude, mMarkerTracker.getPosition().longitude + 0.0001));
                break;
            case 1:
                mMarkerTracker.setPosition(new LatLng(mMarkerTracker.getPosition().latitude - 0.0001, mMarkerTracker.getPosition().longitude));
                break;
            case 2:
                mMarkerTracker.setPosition(new LatLng(mMarkerTracker.getPosition().latitude, mMarkerTracker.getPosition().longitude - 0.0001));
                break;
            case 3:
                mMarkerTracker.setPosition(new LatLng(mMarkerTracker.getPosition().latitude + 0.0001, mMarkerTracker.getPosition().longitude));
                break;
            default:
                UIGenericUtils.ShowToast(this, "UNKOWN");
        }

        double trackerLat = mMarkerTracker.getPosition().latitude;
        double trackerLng = mMarkerTracker.getPosition().longitude;
        double trackerAlt = 50;

        mAlarm.updateVehiclePosition(trackerLat, trackerLng, trackerAlt);
    }

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

        // draw the tracker
        mMarkerTracker = mMap.addMarker(new MarkerOptions().position(getPolygonCenter(mOperationPolygon)));
    }

    private LatLngBounds getPolygonLatLngBounds(final List<LatLng> polygon) {
        final LatLngBounds.Builder centerBuilder = LatLngBounds.builder();
        for (LatLng point : polygon) {
            centerBuilder.include(point);
        }
        return centerBuilder.build();
    }

    private LatLng getPolygonCenter(List<LatLng> polygon){
        double totalLat = 0;
        double totalLng = 0;
        for(LatLng vertex : polygon){
            totalLat += vertex.latitude;
            totalLng += vertex.longitude;
        }
        return new LatLng(totalLat/polygon.size(), totalLng/polygon.size());
    }
}