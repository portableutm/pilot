package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import org.w3c.dom.Text;

public class TrackerRegister extends AppCompatActivity {

    private Button mButtonRegister;
    private RelativeLayout mRoot;
    private TextView mVehicle;
    private TextView mTrackerId;
    private TextView mVehicleName;

    // state
    private String tracker_id = null;
    private String vehicleId = null;
    private String vehicleName = null;

    public static final int REQUEST_CODE_SELECT_DRONE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_register);

        Bundle b = getIntent().getExtras();
        if(b != null)
            tracker_id = b.getString("tracker_id");

        mTrackerId = findViewById(R.id.tracker_id);
        mTrackerId.setText(tracker_id);

        mVehicle = findViewById(R.id.vehicle_uvin);
        mVehicleName = findViewById(R.id.vehicle_name);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_register_tracker);

        mRoot = findViewById(R.id.relative_root);

        mButtonRegister = findViewById(R.id.button_register);
        mButtonRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (vehicleId == null){
                    UIGenericUtils.ShowAlert(TrackerRegister.this,"No vehicle selected","Please select a vehicle to link to the tracker");
                }
                LinearLayout spin = UIGenericUtils.ShowProgressBar(mRoot);
                DronfiesUssServices api = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(TrackerRegister.this));
                new Thread(() -> {
                    try {
                        api.registerTracker(mTrackerId.getText().toString(), vehicleId);
                        runOnUiThread(() ->mRoot.removeView(spin));
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    } catch (Exception e) {
                        runOnUiThread(() ->mRoot.removeView(spin));
                        e.printStackTrace();
                        Intent intent = new Intent();
                        setResult(RESULT_CANCELED, intent);
                        finish();
                    }
                }).start();

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_DRONE){
            if(resultCode == RESULT_OK){
                vehicleId = data.getStringExtra(SelectDroneActivity.PARAM_VEHICLE_ID_KEY);
                vehicleName = data.getStringExtra(SelectDroneActivity.PARAM_VEHICLE_NAME_KEY);
                mVehicle.setText(vehicleId);
                mVehicleName.setText(vehicleName);
            }
        }
    }

    public void onClickEditVehicle(View view){
        Intent intent = new Intent(this, SelectDroneActivity.class);
        if(vehicleId != null){
            intent.putExtra(SelectDroneActivity.PARAM_VEHICLE_ID_KEY, vehicleId);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_DRONE);
    }
}