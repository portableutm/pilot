package com.dronfieslabs.portableutmpilot.ui.activities;

import static com.dronfieslabs.portableutmpilot.utils.UtilsOps.getLocationDistanceInMeters;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.NoAuthenticatedException;
import com.dronfies.portableutmandroidclienttest.TrackerPosition;
import com.dronfies.portableutmandroidclienttest.entities.IGenericCallback;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.ui.utils.Alarm;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FlyWithTrackerActivity extends AppCompatActivity {

    private enum EnumTrackerStatus {DISCONNECTED, CONNECTED, SENDING_POSITION}

    // state
    private String mOperationId;
    private int mOperationMaxAltitude;
    private List<LatLng> mOperationPolygon;
    private Polygon mOperationPolygonMercator;
    private String mTrackerPositionRef;

    // views
    private GoogleMap mMap;
    private Marker mMarkerTracker = null;
    private RelativeLayout mRelativeLayoutAlarm;
    private Button mButtonDismissAlarm;

    private TextView mAltitude;
    private TextView mHeading;
    private TextView mVSpeed;
    private TextView mHSpeed;

    private Date lastTime = null;

    private double calculatedVSpeed = 0;
    private double calculatedHSpeed = 0;


    // other variables
    private Alarm mAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fly_with_tracker);

        // init state
        mOperationId = getIntent().getStringExtra(Constants.OPERATION_ID_KEY);
        mOperationMaxAltitude = getIntent().getIntExtra(Constants.OPERATION_MAX_ALTITUDE_KEY, 0);
        mOperationPolygon = new ArrayList<>();
        List<Point> listVertices = new ArrayList<>();
        for (String str : getIntent().getStringArrayExtra(Constants.OPERATION_POLYGON_KEY)) {
            double lat = Double.parseDouble(str.split(";")[0]);
            double lng = Double.parseDouble(str.split(";")[1]);
            mOperationPolygon.add(new LatLng(lat, lng));
            listVertices.add(Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat)));
        }
        mOperationPolygonMercator = Polygon.newPolygon(listVertices);

        // views binding
        mAltitude = findViewById(R.id.tv_altitude_value);
        mHeading = findViewById(R.id.tv_heading_value);
        mVSpeed = findViewById(R.id.tv_vspeed_value);
        mHSpeed = findViewById(R.id.tv_hspeed_value);

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

        startReceivingTrackerPositionUpdates();

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
        disconnectSocket();
        super.onDestroy();
    }


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickDismissAlarm() {
        mAlarm.dismiss();
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onMapReady2(GoogleMap googleMap) {
        mMap = googleMap;

        // we draw the operation
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeWidth(5);
        polygonOptions.strokeColor(Color.rgb(255, 162, 0));
        polygonOptions.fillColor(Color.argb(64, 255, 162, 0));
        polygonOptions.zIndex(-2);
        for (LatLng latLng : mOperationPolygon) {
            polygonOptions.add(latLng);
        }
        mMap.addPolygon(polygonOptions);

        // center the map on the operation
        mMap.setOnMapLoadedCallback(() -> {
            final LatLngBounds latLngBounds = getPolygonLatLngBounds(mOperationPolygon);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

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

    private void startReceivingTrackerPositionUpdates(){
        final DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(FlyWithTrackerActivity.this));

        try {
            lastTime = new Date();
            mTrackerPositionRef = dronfiesUssServices.connectToTrackerPositionUpdates(mOperationId, new IGenericCallback<TrackerPosition>() {
                @Override
                public void onCallbackExecution(TrackerPosition trackerPosition, String errorMessage) {
                    if(errorMessage != null){
                        runOnUiThread(() -> UIGenericUtils.ShowToast(FlyWithTrackerActivity.this, errorMessage));
                        return;
                    }
                    runOnUiThread(() -> {
                        if (mMarkerTracker == null){mMarkerTracker = mMap.addMarker(new MarkerOptions().position(getPolygonCenter(mOperationPolygon)).flat(true));}
                        mMarkerTracker.setIcon(BitmapFromVector(FlyWithTrackerActivity.this, R.drawable.ic_dronetop));
                        mMarkerTracker.setRotation((float) trackerPosition.getHeading());

                        //Speed has to be done first before overwriting values
                        mVSpeed.setText(new DecimalFormat("#.##").format(calculateVSpeed(Double.parseDouble((String) mAltitude.getText()),trackerPosition.getAltitude(), lastTime,trackerPosition.getTime_sent())));
                        mHSpeed.setText(new DecimalFormat("#.##").format(calculateHSpeed(mMarkerTracker.getPosition(),trackerPosition, lastTime,trackerPosition.getTime_sent())));
                        mHeading.setText(String.valueOf(trackerPosition.getHeading()));
                        mMarkerTracker.setPosition(new LatLng(trackerPosition.getLatitude(), trackerPosition.getLongitude()));
//                        mHeading.setText(trackerPosition.getHeading());
                        mAltitude.setText(String.valueOf(trackerPosition.getAltitude()));

                        mAlarm.updateVehiclePosition(trackerPosition.getLatitude(), trackerPosition.getLongitude(), trackerPosition.getAltitude());
                    });
                }
            });
        } catch (NoAuthenticatedException e) {
            UIGenericUtils.ShowAlert(this, "Error", String.format("There was an error trying to connect to the backend. Please, exit this view and enter again [Error: %s]", e.getMessage()));
        }
    }


    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);
        Bitmap newB = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(newB);
    }

    private void disconnectSocket(){
        final DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(FlyWithTrackerActivity.this));
        dronfiesUssServices.disconnectFromTrackerPositionUpdates(mTrackerPositionRef);
    }

    //TODO: this 2 speed functions
    private double calculateVSpeed(double lastAlt, double actualAlt, Date last_time_sent, Date actual_time_sent){
        double res = 0;
        //Get how many seconds has been between 2 positions
        long diffAux = actual_time_sent.getTime() - last_time_sent.getTime();
        double seconds = TimeUnit.MILLISECONDS.toSeconds(diffAux);

        //get difference in altitud and apply linear rule
        double difference = Math.abs(actualAlt - lastAlt);
        res = difference / seconds;

        return res;


    }

    private double calculateHSpeed(LatLng lastPoint, TrackerPosition actualPosition, Date last_time_sent, Date actual_time_sent){
        double res = 0;
        long diff = actual_time_sent.getTime() - last_time_sent.getTime();
        double seconds = TimeUnit.MILLISECONDS.toSeconds(diff);

        float distance = UtilsOps.getLocationDistanceInMeters(lastPoint.latitude,lastPoint.longitude,actualPosition.getLatitude(),actualPosition.getLongitude());
        res = distance / seconds;
        return res;


    }
}