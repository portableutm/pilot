package com.dronfieslabs.portableutmpilot.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.GPSCoordinates;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfies.portableutmandroidclienttest.entities.Operation;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DefinePolygonManuallyActivity extends AppCompatActivity {

    public static final String PARAM_KEY = "INFORMATION";
    public static final String DATA_SEPARATOR_KEY = "DATA_SEPARATOR";

    private String mReceivedData = null;
    private String mDataSeparator = null;
    // we use this list to save on memory the vertices added by the user
    private final List<double[]> mVertices = new ArrayList<>();

    private LinearLayout mLinearLayoutCoordinates;
    private RelativeLayout mRelativeLayoutRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_define_polygon_manually);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.str_define_polygon));

        mLinearLayoutCoordinates = findViewById(R.id.linear_layout_coordinates);

        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);

        mReceivedData = getIntent().getStringExtra(PARAM_KEY);
        mDataSeparator = getIntent().getStringExtra(DATA_SEPARATOR_KEY);
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

    public void onClickGuardarPoligono(View view){
        if (mVertices.size() == 0) {
            UIGenericUtils.ShowAlert(this, getString(R.string.str_error), getString(R.string.exc_msg_no_polygon_drawn));
        } else if (mVertices.size() == 1 || mVertices.size() == 2) {
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
            String contactPhone = "";
            try{
                contactPhone = mReceivedData.split(mDataSeparator)[5];
            }catch (Exception ex){}
            String vehicleId = null;
            try{
                vehicleId = mReceivedData.split(mDataSeparator)[6];
            }catch(Exception ex){
                UIGenericUtils.ShowToast(this, "Invalid drone id");
                return;
            }
            String owner = SharedPreferencesUtils.getUsername(this);

            // create operation object
            List<GPSCoordinates> polygon = new ArrayList<>();
            for(double[] latLng : mVertices){
                polygon.add(new GPSCoordinates(latLng[0], latLng[1]));
            }
            Operation operation = new Operation(null, description, polygon, startDatetime, endDatetime, maxAltitude, pilotName, contactPhone, vehicleId, null, null, owner, null);

            // show progress bar while the operation is being executed
            final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);

            // use DronfiesUssService to send the operation to the UTM
            try {
                UtilsOps.getDronfiesUssServices(SharedPreferencesUtils.getUTMEndpoint(this)).addOperation_sync(operation);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                        UIGenericUtils.ShowAlert(
                                DefinePolygonManuallyActivity.this,
                                getString(R.string.str_operation_added),
                                getString(R.string.conf_msg_operation_added),
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        // clear the activities stack and go back to the MainActivity
                                        Intent intent = new Intent(DefinePolygonManuallyActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }
                                }
                        );
                    }
                });
                return;
            } catch (final Exception e) {
                mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                        UIGenericUtils.ShowAlert(DefinePolygonManuallyActivity.this, null, getString(R.string.str_error) + ": " + e.getMessage());
                    }
                });
                return;
            }
        }
    }

    public void onClickAgregarCoordenadas(View view){
        // we show a dialog to ask for the coordinates
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(50, 10,50,10);
        linearLayout.setLayoutParams(param);

        TextView textViewLatitude = new TextView(this);
        textViewLatitude.setText(R.string.str_latitude);
        linearLayout.addView(textViewLatitude);
        final EditText editTextLatitude = new EditText(this);
        linearLayout.addView(editTextLatitude);

        TextView textViewLongitude = new TextView(this);
        textViewLongitude.setText(R.string.str_longitude);
        textViewLongitude.setPadding(0,20,0,0);
        linearLayout.addView(textViewLongitude);
        final EditText editTextLongitude = new EditText(this);
        linearLayout.addView(editTextLongitude);

        new AlertDialog.Builder(this)
            .setTitle(R.string.str_add_vertex)
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.str_add), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    double latitude = 0;
                    try{
                        latitude = Double.parseDouble(editTextLatitude.getText().toString());
                        if(latitude < -90 || latitude > 90) throw new Exception();
                    }catch(Exception ex){
                        UIGenericUtils.ShowAlert(DefinePolygonManuallyActivity.this, getString(R.string.str_invalid_latitude), getString(R.string.exc_msg_invalid_latitude));
                        return;
                    }
                    double longitude = 0;
                    try{
                        longitude = Double.parseDouble(editTextLongitude.getText().toString());
                        if(longitude < -180 || longitude > 180) throw new Exception();
                    }catch(Exception ex){
                        UIGenericUtils.ShowAlert(DefinePolygonManuallyActivity.this, getString(R.string.str_invalid_longitude), getString(R.string.exc_msg_invalid_longitude));
                        return;
                    }
                    addVertexToTheActivity(latitude, longitude);
                    mVertices.add(new double[]{latitude, longitude});
                }
            })
            .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }


    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------
    //--------------------------------------- PRIVATE METHODS ---------------------------------------
    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------

    private void addVertexToTheActivity(double latitude, double longitude){
        final LinearLayout linearLayoutVertex = new LinearLayout(this);
        linearLayoutVertex.setOrientation(LinearLayout.HORIZONTAL);
        linearLayoutVertex.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));
        linearLayoutVertex.setPadding(50, 0, 0, 0);

        LinearLayout linearLayoutLatLonTitle = new LinearLayout(this);
        linearLayoutLatLonTitle.setPadding(0, 0, 20, 0);
        linearLayoutLatLonTitle.setOrientation(LinearLayout.VERTICAL);
        linearLayoutLatLonTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0f));
        linearLayoutLatLonTitle.setGravity(Gravity.CENTER_VERTICAL);
        linearLayoutVertex.addView(linearLayoutLatLonTitle);

        TextView textViewLatitudeTitle = new TextView(this);
        textViewLatitudeTitle.setText(getString(R.string.str_latitude) + ":");
        linearLayoutLatLonTitle.addView(textViewLatitudeTitle);

        TextView textViewLongitudeTitle = new TextView(this);
        textViewLongitudeTitle.setText(getString(R.string.str_longitude) + ":");
        linearLayoutLatLonTitle.addView(textViewLongitudeTitle);

        LinearLayout linearLayoutLatLonValue = new LinearLayout(this);
        linearLayoutLatLonValue.setOrientation(LinearLayout.VERTICAL);
        linearLayoutLatLonValue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        linearLayoutLatLonValue.setGravity(Gravity.CENTER_VERTICAL);
        linearLayoutVertex.addView(linearLayoutLatLonValue);

        TextView textViewLatitudeValue = new TextView(this);
        textViewLatitudeValue.setText("" + latitude);
        linearLayoutLatLonValue.addView(textViewLatitudeValue);

        TextView textViewLongitudeValue = new TextView(this);
        textViewLongitudeValue.setText("" + longitude);
        linearLayoutLatLonValue.addView(textViewLongitudeValue);

        ImageButton imageButtonMenu = new ImageButton(new ContextThemeWrapper(this, R.style.FlatButton), null, 0);
        imageButtonMenu.setImageResource(R.drawable.ic_more_vert_black_24dp);
        imageButtonMenu.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        imageButtonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(DefinePolygonManuallyActivity.this, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                    if(menuItem.getItemId() == R.id.item_borrar_latlng){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int indexOf = mLinearLayoutCoordinates.indexOfChild(linearLayoutVertex);
                                mLinearLayoutCoordinates.removeView(linearLayoutVertex);
                                mVertices.remove(indexOf);
                            }
                        });
                        return true;
                    }else{
                        return false;
                    }
                    }
                });
                popupMenu.inflate(R.menu.latlng_menu);
                popupMenu.show();
            }
        });
        linearLayoutVertex.addView(imageButtonMenu);

        mLinearLayoutCoordinates.addView(linearLayoutVertex);
    }
}
