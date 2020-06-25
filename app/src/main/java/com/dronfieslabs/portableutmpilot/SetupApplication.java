package com.dronfieslabs.portableutmpilot;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class SetupApplication extends Application {

    private static SetupApplication sInstance;

    @Deprecated
    public static SetupApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Helper.install(SetupApplication.this);
    }

}
