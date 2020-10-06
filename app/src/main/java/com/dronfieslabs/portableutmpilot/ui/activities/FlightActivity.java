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
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.services.geometry.GeometryUtils;
import com.dronfieslabs.portableutmpilot.services.geometry.MercatorProjection;
import com.dronfieslabs.portableutmpilot.services.geometry.Point;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.services.geometry.Segment;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;
import com.google.android.gms.maps.model.LatLng;

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
    private ValueAnimator mValueAnimatorAlarm;
    private boolean mDroneInsideOperationPolygon = true;
    private Thread mThreadAlarmSound;
    private MediaPlayer mMediaPlayerBeep;
    private MediaPlayer mMediaPlayerBeep2;

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
                if(mDroneInsideOperationPolygon){
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 255,0));
                }else{
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 0,0));
                }
            }
        });
        mValueAnimatorAlarm.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimatorAlarm.setRepeatMode(ValueAnimator.RESTART);
        mMediaPlayerBeep = MediaPlayer.create(this, R.raw.beep);
        mMediaPlayerBeep.setLooping(true);
        mMediaPlayerBeep2 = MediaPlayer.create(this, R.raw.beep2);
        mMediaPlayerBeep2.setLooping(true);

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

        // execute onProductConnected, in case the product was connected before the user enter to the activity
        onProductConnected();
    }

    @Override
    protected void onResume(){
        mMapWidget.onResume();
        setFullscreenAndScreenAlwaysOn();
        setFlightControllerStateCallback();
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
        ifPlayingPauseBothBeeps();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapWidget.onDestroy();
        try{
            mAircraftConnected.getFlightController().setStateCallback(null);
        }catch(Exception ex){}
        ifPlayingPauseBothBeeps();
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

    private void onMapReady2(@NonNull final DJIMap map){
        map.setMapType(DJIMap.MapType.SATELLITE);
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
            map.addPolygon(polygonOptions);
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

    /*private void startSound(){
        if(mDroneInsideOperationPolygon){
            if(mMediaPlayerBeep2.isPlaying()){
                mMedia
            }
        }else{

        }
        if(mMediaPlayerBeep.isPlaying()){
            return;
        }
        mMediaPlayerBeep.seekTo(0);
        mMediaPlayerBeep.start();
        if(mThreadAlarmSound != null){
            return;
        }
        mThreadAlarmSound = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ToneGenerator toneGenerator = new ToneGenerator(0,ToneGenerator.MAX_VOLUME);
                    while(true){
                        if(mDroneInsideOperationPolygon){
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 500);
                            Thread.sleep(1000);
                        }else{
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 250);
                            Thread.sleep(500);
                        }
                    }
                }catch (Exception ex){}
            }
        });
        mThreadAlarmSound.start();
    }*/

    /*private void stopSound(){
        if(!mMediaPlayerBeep.isPlaying()){
            return;
        }
        mMediaPlayerBeep.pause();
        if(mThreadAlarmSound != null){
            try{
                mThreadAlarmSound.interrupt();
                mThreadAlarmSound = null;
            }catch(Exception ex){
            }
        }
    }*/

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

                    // update mDroneInsideOperationPolygon variable
                    mDroneInsideOperationPolygon = droneIsInsidePolygon(lat, lon, alt);

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
                        // verify if the drone is inside the polygon
                        if (mDroneInsideOperationPolygon) {
                            ifPlayingPauseBeep2();
                            // calculate distance between dron and the polygon sides
                            final double minDistanceBetweenDronePositionAnPolygonSidesInMeters = getMinDistanceBetweenPointAndVolumeSides(lat, lon, alt, mOperationPolygonMercator, mOperationMaxAltitude);
                            if (minDistanceBetweenDronePositionAnPolygonSidesInMeters < 20) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!mValueAnimatorAlarm.isRunning()){
                                            mValueAnimatorAlarm.start();
                                        }
                                    }
                                });
                                ifNotPlayingStartBeep();
                            }else {
                                if(mValueAnimatorAlarm.isRunning()){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mValueAnimatorAlarm.cancel();
                                            mRelativeLayoutAlarm.setBackgroundColor(Color.argb(0, 0, 0,0));
                                        }
                                    });
                                }
                                ifPlayingPauseBothBeeps();
                            }
                        } else {
                            if(!mValueAnimatorAlarm.isRunning()){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mValueAnimatorAlarm.start();
                                    }
                                });
                            }
                            ifPlayingPauseBeep();
                            ifNotPlayingStartBeep2();
                        }
                    }
                }
            }
        });
    }

    private void ifPlayingPauseBeep2(){
        if(mMediaPlayerBeep2.isPlaying()) mMediaPlayerBeep2.pause();
    }

    private void ifPlayingPauseBeep(){
        if(mMediaPlayerBeep.isPlaying()) mMediaPlayerBeep.pause();
    }

    private void ifNotPlayingStartBeep(){
        if(!mMediaPlayerBeep.isPlaying()){
            mMediaPlayerBeep.seekTo(0);
            mMediaPlayerBeep.start();
        }
    }

    private void ifPlayingPauseBothBeeps(){
        if(mMediaPlayerBeep.isPlaying()) mMediaPlayerBeep.pause();
        if(mMediaPlayerBeep2.isPlaying()) mMediaPlayerBeep2.pause();
    }

    private void ifNotPlayingStartBeep2(){
        if(!mMediaPlayerBeep2.isPlaying()){
            mMediaPlayerBeep2.seekTo(0);
            mMediaPlayerBeep2.start();
        }
    }

}