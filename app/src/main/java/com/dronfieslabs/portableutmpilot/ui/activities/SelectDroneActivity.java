package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.Vehicle;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.util.ArrayList;
import java.util.List;

public class SelectDroneActivity extends AppCompatActivity {

    // const
    public static final String PARAM_VEHICLE_ID_KEY = "VEHICLE_ID_KEY";
    public static final String PARAM_VEHICLE_NAME_KEY = "VEHICLE_NAME_KEY";

    // state
    private String mSelectedVehicleId = null;
    private int vehicleCount = 0;

    // views
    private RelativeLayout mRelativeLayoutRoot;
    private LinearLayout mLinearLayoutDrones;
    private AppCompatButton mButtonLoadMore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_drone);

        // state
        mSelectedVehicleId = getIntent().getStringExtra(PARAM_VEHICLE_ID_KEY);

        // views binding
        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);
        mLinearLayoutDrones = findViewById(R.id.linear_layout_drones);

        mButtonLoadMore = findViewById(R.id.button_load_more);
        mButtonLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDrones();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrones();
    }

    private void loadDrones(){
        // before loading the drones, we show a progress bar
        final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
        try{
            final DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(SelectDroneActivity.this));
            final String username = SharedPreferencesUtils.getUsername(SelectDroneActivity.this);
            String password = SharedPreferencesUtils.getPassword(SelectDroneActivity.this);
            dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
                @Override
                public void onResponse(String s, String errorMessage) {
                    if(errorMessage != null){
                        UIGenericUtils.ShowAlert(SelectDroneActivity.this, getString(R.string.str_login_failed), getString(R.string.exc_msg_auth_to_see_operations) + " ("+errorMessage+")");
                        return;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                dronfiesUssServices.getOperatorVehicles(10, vehicleCount, new ICompletitionCallback<List<Vehicle>>() {
                                    @Override
                                    public void onResponse(List<Vehicle> vehicles, String s) {
                                        List<Vehicle> listUserVehicles = vehicles;
                                        // onResponse, we remove the progressbar from the activity
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                                            }
                                        });
                                        if(listUserVehicles.isEmpty() && vehicleCount == 0){
                                            // user has no vehicles
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    UIGenericUtils.ShowAlert(
                                                            SelectDroneActivity.this,
                                                            getString(R.string.str_no_drones),
                                                            getString(R.string.exc_msg_user_has_no_drones));
                                                }
                                            });
                                            return;
                                        } else if (listUserVehicles.isEmpty()) {
                                            //No more vehicles to show
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    UIGenericUtils.ShowAlert(
                                                            SelectDroneActivity.this,
                                                            getString(R.string.str_no_drones),
                                                            getString(R.string.exc_msg_no_more_drones));
                                                }
                                            });
                                        }
                                        for(final Vehicle vehicle : listUserVehicles){
                                            final CardView cardView = (CardView) getLayoutInflater().inflate(R.layout.layout_drone, null);
                                            int dp20 = UIGenericUtils.ConvertDPToPX(SelectDroneActivity.this, 20);
                                            ((TextView)cardView.findViewById(R.id.text_view_name)).setText(vehicle.getVehicleName());
                                            ((TextView)cardView.findViewById(R.id.text_view_plate)).setText(getString(R.string.str_plate) + ": " + vehicle.getFaaNumber());
                                            ((TextView)cardView.findViewById(R.id.text_view_serial)).setText(getString(R.string.str_serial) + ": " + vehicle.getnNumber());
                                            ((TextView)cardView.findViewById(R.id.text_view_model)).setText(getString(R.string.str_model) + ": " + vehicle.getModel() + " ("+vehicle.getManufacturer()+")");
                                            ((TextView)cardView.findViewById(R.id.text_view_uvin)).setText(getString(R.string.str_uvin) + ": " + vehicle.getUvin());
                                            if(vehicle.getVehicleClass() != null){
                                                if(vehicle.getVehicleClass().equals(Vehicle.EnumVehicleClass.MULTIROTOR)){
                                                    ((ImageView)cardView.findViewById(R.id.image_view_icon)).setImageDrawable(getDrawable(R.drawable.ic_drone_32));
                                                }else if(vehicle.getVehicleClass().equals(Vehicle.EnumVehicleClass.FIXEDWING)){
                                                    ((ImageView)cardView.findViewById(R.id.image_view_icon)).setImageDrawable(getDrawable(R.drawable.ic_flight_takeoff_24));
                                                }
                                            } else { //Default icon
                                                ((ImageView)cardView.findViewById(R.id.image_view_icon)).setImageDrawable(getDrawable(R.drawable.ic_drone_32));
                                            }
                                            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                            layoutParams.setMargins(0, 0, 0, dp20);
                                            cardView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    onClickDrone(vehicle);
                                                }
                                            });
                                            if(mSelectedVehicleId != null && vehicle.getUvin().equals(mSelectedVehicleId)){
                                                cardView.setBackgroundColor(getColor(R.color.colorPrimary));
                                                ((TextView)cardView.findViewById(R.id.text_view_name)).setTextColor(getColor(R.color.white));
                                                ((TextView)cardView.findViewById(R.id.text_view_plate)).setTextColor(getColor(R.color.white));
                                                ((TextView)cardView.findViewById(R.id.text_view_serial)).setTextColor(getColor(R.color.white));
                                                ((TextView)cardView.findViewById(R.id.text_view_model)).setTextColor(getColor(R.color.white));
                                                ((ImageView)cardView.findViewById(R.id.image_view_icon)).setColorFilter(getColor(R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mLinearLayoutDrones.addView(cardView, layoutParams);
                                                }
                                            });
                                            vehicleCount++;
                                        }
                                    }
                                });

                            }catch (Exception ex){}
                        }
                    }).start();
                }
            });
        }catch(Exception ex){}
    }

    private void onClickDrone(Vehicle vehicle){
        Intent intent = new Intent();
        intent.putExtra(PARAM_VEHICLE_ID_KEY, vehicle.getUvin());
        intent.putExtra(PARAM_VEHICLE_NAME_KEY, vehicle.getVehicleName());
        setResult(RESULT_OK, intent);
        finish();
    }
}