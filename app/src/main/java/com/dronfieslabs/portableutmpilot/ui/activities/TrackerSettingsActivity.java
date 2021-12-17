package com.dronfieslabs.portableutmpilot.ui.activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfies.portableutmandroidclienttest.Directory;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.Tracker;
import com.dronfies.portableutmandroidclienttest.User;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class TrackerSettingsActivity extends AppCompatActivity {

    private RelativeLayout mRelativeLayoutRoot;
    private LinearLayout progressBar;
    private Button mButtonSave;
    private Button mButtonChangeVehicle;
    private TextView mUsername;
    private TextView mPassword;

    final private int INTENT_REGISTER_TRACKER = 2;
    final private int INTENT_CHANGE_VEHICLE = 3;

    private String tracker_id;

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread = null;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_settings);

        mRelativeLayoutRoot = findViewById(R.id.relative_root);
        mButtonSave = findViewById(R.id.button_save_settings);
        mButtonChangeVehicle = findViewById(R.id.button_change_vehicle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_tracker);

        mUsername = findViewById(R.id.username_tracker_input);
        mPassword = findViewById(R.id.password_tracker_input);

        mUsername.setText(SharedPreferencesUtils.getUsername(this));
        mPassword.setText(SharedPreferencesUtils.getPassword(this));


        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle(getString(R.string.connecting_to) + deviceName + "...");
            progressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                //Show a message that the device has no bluetooth adapter
                Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
                    createConnectThread.start();
                } else {
                    //Ask to the user turn the bluetooth on
                    Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnBTon, 1);
                }
            }

        }
        /**
         GUI Handler
         **/
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                mRelativeLayoutRoot.removeView(progressBar);
                                break;
                            case -1:

                                UIGenericUtils.ShowAlert(TrackerSettingsActivity.this, "Tracker not connected","Tracker is not connected",
                                        new DialogInterface.OnDismissListener() {

                                            @Override
                                            public void onDismiss(DialogInterface dialog) {
                                              finish();
                                            }
                                        } );
                                mRelativeLayoutRoot.removeView(progressBar);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String message = msg.obj.toString(); // Read message from Arduino
                        String normalized = message.toLowerCase().substring(0,3);
                        boolean isId = normalized.equals("id:");
                        if (isId){
                            tracker_id = message.substring(3);
                            toolbar.setSubtitle("Tracker id: " + tracker_id);
                        }

                        break;
                }
            }
        };


        // Save settings in the tracker
        mButtonSave.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                linkTracker();
            }
        });
        mButtonChangeVehicle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                changeVehicle();
            }
        });

    }

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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == INTENT_REGISTER_TRACKER || requestCode == INTENT_CHANGE_VEHICLE){
            if(resultCode == RESULT_OK){
                linkTracker();
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickGoFly(){
        UIGenericUtils.GoToActivity(this, FlyWithTrackerActivity.class);
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
            try {
                connectedThread.write("retrieveId");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Tracker Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) throws Exception {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                throw new Exception("Could not connect to device");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
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
        final EditText editTextExpressDuration = new EditText(this);
        editTextExpressDuration.setText(mUsername.getText());
        linearLayout.addView(editTextExpressDuration);
        new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_username)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newUsername = editTextExpressDuration.getText().toString();
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
        final EditText editTextExpressDuration = new EditText(this);
        editTextExpressDuration.setText(mPassword.getText());
        linearLayout.addView(editTextExpressDuration);
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newPassword = editTextExpressDuration.getText().toString();
                        mPassword.setText(newPassword);
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

    }
    boolean getTrackerConfigurationFromInstance(JSONObject settings) throws Exception {
        boolean found = false;

        String user = SharedPreferencesUtils.getUsername(TrackerSettingsActivity.this);
        String pass = SharedPreferencesUtils.getPassword(TrackerSettingsActivity.this);

        DronfiesUssServices api = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(TrackerSettingsActivity.this));
        api.login_sync(user,pass);
        User userInfo = api.getUserInfo();
        settings.put("name",userInfo.getFirstName());
        settings.put("surname", userInfo.getLastName());
        settings.put("mail",userInfo.getEmail());

        Tracker instance = api.getTrackerInformation(tracker_id);
        if ( instance != null ) {
            found = true;
            settings.put("endpoint",SharedPreferencesUtils.getUTMEndpoint(TrackerSettingsActivity.this));
            settings.put("uvin", instance.vehicle.getUvin());
        }
        return found;
    }

    private void linkTracker(){
        final LinearLayout spin = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
        JSONObject settings = new JSONObject();
        try {
            settings.put("username",mUsername.getText());
            settings.put("password",mPassword.getText());

            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        if ( getTrackerConfigurationFromInstance(settings) ){
                            connectedThread.write(settings.toString());
                            runOnUiThread(() ->mRelativeLayoutRoot.removeView(spin));
                            runOnUiThread(()->UIGenericUtils.ShowErrorAlertWithOkButton(TrackerSettingsActivity.this,
                                    getString(R.string.link_sucess), getString(R.string.link_sucess_msg), getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    }));
                        } else {
                            runOnUiThread(() ->mRelativeLayoutRoot.removeView(spin));
                            runOnUiThread(() -> UIGenericUtils.ShowConfirmationAlert(TrackerSettingsActivity.this,
                                    getString(R.string.tracker_not_registered),getString(R.string.tracker_not_registered_msg),
                                    getString(R.string.str_register_tracker), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(TrackerSettingsActivity.this, TrackerRegister.class);
                                            Bundle b = new Bundle();
                                            b.putString("tracker_id", tracker_id);
                                            intent.putExtras(b);
                                            startActivityForResult(intent, INTENT_REGISTER_TRACKER);
                                        }
                                    }, getString(R.string.str_cancel)));

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->mRelativeLayoutRoot.removeView(spin));
                        runOnUiThread(()->UIGenericUtils.ShowAlert(TrackerSettingsActivity.this, "Error!",e.getMessage(), null));
                        return;
                    }
                }
            });
            th.start();
        } catch (Exception e) {
            UIGenericUtils.ShowAlert(TrackerSettingsActivity.this, "Error!",e.getMessage(), null);
            return;
        }
    }

    private void changeVehicle() {
        boolean found = false;
        final LinearLayout spin = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);


        String user = SharedPreferencesUtils.getUsername(TrackerSettingsActivity.this);
        String pass = SharedPreferencesUtils.getPassword(TrackerSettingsActivity.this);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DronfiesUssServices api = UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(TrackerSettingsActivity.this));
                    api.login_sync(user, pass);

                    Tracker instance = null;
                    instance = api.getTrackerInformation(tracker_id);
                    if (instance != null) {
                        runOnUiThread(() -> mRelativeLayoutRoot.removeView(spin));
                        Intent intent = new Intent(TrackerSettingsActivity.this, TrackerRegister.class);
                        Bundle b = new Bundle();
                        b.putString("tracker_id", tracker_id);
                        b.putBoolean("isEdit", true);
                        intent.putExtras(b);
                        startActivityForResult(intent, INTENT_CHANGE_VEHICLE);
                    } else {
                        runOnUiThread(() -> mRelativeLayoutRoot.removeView(spin));
                        runOnUiThread(() -> UIGenericUtils.ShowConfirmationAlert(TrackerSettingsActivity.this,
                                getString(R.string.tracker_not_registered), getString(R.string.tracker_not_registered_msg),
                                getString(R.string.str_register_tracker), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(TrackerSettingsActivity.this, TrackerRegister.class);
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