package com.dronfieslabs.portableutmpilot.djiwrapper;

public interface DJISDKHelperObserver {

    void onProductConnected();

    void onProductDisconnected();

    void onSerialObtained();
}
