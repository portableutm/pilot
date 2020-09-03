package com.dronfieslabs.portableutmpilot.djiwrapper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJISDKHelper {

    public static final String ACTION_CONNECTION_CHANGE = "com.dronfieslabs.portableutmpilot.ACTION_CONNECTION_CHANGE";
    public static final String ACTION_REGISTRATION_COMPLETE = "com.dronfieslabs.portableutmpilot.ACTION_REGISTRATION_COMPLETE";
    public static final String EXTRA_REGISTRATION_RESULT = "com.dronfieslabs.portableutmpilot.EXTRA_REGISTRATION_RESULT";
    public static final String EXTRA_REGISTRATION_ERROR = "com.dronfieslabs.portableutmpilot.EXTRA_REGISTRATION_ERROR";

    private static BaseProduct baseProduct;
    private static Handler mainLoopHandler;

    private static final DJISDKHelper instance = new DJISDKHelper();

    public DJISDKHelper() {}

    public static DJISDKHelper getInstance() {
        return instance;
    }


    public synchronized void initDjiSdk(final Context applicationContext) {
        mainLoopHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().registerApp(applicationContext,
            new DJISDKManager.SDKManagerCallback() {

                @Override
                public void onProductDisconnect() {
                    broadcastProductChange();
                }

                @Override
                public void onProductConnect(final BaseProduct thisBaseProduct) {
                    //actualizo el baseProduct
                    baseProduct = thisBaseProduct;

                    broadcastProductChange();
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

                private final Runnable broadcastProductChangeRunnable = new Runnable() {
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
        return getProductInstance().getCamera();
    }

}
