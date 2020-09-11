package com.dronfieslabs.portableutmpilot.ui.activities;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dji.mapkit.core.maps.DJIMap;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelper;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelperObserver;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.text.DecimalFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import dji.common.flightcontroller.FlightControllerState;
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
    private Aircraft mAircraftConnected = null;
    private int mDistanceBetweenAircraftAndHome = -1;
    private int mAircraftAltitude = -1;
    private double mAircraftHorizontalSpeed = -1;
    private double mAircraftVerticalSpeed = -1;

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

        // views binding
        mRelativeLayoutFullscreenMapFPV = findViewById(R.id.relative_layout_fullscreen_map_fpv);
        mRelativeLayoutSmallMapFPV = findViewById(R.id.relative_layout_small_map_fpv);
        //mFPVWidget = findViewById(R.id.fpv_widget);
        mFPVOverlayWidget = findViewById(R.id.fpv_overlay_widget);
        mMapWidget = findViewById(R.id.mapWidget);
        mMapWidget.initGoogleMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull final DJIMap map) {
                map.setMapType(DJIMap.MapType.SATELLITE);
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

        // execute onProductConnected, in case the product was connected before the user enter to the activity
        onProductConnected();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapWidget.onResume();
        setFullscreenAndScreenAlwaysOn();
        // subscribe to productConnect and productDisconnect methods
        DJISDKHelper.getInstance().addObserver(this);
    }

    @Override
    protected void onPause() {
        mMapWidget.onPause();
        // disubscribe to productConnect and productDisconnect methods
        DJISDKHelper.getInstance().removeObserver(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapWidget.onDestroy();
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

    private void onClickFullscreen(){
        /*if(mFPVWidget.getParent().equals(mRelativeLayoutFullscreenMapFPV)){
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
            mRelativeLayoutFullscreenMapFPV.addView(mFPVWidget, 0, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRelativeLayoutFullscreenMapFPV.addView(mFPVOverlayWidget, 1, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRelativeLayoutSmallMapFPV.addView(mMapWidget, 0);
        }*/
        UIGenericUtils.ShowToast(this, "not implemented!");
    }

    @Override
    public void onProductConnected() {
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
            }
        });
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

}