package com.dronfieslabs.portableutmpilot.ui.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;

import org.w3c.dom.Text;

public class TrackerWifiSettingsActivity extends AppCompatActivity {

    private Button mSaveButton;

    private TextView mSSID;
    private TextView mPassword;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_wifi);

        mSaveButton = findViewById(R.id.button_save_settings);
        mSSID = findViewById(R.id.ssid_tracker_input);
        mPassword = findViewById(R.id.password_tracker_input);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_tracker);

        String ssidFromIntent = getIntent().getStringExtra("ssid");
        String passwordFromIntent = getIntent().getStringExtra("password");

        if (ssidFromIntent != null) {
            mSSID.setText(ssidFromIntent);
        }

        if (passwordFromIntent != null) {
            mPassword.setText(passwordFromIntent);
        }

        mSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("ssid",mSSID.getText().toString());
                returnIntent.putExtra("password",mPassword.getText().toString());
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });
    }

    public void onClickEditSsid(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editSsid = new EditText(this);
        editSsid.setText(mSSID.getText());
        linearLayout.addView(editSsid);
        new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_ssid)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newUsername = editSsid.getText().toString();
                        mSSID.setText(newUsername);
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

    }

    public void onClickEditPassword(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editPassword = new EditText(this);
        editPassword.setText(mPassword.getText());
        linearLayout.addView(editPassword);
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newPassword = editPassword.getText().toString();
                        mPassword.setText(newPassword);
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

    }

}
