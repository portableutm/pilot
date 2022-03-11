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

public class TrackerSimSettingsActivity extends AppCompatActivity {

    private TextView mAPN;
    private TextView mUsername;
    private TextView mPassword;
    private TextView mPin;

    private Button mSaveButton;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_sim);

        mSaveButton = findViewById(R.id.button_save_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_tracker);

        mAPN =findViewById(R.id.apn_tracker_input);
        mUsername = findViewById(R.id.username_tracker_input);
        mPassword = findViewById(R.id.password_tracker_input);
        mPin = findViewById(R.id.pin_tracker_input);

        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        String apn = getIntent().getStringExtra("apn");
        String pin = String.valueOf(getIntent().getIntExtra("pin",0000));

        if (username != null) {
            mUsername.setText(username);
        }
        if (password != null) {
            mPassword.setText(password);
        }
        if (apn != null) {
            mAPN.setText(apn);
        }
        if (pin != "0000") {
            mPin.setText(pin);
        }

        mSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("apn",mAPN.getText().toString());
                returnIntent.putExtra("username",mUsername.getText().toString());
                returnIntent.putExtra("password",mPassword.getText().toString());
                returnIntent.putExtra("pin",mPin.getText().toString());
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });
    }

    public void onClickEditApn(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editAPN = new EditText(this);
        editAPN.setText(mAPN.getText());
        linearLayout.addView(editAPN);
        new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_apn)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newUsername = editAPN.getText().toString();
                        mAPN.setText(newUsername);
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

    }

    public void onClickEditUsername(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editUsername = new EditText(this);
        editUsername.setText(mUsername.getText());
        linearLayout.addView(editUsername);
        new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_username)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newUsername = editUsername.getText().toString();
                        mUsername.setText(newUsername);
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


    public void onClickEditPin(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editPin = new EditText(this);
        editPin.setText(mPin.getText());
        linearLayout.addView(editPin);
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_pin)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newPassword = editPin.getText().toString();
                        mPin.setText(newPassword);
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

    }


}
