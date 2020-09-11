package com.dronfieslabs.portableutmpilot.djiwrapper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import dji.common.airlink.LightbridgeDataRate;
import dji.common.airlink.SignalQualityCallback;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.imu.IMUState;
import dji.common.flightcontroller.imu.SensorState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.BatteryKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.sdk.airlink.AirLink;
import dji.sdk.airlink.LightbridgeLink;
import dji.sdk.airlink.OcuSyncLink;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.io.reactivex.Single;
import dji.thirdparty.io.reactivex.SingleEmitter;
import dji.thirdparty.io.reactivex.SingleOnSubscribe;

/**
 * Created by gabriel on 8/18/17.
 */

public class DJISDKHelper {

    private static final String TAG = DJISDKHelper.class.getName();
    private static String LOG_TAG = "_DJISDKHelper_";
    public static final String ACTION_CONNECTION_CHANGE = "com.vistaguay.volare.ACTION_CONNECTION_CHANGE";
    public static final String ACTION_REGISTRATION_COMPLETE = "com.vistaguay.volare.ACTION_REGISTRATION_COMPLETE";
    public static final String EXTRA_REGISTRATION_RESULT = "com.vistaguay.volare.EXTRA_REGISTRATION_RESULT";
    public static final String EXTRA_REGISTRATION_ERROR = "com.vistaguay.volare.EXTRA_REGISTRATION_ERROR";

    private static BaseProduct baseProduct;
    private static String connectedSerialNumber;
    private static Handler mainLoopHandler;

    private List<DJISDKHelperObserver> observers;

    private static DJISDKHelper instance = new DJISDKHelper();

    private SensorState mAccelerometerState, mGyroscopeState;

//    private Thread streamThread;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public DJISDKHelper() {
        this.observers = new ArrayList<DJISDKHelperObserver>();
    }

    public void addObserver(DJISDKHelperObserver observer){
        this.observers.add(observer);
    }

    public void removeObserver(DJISDKHelperObserver observer){
        this.observers.remove(observer);
    }

    public static DJISDKHelper getInstance() {
        return instance;
    }


    public synchronized void initDjiSdk(final Context applicationContext) {
        mainLoopHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().registerApp(applicationContext,
                new DJISDKManager.SDKManagerCallback() {

                    @Override
                    public void onProductDisconnect() {
                        connectedSerialNumber = null;
//                        djiStreamManager.stopStream();

                        //vuelvo a dejar el estado del IMU en null
                        mAccelerometerState = null;
                        mGyroscopeState = null;

                        broadcastProductChange();
                        for(DJISDKHelperObserver observer : observers){
                            observer.onProductDisconnected();
                        }
                    }

                    @Override
                    public void onProductConnect(final BaseProduct thisBaseProduct) {
                        //actualizo el baseProduct
                        baseProduct = thisBaseProduct;
                        //seteo el serial del drone conectado, en la variable connectedSerialNumber
                        try{
                            ((Aircraft)thisBaseProduct).getFlightController().getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    connectedSerialNumber = s;
                                    //notifico a los observers que se obtuvo el serial del drone conectado
                                    for(DJISDKHelperObserver observer : observers){
                                        observer.onSerialObtained();
                                    }
                                }
                                @Override
                                public void onFailure(DJIError djiError) {}
                            });
                        }catch(Exception ex){}

                        //comienzo a actualizar el estado del IMU
                        try{
                            ((Aircraft)thisBaseProduct).getFlightController().setIMUStateCallback(new IMUState.Callback() {
                                @Override
                                public void onUpdate(@NonNull IMUState imuState) {
                                    //Este metodo del sdk a veces retorna valores en null. En le caso que me de valores null, los descarto.
                                    if(imuState.getAccelerometerState() != null){
                                        mAccelerometerState = imuState.getAccelerometerState();
                                    }
                                    if(imuState.getGyroscopeState() != null){
                                        mGyroscopeState = imuState.getGyroscopeState();
                                    }
                                }
                            });
                        }catch(Exception ex){}

//                        djiStreamManager = new DJIStreamManager();
//                        djiStreamManager.startStream();
                        broadcastProductChange();

                        //una vez que obtuve el serial del drone conectado, notifico a los observers que se conecto un drone
                        for(DJISDKHelperObserver observer : observers){
                            observer.onProductConnected();
                        }
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {

                    }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {
                        Log.d("DJISDKHelper", "onComponentChange " + componentKey + ": " + baseComponent + " / " + baseComponent1);
                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                    }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) {

                    }

                    private void broadcastProductChange() {
//                        doStream();
                        //TODO: Reducir el delay y notificar de otra forma la conexión inicial
                        mainLoopHandler.removeCallbacks(broadcastProductChangeRunnable);
                        mainLoopHandler.postDelayed(broadcastProductChangeRunnable, 2000);
                    }

                    private Runnable broadcastProductChangeRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ACTION_CONNECTION_CHANGE);
                            applicationContext.sendBroadcast(intent);
                        }
                    };

                    private void broadcastRegistrationResult(final boolean registrationWasSuccessful,
                                                             final String errorMessage) {
                        mainLoopHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(ACTION_REGISTRATION_COMPLETE);
                                intent.putExtra(EXTRA_REGISTRATION_RESULT, registrationWasSuccessful);
                                intent.putExtra(EXTRA_REGISTRATION_ERROR, errorMessage);
                                applicationContext.sendBroadcast(intent);
                            }
                        }, 500);
                    }

                    @Override
                    public void onRegister(final DJIError error) {
                        final boolean registrationWasSuccessful = error == DJISDKError.REGISTRATION_SUCCESS;
                        if (registrationWasSuccessful) {
                            DJISDKManager.getInstance().startConnectionToProduct();
                        }
                        broadcastRegistrationResult(registrationWasSuccessful, error.getDescription());
                    }
                });
    }

    public static synchronized BaseProduct getProductInstance() {
        if (baseProduct == null) {
            baseProduct = DJISDKManager.getInstance().getProduct();
        }
        return baseProduct;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (getProductInstance() == null) return null;
        Aircraft aircraft = null;
        if (getProductInstance() instanceof Aircraft) {
            aircraft = (Aircraft) getProductInstance();
        }
        return aircraft;
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = getProductInstance().getCamera();
        return camera;
//        if (getProductInstance() instanceof Aircraft){
//        } else if (getProductInstance() instanceof HandHeld) {
//        }
    }



}
