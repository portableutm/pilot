package com.dronfieslabs.portableutmpilot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.dronfieslabs.portableutmpilot.ui.activities.TrackerSettingsActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectDeviceActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        // Bluetooth Setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled())
        {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        } else {
            nextStep();
        }




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            nextStep();
        }else{
            View view = findViewById(R.id.recyclerViewDevice);
            Snackbar snackbar = Snackbar.make(view, "You denied bluetooth activation", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Ask to the user turn the bluetooth on
                    Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnBTon,1);
                }
            });
            snackbar.show();
        }
    }
    private void nextStep() {
            // Get List of Paired Bluetooth Device
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            List<Object> deviceList = new ArrayList<>();
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    DeviceInfoModel deviceInfoModel = new DeviceInfoModel(deviceName,deviceHardwareAddress);
                    deviceList.add(deviceInfoModel);
                }
                // Display paired device using recyclerView
                RecyclerView recyclerView = findViewById(R.id.recyclerViewDevice);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                DeviceListAdapter deviceListAdapter = new DeviceListAdapter(this,deviceList);
                recyclerView.setAdapter(deviceListAdapter);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
            } else {
                View view = findViewById(R.id.recyclerViewDevice);
                Snackbar snackbar = Snackbar.make(view, "Pair a Bluetooth device", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) { }
                });
                snackbar.show();
            }
    }
    @Override
    public void onBackPressed() {
        finish();
    }
}