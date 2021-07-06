package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dronfies.portableutmandroidclienttest.Endpoint;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.ui.Constants;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {

    // state
    private List<Endpoint> mEndpoints;

    // views
    private LinearLayout mLinearLayoutEndpoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // init state
        mEndpoints = new ArrayList<>();
        String[] receivedEndpoints = getIntent().getStringArrayExtra(Constants.UTM_ENDPOINTS_KEY);

        if(receivedEndpoints != null){
            Log.d(SignUpActivity.class.getName() + "_Logs", receivedEndpoints.length + "");
            for(String strEndpoint : receivedEndpoints){
                mEndpoints.add(new Gson().fromJson(strEndpoint, Endpoint.class));
            }
        }

        // views binding
        mLinearLayoutEndpoints = findViewById(R.id.linear_layout_endpoints);

        loadEndpoints();
    }

    private void loadEndpoints(){
        if(mEndpoints == null || mEndpoints.isEmpty()) return;
        for(Endpoint endpoint : mEndpoints){
            LinearLayout linearLayoutEndpoint = (LinearLayout)getLayoutInflater().inflate(R.layout.layout_endpoint, null);

            ImageView imageViewFlag = linearLayoutEndpoint.findViewById(R.id.image_view_flag);
            UIGenericUtils.SetDrawableIntoImageView(this, imageViewFlag, String.format("ic_flag_%s", endpoint.getCountryCode()).toLowerCase());

            TextView textViewName = linearLayoutEndpoint.findViewById(R.id.text_view_name);
            textViewName.setText(endpoint.getName());

            TextView textViewEndpoint = linearLayoutEndpoint.findViewById(R.id.text_view_endpoint);
            textViewEndpoint.setText(endpoint.getFrontendEndpoint());

            Button buttonSelect = linearLayoutEndpoint.findViewById(R.id.button_select);
            buttonSelect.setOnClickListener(view -> onClickSelectEndpoint(endpoint));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mLinearLayoutEndpoints.addView(linearLayoutEndpoint, layoutParams);
        }
    }

    private void onClickSelectEndpoint(Endpoint endpoint){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s/registration", endpoint.getFrontendEndpoint())));
        startActivity(browserIntent);
    }
}