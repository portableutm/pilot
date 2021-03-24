package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;

public class ReportDevicePositionActivity extends AppCompatActivity {

    // constants
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // services
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private ValueAnimator mValueAnimatorReportingPosition;
    private Thread mReportingThread;

    // state
    private Double mCurrentLatitude = null;
    private Double mCurrentLongitude = null;
    private Double mCurrentAltitude = null;
    private boolean mIsReporting = false;

    // views
    private TextView mTextViewLatitude;
    private TextView mTextViewLongitude;
    private TextView mTextViewAltitude;
    private TextView mTextViewReportingMessage;
    private Button mButtonReport;


    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------- LIFE CYCLE METHODS  --------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_device_position);

        // view binding
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_report_position);
        mTextViewLatitude = findViewById(R.id.text_view_latitude);
        mTextViewLongitude = findViewById(R.id.text_view_longitude);
        mTextViewAltitude = findViewById(R.id.text_view_altitude);
        mTextViewReportingMessage = findViewById(R.id.text_view_reporting_message);
        mButtonReport = findViewById(R.id.button_report);
        mButtonReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonReport();
            }
        });

        initializeLocationRequest();
        initializeFusedLocationProviderClient();
        initializeValueAnimator();
        initializeReportingThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        try{
            mReportingThread.interrupt();
        }catch (Exception ex){}
    }

    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //----------------------------------- ANOTHER OVERRIDED METHODS -----------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.back_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.item_back){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }else{
                    UIGenericUtils.ShowToast(this, getString(R.string.exc_msg_permission_not_granted));
                    finish();
                }
                break;
        }
    }

    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //---------------------------------------- EVENT HANDLERS  ----------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    private void onClickButtonReport(){
        mIsReporting = !mIsReporting;
        updateButtonReport();
    }

    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //---------------------------------------- PRIVATE METHODS ----------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    private void initializeLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void initializeFusedLocationProviderClient(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // user provided the permissions to use the location of the device
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    Location location = locationResult.getLastLocation();
                    mCurrentLatitude = location.getLatitude();
                    mCurrentLongitude = location.getLongitude();
                    mCurrentAltitude = location.getAltitude();
                    updateCoordinatesViews();
                    if(mButtonReport.getVisibility() == View.INVISIBLE){
                        mButtonReport.setVisibility(View.VISIBLE);
                    }
                }
            };
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }else{
            // permissions not granted yet
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateCoordinatesViews(){
        mTextViewLatitude.setText("-");
        mTextViewLongitude.setText("-");
        mTextViewAltitude.setText("-");
        if(mCurrentLatitude != null){
            mTextViewLatitude.setText(mCurrentLatitude + " °");
        }
        if(mCurrentLongitude != null){
            mTextViewLongitude.setText(mCurrentLongitude + " °");
        }
        if(mCurrentAltitude != null){
            mTextViewAltitude.setText(new DecimalFormat("0.00").format(mCurrentAltitude) + " m");
        }
    }

    private void updateButtonReport(){
        if(mIsReporting){
            mButtonReport.setText(R.string.str_stop_reporting);
            mButtonReport.setBackground(getDrawable(R.drawable.rounded_button_red));
            mButtonReport.setTextColor(Color.rgb(255, 99, 99));
        }else{
            mButtonReport.setText(R.string.str_start_reporting);
            mButtonReport.setBackground(getDrawable(R.drawable.rounded_button_green));
            mButtonReport.setTextColor(Color.rgb(121, 193, 96));
        }
    }

    private void initializeValueAnimator(){
        mValueAnimatorReportingPosition = new ValueAnimator();
        mValueAnimatorReportingPosition.setDuration(2000);
        mValueAnimatorReportingPosition.setEvaluator(new ArgbEvaluator());
        mValueAnimatorReportingPosition.setIntValues(0, 255, 0);
        mValueAnimatorReportingPosition.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(mIsReporting){
                    mTextViewReportingMessage.setTextColor(Color.argb((int)animation.getAnimatedValue(), 255, 0, 0));
                }else{
                    mTextViewReportingMessage.setTextColor(Color.argb(0, 255, 0, 0));
                }
            }
        });
        mValueAnimatorReportingPosition.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimatorReportingPosition.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimatorReportingPosition.start();
    }

    private void initializeReportingThread(){
        mReportingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Thread.sleep(3000);
                    }catch (Exception ex){
                        // if the thread is interrupted, we finish the while
                        break;
                    }
                    if(mIsReporting){
                        try{
                            DronfiesUssServices dronfiesUssServices = DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(ReportDevicePositionActivity.this));
                            if(!dronfiesUssServices.isAuthenticated()){
                                dronfiesUssServices.login_sync(SharedPreferencesUtils.getUsername(ReportDevicePositionActivity.this), SharedPreferencesUtils.getPassword(ReportDevicePositionActivity.this));
                            }
                            dronfiesUssServices.sendParaglidingPosition(mCurrentLongitude, mCurrentLatitude, mCurrentAltitude, new ICompletitionCallback<String>() {
                                @Override
                                public void onResponse(String s, String errorMessage) {
                                    if(errorMessage != null){
                                        mTextViewReportingMessage.setText(errorMessage);
                                        return;
                                    }
                                    mTextViewReportingMessage.setText(getString(R.string.str_reporting_position));
                                }
                            });
                        }catch(final Exception ex){
                            mTextViewReportingMessage.setText(ex.getMessage());
                        }
                    }
                }
            }
        });
        mReportingThread.start();
    }
}