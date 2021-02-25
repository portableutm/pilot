package com.dronfieslabs.portableutmpilot.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dronfieslabs.portableutmpilot.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

public class OperationActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String FLIGHT_COMMENTS = "FLIGHT_COMMENTS";
    public static final String DESCRIPTION_KEY = "DESCRIPTION";
    public static final String START_KEY = "START";
    public static final String END_KEY = "END";
    public static final String MAX_ALTITUDE_KEY = "MAX_ALTITUDE";
    public static final String PILOT_KEY = "PILOT";
    public static final String CONTACT_PHONE_KEY = "CONTACT_PHONE";
    public static final String DRONE_KEY = "DRONE";
    public static final String POLYGON_KEY = "POLYGON";

    private final int[] polygonColorRGB = {255, 162, 0};

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //--------------------------------------------- LIFECYCLE METHODS ---------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation);

        // add colon to the textviews
        ((TextView)findViewById(R.id.textview_description_title)).append(":");
        ((TextView)findViewById(R.id.textview_start_title)).append(":");
        ((TextView)findViewById(R.id.textview_end_title)).append(":");
        ((TextView)findViewById(R.id.textview_max_altitude_title)).append(":");
        ((TextView)findViewById(R.id.textview_pilot_title)).append(":");
        ((TextView)findViewById(R.id.textview_contact_phone_title)).append(":");
        ((TextView)findViewById(R.id.textview_drone_title)).append(":");

        // add operation data to the textviews
        String flightComments = getIntent().getStringExtra(FLIGHT_COMMENTS);
        TextView textViewFlightComments = findViewById(R.id.textview_flight_comments);
        if(flightComments == null){
            ((LinearLayout)textViewFlightComments.getParent()).removeView(textViewFlightComments);
        }else{
            textViewFlightComments.setText(flightComments);
        }
        ((TextView)findViewById(R.id.textview_description_value)).setText(getIntent().getStringExtra(DESCRIPTION_KEY));
        ((TextView)findViewById(R.id.textview_start_value)).setText(getIntent().getStringExtra(START_KEY));
        ((TextView)findViewById(R.id.textview_end_value)).setText(getIntent().getStringExtra(END_KEY));
        ((TextView)findViewById(R.id.textview_max_altitude_value)).setText(getIntent().getStringExtra(MAX_ALTITUDE_KEY));
        ((TextView)findViewById(R.id.textview_pilot_value)).setText(getIntent().getStringExtra(PILOT_KEY));
        ((TextView)findViewById(R.id.textview_contact_phone_value)).setText(getIntent().getStringExtra(CONTACT_PHONE_KEY));
        ((TextView)findViewById(R.id.textview_drone_value)).setText(getIntent().getStringExtra(DRONE_KEY));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // add the polygon to the map
        List<LatLng> polygon = parsePolygon(getIntent().getStringExtra(POLYGON_KEY));
        for(LatLng vertice : polygon){
            MarkerOptions markerOptions = new MarkerOptions().position(vertice);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmap()));
            markerOptions.anchor(0.5f, 0.5f);
            googleMap.addMarker(markerOptions);
        }
        googleMap.addPolygon(
                new PolygonOptions()
                        .addAll(polygon)
                        .fillColor(Color.argb(64, polygonColorRGB[0], polygonColorRGB[1], polygonColorRGB[2]))
                        .strokeColor(Color.rgb(polygonColorRGB[0], polygonColorRGB[1], polygonColorRGB[2]))
                        .strokeWidth(3f)
        );

        // center the map on the polygon
        try{
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng vertice : polygon) {
                builder.include(vertice);
            }
            LatLngBounds bounds = builder.build();
            int padding = 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,
                    padding);
            googleMap.animateCamera(cu);
        }catch(Exception ex){}
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    public void onClickSalir(View view) {
        onBackPressed();
    }


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private List<LatLng> parsePolygon(String strPolygon){
        // received string example: [(34.22;-56.14),(34.44;-56.60),(34.31;-56.18)]
        List<LatLng> retorno = new ArrayList();
        try{
            // if there is an exception parsing the string, we return empty list
            String strCoordinates = strPolygon.substring(1, strPolygon.length()-1);
            for(String strLatLon : strCoordinates.split(",")){
                String strLatLonWithoutParentesis = strLatLon.substring(1, strLatLon.length()-1);
                double lat = Double.parseDouble(strLatLonWithoutParentesis.split(";")[0]);
                double lon = Double.parseDouble(strLatLonWithoutParentesis.split(";")[1]);
                retorno.add(new LatLng(lat, lon));
            }
        }catch(Exception ex){}
        return retorno;
    }

    private Bitmap getMarkerBitmap() {
        int circleRadius = 20;
        Bitmap bitmap = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(polygonColorRGB[0], polygonColorRGB[1], polygonColorRGB[2]));
        canvas.drawCircle(circleRadius, circleRadius, circleRadius, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(circleRadius, circleRadius, circleRadius - 3, paint);
        return bitmap;
    }
}
