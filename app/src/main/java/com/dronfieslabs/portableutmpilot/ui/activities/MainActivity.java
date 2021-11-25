package com.dronfieslabs.portableutmpilot.ui.activities;

import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dronfies.portableutmandroidclienttest.ExpressOperationData;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // views
    private LinearLayout mLinearLayoutRoot;
    private Button mButtonOperations;
    private RelativeLayout mRelativeLayoutRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // views binding
        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);
        mLinearLayoutRoot = findViewById(R.id.linear_layout_root);
        final Button mButtonGoFly = findViewById(R.id.button_go_fly);
        mButtonGoFly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickGoFly();
            }
        });
        final Button mButtonInstantRequest = findViewById(R.id.button_instant_request);
        mButtonInstantRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickInstantRequest();}
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
                onClickSettings(false);
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

    private void onClickInstantRequest(){
        AlertDialog alertDialog = showDialogSayingToTheUserHeHasToWait30Seconds();
        DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(MainActivity.this));
        if(dronfiesUssServices == null){
            UIGenericUtils.ShowAlert(MainActivity.this, getString(R.string.str_utm_connection_failed), getString(R.string.exc_msg_utm_connection_failed));
            return;
        }
        String vehicleId = SharedPreferencesUtils.getExpressVehicle(this);
        if(vehicleId == ""){
            alertDialog.dismiss();
            showDialogExplainingToTheUserHeHasToSelectAVehicleToUseThisFeature();
        }
        UtilsOps.getLocation(MainActivity.this, (latLng, errorMessage) -> {
            //CREATES THE OPERATION//
            int radius = SharedPreferencesUtils.getExpressRadius(MainActivity.this);
            int duration = SharedPreferencesUtils.getExpressDuration(MainActivity.this);
            ExpressOperationData oper = new ExpressOperationData(latLng,radius,duration,vehicleId);
            //*********************//
            if(!dronfiesUssServices.isAuthenticated()){
                String username = SharedPreferencesUtils.getUsername(MainActivity.this);
                String password = SharedPreferencesUtils.getPassword(MainActivity.this);
                dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
                    @Override
                    public void onResponse(String s, String errorMessage) {
                        if(errorMessage != null){
                            UIGenericUtils.ShowAlert(MainActivity.this, getString(R.string.str_login_failed), getString(R.string.exc_msg_auth_to_see_operations) + " ("+errorMessage+")");
                            return;
                        }
                        onClickInstantRequest_2ndPart(dronfiesUssServices, oper, alertDialog);
                    }
                });
            }else{
                onClickInstantRequest_2ndPart(dronfiesUssServices, oper, alertDialog);
            }
        });
    }

    private void onClickInstantRequest_2ndPart(DronfiesUssServices dronfiesUssServices, ExpressOperationData operation, AlertDialog alertDialog){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dronfiesUssServices.addExpressOperation_sync(operation);
                    // we need to wait 30 seconds to let the backend create the operation
                    Thread.sleep(30000);
                    runOnUiThread(() -> {
                        alertDialog.dismiss();
                    });
                    UIGenericUtils.GoToActivity(MainActivity.this, OperationsActivity.class);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        alertDialog.dismiss();
                        UIGenericUtils.ShowAlert(MainActivity.this, getString(R.string.express_vehicle_not_selected_title), getString(R.string.express_vehicle_not_selected));
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void onClickGoFly(){
        boolean utmEnable = SharedPreferencesUtils.getUTMEnable(this);
        if(!utmEnable){
            UIGenericUtils.GoToActivity(this, FlightActivity.class);
        }else{
            goToOperationsActivity(true);
        }
    }

    private void onClickOperations(){
        goToOperationsActivity(false);
    }

    private void onClickSettings(boolean scrollDownSettingsActivity){
        Intent intent = new Intent(this, SettingsActivity.class);
        // we have to clear the activities stack before going to the settings activity
        // this is because in settings activity we can change important settings (like app language),
        // and we can't leave on the stack old activities after an important configuration change
        if(scrollDownSettingsActivity){
            intent.putExtra(Constants.SCROLL_TO_BOTTOM_KEY, true);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------- PRIVATE METHODS  -------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------------

    private AlertDialog showDialogSayingToTheUserHeHasToWait30Seconds(){
        AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.str_creating_operation)
                .setMessage(R.string.miscellaneous_wait_until_op_created)
                .setCancelable(false)
                .create();

        alertDialog.show();

        return alertDialog;
    }

    private void showDialogExplainingToTheUserHeHasToSelectAVehicleToUseThisFeature(){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(50, 50,50,50);
        linearLayout.setLayoutParams(param);

        Button button = new Button(new ContextThemeWrapper(this, R.style.RaisedButton), null, 0);
        button.setText(getString(R.string.str_settings));
        button.setOnClickListener(view -> onClickSettings(true));
        linearLayout.addView(button);

        AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.express_vehicle_not_selected_title))
                .setMessage(getString(R.string.express_vehicle_not_selected))
                .setView(linearLayout)
                .create();

        alertDialog.show();
    }

    private void goToOperationsActivity(final boolean nextGoToFreeFlight){
        DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(MainActivity.this));
        if(dronfiesUssServices == null){
            UIGenericUtils.ShowAlert(this, getString(R.string.str_utm_connection_failed), getString(R.string.exc_msg_utm_connection_failed));
            return;
        }
        if(!dronfiesUssServices.isAuthenticated()){
            String username = SharedPreferencesUtils.getUsername(this);
            String password = SharedPreferencesUtils.getPassword(this);
            final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
            dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
                @Override
                public void onResponse(String s, String errorMessage) {
                    mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
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