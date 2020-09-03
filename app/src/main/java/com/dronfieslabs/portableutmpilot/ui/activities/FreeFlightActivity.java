package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.dronfieslabs.portableutmpilot.ui.fragments.FreeFlightFragment;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

public class FreeFlightActivity extends AppCompatActivity {

    private String operationId = null;

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //--------------------------------------------- OVERRIDED METHODS ---------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_flight);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.container, FreeFlightFragment.newInstance())
                .commit();

        try{
            operationId = getIntent().getStringExtra("OPERATION_ID");
        }catch(Exception ex){}
    }

    @Override
    protected void onResume(){
        super.onResume();
        setFullscreenAndScreenAlwaysOn();
    }


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PUBLIC METHODS  ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    public String getOperationId(){
        return this.operationId;
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void setFullscreenAndScreenAlwaysOn() {
        View decorView = UtilsOps.setFullscreenModeFlags(getWindow());
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                UtilsOps.setFullscreenModeFlags(getWindow());
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
