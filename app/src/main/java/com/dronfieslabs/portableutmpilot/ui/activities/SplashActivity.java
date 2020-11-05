package com.dronfieslabs.portableutmpilot.ui.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.djiwrapper.DJISDKHelper;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import java.util.Locale;


public class SplashActivity extends AppCompatActivity {

    // 65535 is the max
    private static final int RC_APP_PERMISSIONS = 65535/66;

    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        boolean registrationWasSuccessful = intent.getBooleanExtra(DJISDKHelper.EXTRA_REGISTRATION_RESULT, false);
        if(registrationWasSuccessful){
            goToNextActivity();
        }else{
            UIGenericUtils.ShowErrorAlertWithOkButton(
                SplashActivity.this,
                getString(R.string.str_sign_up_failed),
                getString(R.string.exc_msg_dji_sdk_sign_up_failed),
                getString(R.string.str_it_is_understood),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToNextActivity();
                    }
                }
            );
        }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISDKHelper.ACTION_REGISTRATION_COMPLETE);
        registerReceiver(broadcastReceiver, filter);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initializeDjiSdk();
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                        Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                        Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA
                },
                RC_APP_PERMISSIONS);

        setAppLocale();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != RC_APP_PERMISSIONS) return;
        for (int i = 0; i < grantResults.length; i++){
            if(!permissions[i].equals(Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS) &&
                    !permissions[i].equals(Manifest.permission.SYSTEM_ALERT_WINDOW) &&
                    grantResults[i] == PackageManager.PERMISSION_DENIED){
                //TODO: Mostrar mensaje error/retry
                finish();
            }
        }
        initializeDjiSdk();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void initializeDjiSdk() {
        DJISDKHelper.getInstance().initDjiSdk(getApplicationContext());
    }

    private void setAppLocale(){
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            conf.setLocale(new Locale(SharedPreferencesUtils.getAppLocale(this).toLowerCase()));
        }else{
            conf.locale = new Locale(SharedPreferencesUtils.getAppLocale(this).toLowerCase());
        }
        res.updateConfiguration(conf, dm);
    }

    private void goToNextActivity(){
        String username = SharedPreferencesUtils.getUsername(this);
        if(username != null && !username.isEmpty()){
            UIGenericUtils.GoToActivity(SplashActivity.this, MainActivity.class);
        }else{
            UIGenericUtils.GoToActivity(SplashActivity.this, LoginActivity.class);
        }
    }
}