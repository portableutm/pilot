package com.dronfieslabs.portableutmpilot.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelper;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.activities.FreeFlightActivity;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class FreeFlightFragment extends androidx.fragment.app.Fragment implements View.OnClickListener {

    @BindView(R.id.button_cancel) ImageButton mButtonCancel;
    @BindView(R.id.tv_free_flight_mode)
    TextView mFreeFlightMode;
    @BindView(R.id.button_secondary)
    Button mReturnButton;

    private Handler handler;
    private BroadcastReceiver mProductStateChangedReceiver;
    private FlightController mFlightController;
    private FlightControllerState.Callback mFlightControllerStateCallback;
    private Context mContext;

    // we try to send the position to the UTM each 3 seconds, so we need this variable to know when was the last time we sent it
    private long mLastPositionSentTimestamp = -1;

    public FreeFlightFragment() {
        // Required empty public constructor
    }

    public static FreeFlightFragment newInstance() {
        FreeFlightFragment fragment = new FreeFlightFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        initDJIListeners();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_free_flight, container, false);
        ButterKnife.bind(this, v);

        mButtonCancel.setOnClickListener(this);

        mReturnButton.setOnClickListener(this);

        //Virtual Stick
        Aircraft aircraft = DJISDKHelper.getAircraftInstance();
        if(aircraft!=null){
            mFlightController = aircraft.getFlightController();
            initSingleShootMode();
        }

        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_cancel:
                getActivity().onBackPressed();
                break;
            case R.id.button_secondary:
                new AlertDialog.Builder(mContext)
                        .setMessage(R.string.question_return_to_home)
                        .setPositiveButton(R.string.str_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                returnToHome();
                            }
                        })
                        .setNegativeButton(R.string.str_cancel, null)
                        .show();
                break;
        }
    }

    private void returnToHome(){
        Aircraft aircraft = DJISDKHelper.getAircraftInstance();
        if (aircraft != null) {
            aircraft.getFlightController().startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(getString(R.string.str_error));
                    }
                }
            });
        } else {
            showToast(getString(R.string.str_drone_not_connected));
        }
    }

    private void initDJIListeners() {

        mProductStateChangedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                onProductStateChanged();
            }
        };

        mFlightControllerStateCallback = new FlightControllerState.Callback() {
            @Override
            public void onUpdate(FlightControllerState state) {
                GPSSignalLevel gpsSignalLevel = state.getGPSSignalLevel();
                if(state.getSatelliteCount() < 6 || (gpsSignalLevel != GPSSignalLevel.LEVEL_3 && gpsSignalLevel != GPSSignalLevel.LEVEL_4 && gpsSignalLevel != GPSSignalLevel.LEVEL_5)){
                    // if we dont have good gps signal, we dont execute the flight controller state listener
                    return;
                }
                double lat = state.getAircraftLocation().getLatitude();
                double lon = state.getAircraftLocation().getLongitude();
                double alt = state.getAircraftLocation().getAltitude();
                if(alt < 0) alt = 0;
                double heading = state.getAircraftHeadDirection();

                if(getOperationId() != null){
                    long now = new Date().getTime();
                    if((now - mLastPositionSentTimestamp)/1000 >= 3){
                        mLastPositionSentTimestamp = now;
                        try{
                            UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(FreeFlightFragment.this.getContext())).sendPosition(lon, lat, alt, heading, getOperationId(), new ICompletitionCallback<String>() {
                                @Override
                                public void onResponse(String s, String errorMessage) {}
                            });
                        }catch(Exception ex){}
                    }
                }
            }
        };
    }

    private void attachComponentsStateListeners(){
        addFlightControllerStateListener();
    }

    private void addFlightControllerStateListener() {

        BaseProduct product = DJISDKHelper.getProductInstance();

        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
                //Added for emulated controls
                mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(mFlightControllerStateCallback);
        }
    }

    private void onProductStateChanged() {
        BaseProduct product = DJISDKHelper.getProductInstance();
        if (product != null && product.isConnected()) {
            attachComponentsStateListeners();
        }
    }

    private void removeFlightControllerStateListener() {
        if (mFlightController != null) {
            mFlightController.setStateCallback(null);
        }
    }

    private void showToast(final String msg) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                //a msg le sumo "" por las dudas que venga en null
                Toast.makeText(getActivity(), msg + "", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initSingleShootMode() {
        //get camera
        final Camera camera = DJISDKHelper.getCameraInstance();
        if (camera != null) {
            //set mode to shoot photo
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError==null) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE,
                                        new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError == null) {
                                                    Log.d("ExecutionFragment", "Camera Mode Single Photo");
                                                } else {
                                                    Log.d("ExecutionFragment", "Camera Mode Error: " + djiError.getDescription());
                                                    initSingleShootMode();
                                                }
                                            }
                                        });
                            }
                        }, 2000);
                    } else {
                        Log.d("ExecutionFragment", "Camera Mode Error: " + djiError.getDescription());
                        initSingleShootMode();
                    }
                }
            });
        } else {
            Log.d("ExecutionFragment", "Camera null");
            initSingleShootMode();
        }
    }

    private void removeComponentsStateListeners(){
        removeFlightControllerStateListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        BaseProduct product = DJISDKHelper.getProductInstance();
        if (product != null && product.isConnected()) {
            attachComponentsStateListeners();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISDKHelper.ACTION_CONNECTION_CHANGE);
        getActivity().registerReceiver(mProductStateChangedReceiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mProductStateChangedReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        removeComponentsStateListeners();
    }

    private String getOperationId(){
        return ((FreeFlightActivity)getActivity()).getOperationId();
    }
}

