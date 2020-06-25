package com.dronfieslabs.portableutmpilot.ui.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.GPSCoordinates;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGoogleMapsUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DefinePolygonOnMapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String PARAM_KEY = "INFORMATION";
    public static final String DATA_SEPARATOR_KEY = "DATA_SEPARATOR";

    private GoogleMap map;
    private RelativeLayout mRelativeLayoutRoot;

    private String mReceivedData = null;
    private String mDataSeparator = null;
    private final List<Marker> mListMarkers = new ArrayList<>();
    private final List<LatLng> mListPolygonCoordinates = new ArrayList<>();
    private Polygon polygon = null;
    private final int[] polygonColorRGB = {255, 162, 0};
    private boolean instructionsShowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_define_polygon_on_map);

        mReceivedData = getIntent().getStringExtra(PARAM_KEY);
        mDataSeparator = getIntent().getStringExtra(DATA_SEPARATOR_KEY);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!instructionsShowed) {
            showInstructions();
            instructionsShowed = true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);

        UIGoogleMapsUtils.CenterMapOnDeviceLocation(this, map);

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
            final MarkerOptions markerOptions = new MarkerOptions().position(latLng).draggable(true);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(UIGoogleMapsUtils.GetMarkerBitmap(polygonColorRGB)));
            markerOptions.anchor(0.5f, 0.5f);
            mListMarkers.add(map.addMarker(markerOptions));
            mListPolygonCoordinates.add(latLng);
            updatePolygon();
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                UIGenericUtils.ShowConfirmationAlert(
                    DefinePolygonOnMapActivity.this,
                    getString(R.string.str_delete_point),
                    getString(R.string.question_delete_point),
                    getString(R.string.str_delete),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            marker.remove();
                            int index = mListMarkers.indexOf(marker);
                            if (index >= 0) {
                                mListMarkers.remove(index);
                                mListPolygonCoordinates.remove(index);
                                updatePolygon();
                            }
                        }
                    },
                    getString(R.string.str_cancel)
                );
                }
            });
            return true;
            }
        });

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                int index = mListMarkers.indexOf(marker);
                mListPolygonCoordinates.remove(index);
                mListPolygonCoordinates.add(index, marker.getPosition());
                updatePolygon();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });
    }

    private void updatePolygon() {
        if (polygon != null) polygon.remove();
        if (mListPolygonCoordinates.size() >= 2) {
            polygon = map.addPolygon(
                new PolygonOptions()
                .addAll(mListPolygonCoordinates)
                .fillColor(Color.argb(64, polygonColorRGB[0], polygonColorRGB[1], polygonColorRGB[2]))
                .strokeColor(Color.rgb(polygonColorRGB[0], polygonColorRGB[1], polygonColorRGB[2]))
                .strokeWidth(3f)
            );
        }
    }



    public void onClickGuardarPoligono(View view) {
        if (mListPolygonCoordinates.size() == 0) {
            UIGenericUtils.ShowAlert(this, getString(R.string.str_error), getString(R.string.exc_msg_no_polygon_drawn));
        } else if (mListPolygonCoordinates.size() == 1 || mListPolygonCoordinates.size() == 2) {
            UIGenericUtils.ShowAlert(this, getString(R.string.str_error), getString(R.string.exc_msg_not_enough_points));
        } else {
            // deserialize received data
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String description = mReceivedData.split(mDataSeparator)[0];
            Date startDatetime = null;
            try{
                startDatetime = sdf.parse(mReceivedData.split(mDataSeparator)[1]);
            }catch(Exception ex){
                UIGenericUtils.ShowToast(this, "Invalid startDatetime '"+mReceivedData.split(mDataSeparator)[1]+"'");
                return;
            }
            Date endDatetime = null;
            try{
                endDatetime = sdf.parse(mReceivedData.split(mDataSeparator)[2]);
            }catch(Exception ex){
                UIGenericUtils.ShowToast(this, "Invalid endDatetime '"+mReceivedData.split(mDataSeparator)[2]+"'");
                return;
            }
            int maxAltitude = Integer.parseInt(mReceivedData.split(mDataSeparator)[3]);
            String pilotName = "";
            try{
                pilotName = mReceivedData.split(mDataSeparator)[4];
            }catch (Exception ex){}
            String droneDescription = "";
            try{
                droneDescription = mReceivedData.split(mDataSeparator)[5];
            }catch(Exception ex){}

            // create operation object
            List<GPSCoordinates> polygon = new ArrayList<>();
            for(LatLng latLng : mListPolygonCoordinates){
                polygon.add(new GPSCoordinates(latLng.latitude, latLng.longitude));
            }
            LatLng firstVertix = mListPolygonCoordinates.get(0);
            polygon.add(new GPSCoordinates(firstVertix.latitude, firstVertix.longitude));
            Operation operation = new Operation(null, description, polygon, startDatetime, endDatetime, maxAltitude, pilotName, droneDescription, null);

            // show progress bar while the operation is being executed
            final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);

            // use DronfiesUssService to send the operation to the UTM
            DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(this)).addOperation(operation, new ICompletitionCallback<String>() {
                @Override
                public void onResponse(final String response, final String errorMessage) {
                // on response, we remove the progress bar
                mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                // then, we handle the response
                if(errorMessage != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIGenericUtils.ShowAlert(DefinePolygonOnMapActivity.this, null, getString(R.string.str_error) + ": " + errorMessage);
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    UIGenericUtils.ShowAlert(
                        DefinePolygonOnMapActivity.this,
                        getString(R.string.str_operation_added),
                        getString(R.string.conf_msg_operation_added),
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            // clear the activities stack and go back to the ProjectsActivity
                            Intent intent = new Intent(DefinePolygonOnMapActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            }
                        }
                    );
                    }
                });
                return;
                }
            });
        }
    }

    public void onClickSalir(View view) {
        if (mListPolygonCoordinates.size() == 0) {
            onBackPressed();
            return;
        }
        UIGenericUtils.ShowConfirmationAlert(
                this,
                getString(R.string.str_quit),
                getString(R.string.question_quit_without_save_polygon),
                getString(R.string.str_quit),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                },
                getString(R.string.str_dont_quit)
        );
    }

    public void onClickVerInstrucciones(View view) {
        showInstructions();
    }

    private void showInstructions() {
        UIGenericUtils.ShowAlert(
                this,
                getString(R.string.str_instructions),
                getString(R.string.miscellaneous_def_polygon_instructions));
    }
}
