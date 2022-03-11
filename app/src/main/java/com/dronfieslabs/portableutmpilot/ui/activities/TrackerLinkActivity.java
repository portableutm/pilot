package com.dronfieslabs.portableutmpilot.ui.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.Tracker;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

public class TrackerLinkActivity extends AppCompatActivity {

    private RelativeLayout mRelativeLayoutRoot;
    private Button mLinkButton;
    private Button mChangeVehicleButton;
    private TextView mUsername;
    private TextView mPassword;

    private String tracker_id;

    final private int INTENT_REGISTER_TRACKER = 2;
    final private int INTENT_CHANGE_VEHICLE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_link);

        tracker_id = getIntent().getStringExtra("tracker_id");

        mRelativeLayoutRoot = findViewById(R.id.relative_root);
        mLinkButton = findViewById(R.id.button_save_settings);
        mChangeVehicleButton = findViewById(R.id.button_change_vehicle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_tracker);
        getSupportActionBar().setSubtitle(tracker_id);

        mUsername = findViewById(R.id.username_tracker_input);
        mPassword = findViewById(R.id.password_tracker_input);
        String usernameFromIntent = getIntent().getStringExtra("username");
        if (usernameFromIntent != null) {
            mUsername.setText(usernameFromIntent);
        } else {
            mUsername.setText("");
        }
        mPassword.setText("");

        mLinkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("username",mUsername.getText().toString());
                returnIntent.putExtra("password",mPassword.getText().toString());
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case INTENT_REGISTER_TRACKER:
            case INTENT_CHANGE_VEHICLE:
                if(resultCode == RESULT_OK){
//                    linkTracker(data.getStringExtra("username"), data.getStringExtra("password"));
                }
                break;
        }
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

    private void changeVehicle() {
        boolean found = false;
        final LinearLayout spin = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);


        String user = mUsername.getText().toString();
        String pass = mPassword.getText().toString();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DronfiesUssServices api = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(TrackerLinkActivity.this));
                    api.login_sync(user, pass);

                    Tracker instance = null;
                    instance = api.getTrackerInformation(tracker_id);
                    if (instance != null) {
                        runOnUiThread(() -> mRelativeLayoutRoot.removeView(spin));
                        Intent intent = new Intent(TrackerLinkActivity.this, TrackerRegister.class);
                        Bundle b = new Bundle();
                        b.putString("tracker_id", tracker_id);
                        b.putBoolean("isEdit", true);
                        intent.putExtras(b);
                        startActivityForResult(intent, INTENT_CHANGE_VEHICLE);
                    } else {
                        runOnUiThread(() -> mRelativeLayoutRoot.removeView(spin));
                        runOnUiThread(() -> UIGenericUtils.ShowConfirmationAlert(TrackerLinkActivity.this,
                                getString(R.string.tracker_not_registered), getString(R.string.tracker_not_registered_msg),
                                getString(R.string.str_register_tracker), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(TrackerLinkActivity.this, TrackerRegister.class);
                                        Bundle b = new Bundle();
                                        b.putString("tracker_id", tracker_id);
                                        intent.putExtras(b);
                                        startActivityForResult(intent, INTENT_REGISTER_TRACKER);
                                    }
                                }, getString(R.string.str_cancel)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();
    }

}
