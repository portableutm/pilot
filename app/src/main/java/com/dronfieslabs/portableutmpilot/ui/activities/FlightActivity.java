package com.dronfieslabs.portableutmpilot.ui.activities;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJIPolygonOptions;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelper;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelperObserver;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.RestrictedFlightVolume;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.services.geometry.GeometryUtils;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.services.geometry.Segment;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGoogleMapsUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.ux.widget.FPVOverlayWidget;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;

public class FlightActivity extends AppCompatActivity implements DJISDKHelperObserver {

    // consts
    private DecimalFormat dfInteger = new DecimalFormat("###,##0");
    private DecimalFormat dfDecimal = new DecimalFormat("###,##0.0");
    private enum EnumDronePosition{
        INSIDE_OPERATION_FAR_FROM_THE_EDGE,
        INSIDE_OPERATION_CLOSE_TO_THE_EDGE,
        OUTSIDE_OPERATION
    }

    // state
    private String mOperationId = null;
    private int mOperationMaxAltitude = 0;
    private List<LatLng> mOperationPolygon = null;
    private Polygon mOperationPolygonMercator = null;
    private Aircraft mAircraftConnected = null;
    private int mDistanceBetweenAircraftAndHome = -1;
    private int mAircraftAltitude = -1;
    private double mAircraftHorizontalSpeed = -1;
    private double mAircraftVerticalSpeed = -1;
    // we try to send the position to the UTM each 3 seconds, so we need this variable to know when was the last time we sent it
    private long mLastPositionSentTimestamp = -1;
    // alarm
    private EnumDronePosition mDroneLastPosition;
    private EnumDronePosition mDroneCurrentPosition;
    private ValueAnimator mValueAnimatorAlarm;
    private MediaPlayer mMediaPlayerBeepBuffer;
    private MediaPlayer mMediaPlayerBeepOutside;

    // views
    private RelativeLayout mRelativeLayoutFullscreenMapFPV;
    private RelativeLayout mRelativeLayoutSmallMapFPV;
    private FPVWidget mFPVWidget;
    private FPVOverlayWidget mFPVOverlayWidget;
    private MapWidget mMapWidget;
    private TextView mTextViewDistance;
    private TextView mTextViewAltitude;
    private TextView mTextViewHorizontalSpeed;
    private TextView mTextViewVerticalSpeed;
    private RelativeLayout mRelativeLayoutAlarm;
    private Button mButtonDismissAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                    Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                    Manifest.permission.READ_PHONE_STATE,
                }
                , 1);
        }
        setContentView(R.layout.activity_flight);

        // initialize state
        try{
            mOperationId = getIntent().getStringExtra("OPERATION_ID");
            mOperationMaxAltitude = getIntent().getIntExtra("OPERATION_MAX_ALTITUDE", 0);
            mOperationId = getIntent().getStringExtra("OPERATION_ID");
            mOperationPolygon = new ArrayList<>();
            List<Point> listVertices = new ArrayList<>();
            for(String str : getIntent().getStringArrayExtra("OPERATION_POLYGON")){
                double lat = Double.parseDouble(str.split(";")[0]);
                double lng = Double.parseDouble(str.split(";")[1]);
                mOperationPolygon.add(new LatLng(lat, lng));
                listVertices.add(Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat)));
            }
            mOperationPolygonMercator = Polygon.newPolygon(listVertices);
        }catch(Exception ex){}
        mValueAnimatorAlarm = new ValueAnimator();
        mValueAnimatorAlarm.setDuration(1000);
        mValueAnimatorAlarm.setEvaluator(new ArgbEvaluator());
        mValueAnimatorAlarm.setIntValues(0, 100, 0);
        mValueAnimatorAlarm.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(mDroneCurrentPosition == EnumDronePosition.OUTSIDE_OPERATION){
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 0,0));
                }else{
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 255,0));
                }
            }
        });
        mValueAnimatorAlarm.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimatorAlarm.setRepeatMode(ValueAnimator.RESTART);
        mMediaPlayerBeepBuffer = MediaPlayer.create(this, R.raw.beep);
        mMediaPlayerBeepBuffer.setLooping(true);
        mMediaPlayerBeepOutside = MediaPlayer.create(this, R.raw.beep2);
        mMediaPlayerBeepOutside.setLooping(true);

        // views binding
        mRelativeLayoutFullscreenMapFPV = findViewById(R.id.relative_layout_fullscreen_map_fpv);
        mRelativeLayoutSmallMapFPV = findViewById(R.id.relative_layout_small_map_fpv);
        mFPVWidget = findViewById(R.id.fpv_widget);
        mFPVOverlayWidget = findViewById(R.id.fpv_overlay_widget);
        mMapWidget = findViewById(R.id.mapWidget);
        mMapWidget.initGoogleMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull final DJIMap map) {
                onMapReady2(map);
            }
        });
        mMapWidget.onCreate(savedInstanceState);
        mTextViewDistance = findViewById(R.id.text_view_distance);
        mTextViewAltitude = findViewById(R.id.text_view_altitude);
        mTextViewHorizontalSpeed = findViewById(R.id.text_view_horizontal_speed);
        mTextViewVerticalSpeed = findViewById(R.id.text_view_vertical_speed);
        ((ImageButton)findViewById(R.id.buttonFullscreen)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickFullscreen();
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

        // execute onProductConnected, in case the product was connected before the user enter to the activity
        onProductConnected();
    }

    @Override
    protected void onResume(){
        mMapWidget.onResume();
        setFullscreenAndScreenAlwaysOn();
        setFlightControllerStateCallback();
        mDroneLastPosition = null;
        // subscribe to productConnect and productDisconnect methods
        DJISDKHelper.getInstance().addObserver(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mMapWidget.onPause();
        // disubscribe to productConnect and productDisconnect methods
        DJISDKHelper.getInstance().removeObserver(this);
        try{
            mAircraftConnected.getFlightController().setStateCallback(null);
        }catch(Exception ex){}
        // if playing, stop both beeps
        stopBeep(mMediaPlayerBeepBuffer);
        stopBeep(mMediaPlayerBeepOutside);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapWidget.onDestroy();
        try{
            mAircraftConnected.getFlightController().setStateCallback(null);
        }catch(Exception ex){}
        // if playing, stop both beeps
        stopBeep(mMediaPlayerBeepBuffer);
        stopBeep(mMediaPlayerBeepOutside);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapWidget.onLowMemory();
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- EVENT HANDLERS  ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onMapReady2(@NonNull final DJIMap djiMap){
        djiMap.setMapType(DJIMap.MapType.SATELLITE);

        // draw restricted flight volumes
        final DronfiesUssServices dronfiesUssServices = DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(FlightActivity.this));
        if(dronfiesUssServices == null){
            // if we couldn't connect to the utm, we didn't draw the rfvs
            return;
        }
        if(!dronfiesUssServices.isAuthenticated()){
            String username = SharedPreferencesUtils.getUsername(this);
            String password = SharedPreferencesUtils.getPassword(this);
            dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
                @Override
                public void onResponse(String s, String errorMessage) {
                    if(errorMessage != null){
                        // if we couldn't connect to the utm, we didn't draw the rfvs
                        return;
                    }
                    drawRFVs(dronfiesUssServices, djiMap);
                }
            });
        }else{
            drawRFVs(dronfiesUssServices, djiMap);
        }

        // if operation received, draw polygon
        if(mOperationId != null && mOperationPolygon != null && mOperationPolygon.size() > 2){
            DJIPolygonOptions polygonOptions = new DJIPolygonOptions();
            polygonOptions.strokeWidth(5);
            polygonOptions.strokeColor(Color.rgb(255, 162, 0));
            polygonOptions.fillColor(Color.argb(64, 255, 162, 0));
            polygonOptions.zIndex(-2);
            for(LatLng latLng : mOperationPolygon){
                polygonOptions.add(new DJILatLng(latLng.latitude, latLng.longitude));
            }
            djiMap.addPolygon(polygonOptions);
        }
    }

    private void onClickFullscreen(){
        if(mFPVWidget.getParent().equals(mRelativeLayoutFullscreenMapFPV)){
            // it means that we have to minimize the fpv and maximize the map
            mRelativeLayoutFullscreenMapFPV.removeView(mFPVWidget);
            mRelativeLayoutFullscreenMapFPV.removeView(mFPVOverlayWidget);
            mRelativeLayoutSmallMapFPV.removeView(mMapWidget);
            mRelativeLayoutFullscreenMapFPV.addView(mMapWidget, 0);
            mRelativeLayoutSmallMapFPV.addView(mFPVWidget, 0);
            mRelativeLayoutSmallMapFPV.addView(mFPVOverlayWidget, 1);
        }else{
            // it means that we have to minimize the map and maximize the fpv
            mRelativeLayoutFullscreenMapFPV.removeView(mMapWidget);
            mRelativeLayoutSmallMapFPV.removeView(mFPVWidget);
            mRelativeLayoutSmallMapFPV.removeView(mFPVOverlayWidget);
            // for some reason when we maximize the fpvwidget, we have to send layoutParams because if we dont, the size of the fpvWidget doesn't change
            mRelativeLayoutFullscreenMapFPV.addView(mFPVWidget, 0, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRelativeLayoutFullscreenMapFPV.addView(mFPVOverlayWidget, 1, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRelativeLayoutSmallMapFPV.addView(mMapWidget, 0);
        }
    }

    private void onClickDismissAlarm(){
        stopBeep(mMediaPlayerBeepBuffer);
        stopValueAnimatorAlarm();
        changeDismissButtonVisibility(View.INVISIBLE);
    }

    @Override
    public void onProductConnected() {
        setFlightControllerStateCallback();
    }

    @Override
    public void onProductDisconnected() {
        mAircraftConnected = null;
        mAircraftAltitude = -1;
        mAircraftHorizontalSpeed = -1;
        mAircraftVerticalSpeed = -1;
        updateScreenValues();
    }

    @Override
    public void onSerialObtained() {}

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void setFullscreenAndScreenAlwaysOn() {
        View decorView = UtilsOps.setFullscreenModeFlags(getWindow());
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                UtilsOps.setFullscreenModeFlags(getWindow());
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updateScreenValues(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mAircraftConnected == null){
                    mTextViewDistance.setText("-");
                    mTextViewAltitude.setText("-");
                    mTextViewHorizontalSpeed.setText("-");
                    mTextViewVerticalSpeed.setText("-");
                }else{
                    mTextViewDistance.setText(dfInteger.format(mDistanceBetweenAircraftAndHome));
                    mTextViewAltitude.setText(dfInteger.format(mAircraftAltitude));
                    mTextViewHorizontalSpeed.setText(dfDecimal.format(mAircraftHorizontalSpeed));
                    mTextViewVerticalSpeed.setText(dfDecimal.format(mAircraftVerticalSpeed));
                }
            }
        });
    }

    private double getMinDistanceBetweenPointAndVolumeSides(double lat, double lng, double alt, Polygon polygon, double volumeMaxAlt){
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

    private boolean droneIsInsidePolygon(double lat, double lng, double alt){
        Point point = Point.newPoint(MercatorProjection.convertLngToX(lng), MercatorProjection.convertLatToY(lat));
        return GeometryUtils.isInside(mOperationPolygonMercator, point) && alt <= mOperationMaxAltitude;
    }


    private void setFlightControllerStateCallback(){
        mAircraftConnected = DJISDKHelper.getAircraftInstance();
        if(mAircraftConnected == null){
            return;
        }
        final FlightController flightController = mAircraftConnected.getFlightController();
        if(flightController == null){
            return;
        }
        flightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)){
                    // if activity is not resumed, we do not do anything
                    return;
                }
                mDistanceBetweenAircraftAndHome = Math.round(UtilsOps.getLocationDistanceInMeters(
                        flightControllerState.getHomeLocation().getLatitude(),
                        flightControllerState.getHomeLocation().getLongitude(),
                        flightControllerState.getAircraftLocation().getLatitude(),
                        flightControllerState.getAircraftLocation().getLongitude()
                ));
                mAircraftAltitude = Math.round(flightControllerState.getAircraftLocation().getAltitude());
                mAircraftHorizontalSpeed = Math.sqrt(Math.pow(flightControllerState.getVelocityX(), 2) + Math.pow(flightControllerState.getVelocityY(), 2));
                mAircraftVerticalSpeed = Math.abs(flightControllerState.getVelocityZ());
                updateScreenValues();

                // send position to the utm
                if(mOperationId != null){
                    GPSSignalLevel gpsSignalLevel = flightControllerState.getGPSSignalLevel();
                    if(flightControllerState.getSatelliteCount() < 6 || (gpsSignalLevel != GPSSignalLevel.LEVEL_3 && gpsSignalLevel != GPSSignalLevel.LEVEL_4 && gpsSignalLevel != GPSSignalLevel.LEVEL_5)){
                        // if we dont have good gps signal, we dont execute the flight controller state listener
                        return;
                    }
                    double lat = flightControllerState.getAircraftLocation().getLatitude();
                    double lon = flightControllerState.getAircraftLocation().getLongitude();
                    double alt = flightControllerState.getAircraftLocation().getAltitude();
                    if(alt < 0) alt = 0;
                    double heading = flightControllerState.getAircraftHeadDirection();

                    long now = new Date().getTime();
                    if((now - mLastPositionSentTimestamp)/1000 >= 3){
                        mLastPositionSentTimestamp = now;
                        try{
                            DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(FlightActivity.this)).sendPosition(lon, lat, alt, heading, mOperationId, new ICompletitionCallback<String>() {
                                @Override
                                public void onResponse(String s, String errorMessage) {}
                            });
                        }catch(Exception ex){}
                    }



                    if(mOperationPolygon != null && mOperationPolygon.size() > 2) {
                        // calculate drone current position
                        if(droneIsInsidePolygon(lat, lon, alt)){
                            // calculate distance between dron and the polygon sides
                            final double minDistanceBetweenDronePositionAnPolygonSidesInMeters = getMinDistanceBetweenPointAndVolumeSides(lat, lon, alt, mOperationPolygonMercator, mOperationMaxAltitude);
                            if (minDistanceBetweenDronePositionAnPolygonSidesInMeters < 10) {
                                mDroneCurrentPosition = EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE;
                            }else{
                                mDroneCurrentPosition = EnumDronePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE;
                            }
                        }else{
                            mDroneCurrentPosition = EnumDronePosition.OUTSIDE_OPERATION;
                        }

                        // if mDroneLastPosition is null, it means it is the first time this method is executed
                        // if mDroneLastPosition is null and mDroneCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
                        // we have to play beep buffer and show dismiss button
                        if(mDroneLastPosition == null && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
                            playBeep(mMediaPlayerBeepBuffer);
                            startValueAnimatorAlarm();
                            changeDismissButtonVisibility(View.VISIBLE);
                        }
                        // if mDroneLastPosition is null and mDroneCurrentPosition is OUTSIDE_OPERATION
                        // we have to play beep outside
                        else if(mDroneLastPosition == null && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
                            playBeep(mMediaPlayerBeepOutside);
                            startValueAnimatorAlarm();
                        }
                        // if mDroneLastPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE and mDroneCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
                        // we have to play beep buffer and show dismiss button
                        else if(mDroneLastPosition == EnumDronePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
                            playBeep(mMediaPlayerBeepBuffer);
                            startValueAnimatorAlarm();
                            changeDismissButtonVisibility(View.VISIBLE);
                        }
                        // if mDroneLastPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE and mDroneCurrentPosition is OUTSIDE_OPERATION
                        // we have to play beep outside
                        else if(mDroneLastPosition == EnumDronePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE && mDroneCurrentPosition == EnumDronePosition.OUTSIDE_OPERATION){
                            playBeep(mMediaPlayerBeepOutside);
                            startValueAnimatorAlarm();
                        }
                        // if mDroneLastPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE and mDroneCurrentPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE
                        // we have to stop beep buffer and hide dismiss button
                        else if(mDroneLastPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE){
                            stopBeep(mMediaPlayerBeepBuffer);
                            stopValueAnimatorAlarm();
                            changeDismissButtonVisibility(View.INVISIBLE);
                        }
                        // if mDroneLastPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE and mDroneCurrentPosition is OUTSIDE_OPERATION
                        // we have to stop beep buffer, play beep outside and hide dismiss button
                        else if(mDroneLastPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE && mDroneCurrentPosition == EnumDronePosition.OUTSIDE_OPERATION){
                            stopBeep(mMediaPlayerBeepBuffer);
                            playBeep(mMediaPlayerBeepOutside);
                            startValueAnimatorAlarm();
                            changeDismissButtonVisibility(View.INVISIBLE);
                        }
                        // if mDroneLastPosition is OUTSIDE_OPERATION and mDroneCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
                        // we have to stop beep outside, play beep buffer and show dismiss button
                        else if(mDroneLastPosition == EnumDronePosition.OUTSIDE_OPERATION && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
                            stopBeep(mMediaPlayerBeepOutside);
                            playBeep(mMediaPlayerBeepBuffer);
                            startValueAnimatorAlarm();
                            changeDismissButtonVisibility(View.VISIBLE);
                        }
                        // if mDroneLastPosition is OUTSIDE_OPERATION and mDroneCurrentPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE
                        // we have to stop beep outside
                        else if(mDroneLastPosition == EnumDronePosition.OUTSIDE_OPERATION && mDroneCurrentPosition == EnumDronePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE){
                            stopBeep(mMediaPlayerBeepOutside);
                            stopValueAnimatorAlarm();
                        }

                        // update mDroneLastPosition
                        mDroneLastPosition = mDroneCurrentPosition;
                    }
                }
            }
        });
    }

    private void changeDismissButtonVisibility(final int visibility){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonDismissAlarm.setVisibility(visibility);
            }
        });
    }

    private void playBeep(final MediaPlayer mediaPlayer){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }

    private void stopBeep(final MediaPlayer mediaPlayer){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    private void startValueAnimatorAlarm(){
        if(!mValueAnimatorAlarm.isRunning()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mValueAnimatorAlarm.start();
                }
            });
        }
    }

    private void stopValueAnimatorAlarm(){
        if(mValueAnimatorAlarm.isRunning()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mValueAnimatorAlarm.cancel();
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb(0, 0, 0,0));
                }
            });
        }
    }

    private void drawRFVs(final DronfiesUssServices dronfiesUssServices, final DJIMap djiMap){
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<RestrictedFlightVolume> listRFVs = null;
                try {
                    listRFVs = dronfiesUssServices.getRestrictedFlightVolumes();
                } catch (Exception e) {
                    // if we couldn't get the rfvs, we do not draw them
                    return;
                }
                for(final RestrictedFlightVolume rfv : listRFVs){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            djiMap.addPolygon(
                                new DJIPolygonOptions()
                                    .addAll(convertToDJIPolygon(rfv.getPolygon()))
                                    .fillColor(Color.argb(64, 255, 0, 0))
                                    .strokeColor(Color.rgb(255, 0, 0))
                                    .strokeWidth(3f)
                            );
                        }
                    });
                }
            }
        }).start();
    }

    private List<DJILatLng> convertToDJIPolygon(List<LatLng> polygon){
        List<DJILatLng> ret = new ArrayList<>();
        for(LatLng latLng : polygon){
            ret.add(new DJILatLng(latLng.latitude, latLng.longitude));
        }
        return ret;
    }

}