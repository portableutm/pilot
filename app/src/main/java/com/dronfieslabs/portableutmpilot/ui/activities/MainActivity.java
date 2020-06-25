package com.dronfieslabs.portableutmpilot.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;

import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // views
    private LinearLayout mLinearLayoutRoot;
    private Button mButtonOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // views binding
        mLinearLayoutRoot = findViewById(R.id.linear_layout_root);
        Button mButtonGoFly = findViewById(R.id.button_go_fly);
        mButtonGoFly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickGoFly();
            }
        });
        mButtonOperations = findViewById(R.id.button_operations);
        mButtonOperations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickOperations();
            }
        });
        Button mButtonSettings = findViewById(R.id.button_settings);
        mButtonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSettings();
            }
        });
    }

    @Override
    protected void onResume() {
        // hide or show mButtonOperations, depending on if utm is enabled or disabled
        if(!SharedPreferencesUtils.getUTMEnable(this)){
            if(mLinearLayoutRoot.findViewById(R.id.button_operations) != null){
                mLinearLayoutRoot.removeView(mButtonOperations);
            }
        }else{
            if(mLinearLayoutRoot.findViewById(R.id.button_operations) == null){
                mLinearLayoutRoot.addView(mButtonOperations, 1);
            }
        }
        super.onResume();
    }

    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------- EVENT HANDLERS --------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------

    private void onClickGoFly(){
        boolean utmEnable = SharedPreferencesUtils.getUTMEnable(this);
        if(!utmEnable){
            UIGenericUtils.GoToActivity(this, FreeFlightActivity.class);
        }else{
            UIGenericUtils.ShowConfirmationAlert(
                this,
                getString(R.string.str_report_position),
                getString(R.string.question_report_position_free_flight),
                getString(R.string.str_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToOperationsActivity(true);
                    }
                },
                getString(R.string.str_no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UIGenericUtils.GoToActivity(MainActivity.this, FreeFlightActivity.class);
                    }
                }
            );
        }
    }

    private void onClickOperations(){
        goToOperationsActivity(false);
    }

    private void onClickSettings(){
        Intent intent = new Intent(this, SettingsActivity.class);
        // we have to clear the activities stack before going to the settings activity
        // this is because in settings activity we can change important settings (like app language),
        // and we can't leave on the stack old activities after an important configuration change
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------- PRIVATE METHODS  -------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------

    private void goToOperationsActivity(final boolean nextGoToFreeFlight){
        DronfiesUssServices dronfiesUssServices = DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(MainActivity.this));
        if(dronfiesUssServices == null){
            UIGenericUtils.ShowAlert(this, getString(R.string.str_utm_connection_failed), getString(R.string.exc_msg_utm_connection_failed));
            return;
        }
        if(!dronfiesUssServices.isAuthenticated()){
            String username = SharedPreferencesUtils.getUsername(this);
            String password = SharedPreferencesUtils.getPassword(this);
            dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
                @Override
                public void onResponse(String s, String errorMessage) {
                    if(errorMessage != null){
                        UIGenericUtils.ShowAlert(MainActivity.this, getString(R.string.str_login_failed), getString(R.string.exc_msg_auth_to_see_operations) + " ("+errorMessage+")");
                        return;
                    }
                    if(nextGoToFreeFlight){
                        UIGenericUtils.GoToActivity(
                                MainActivity.this,
                                OperationsActivity.class,
                                Arrays.asList("NEXT_ACTION"),
                                Arrays.asList("FREE_FLIGHT")
                        );
                    }else{
                        UIGenericUtils.GoToActivity(MainActivity.this, OperationsActivity.class);
                    }
                }
            });
        }else{
            if(nextGoToFreeFlight){
                UIGenericUtils.GoToActivity(
                        MainActivity.this,
                        OperationsActivity.class,
                        Arrays.asList("NEXT_ACTION"),
                        Arrays.asList("FREE_FLIGHT")
                );
            }else{
                UIGenericUtils.GoToActivity(MainActivity.this, OperationsActivity.class);
            }
        }
    }
}