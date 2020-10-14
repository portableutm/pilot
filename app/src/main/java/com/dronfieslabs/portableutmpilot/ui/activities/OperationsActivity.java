package com.dronfieslabs.portableutmpilot.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.DronfiesUssServices;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.NoAuthenticatedException;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.GPSCoordinates;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class OperationsActivity extends AppCompatActivity {

    // const
    private static int OPERATIONS_PER_PAGE = 10;

    // state
    private String nextAction = null;
    private int mOperationsLoaded = 0;

    // views
    private LinearLayout mLinearLayoutOperations;
    private RelativeLayout mRelativeLayoutRoot;
    private AppCompatButton mButtonLoadMore;

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //--------------------------------------------- LIFECYCLE METHODS ---------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_operations);

        mLinearLayoutOperations = findViewById(R.id.linearLayoutOperations);
        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);
        mButtonLoadMore = findViewById(R.id.button_load_more);
        mButtonLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickLoadOperations();
            }
        });

        try{
            nextAction = getIntent().getStringExtra("NEXT_ACTION");
        }catch(Exception ex){}

        if(nextAction != null && nextAction.equals("FREE_FLIGHT")){
            // if next action is FREE_FLIGHT, hide add operation button
            findViewById(R.id.buttonAddOperation).setVisibility(View.INVISIBLE);
        }

        loadOperations();
    }

    @Override
    protected void onResume(){
        super.onResume();
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


    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickLoadOperations(){
        loadOperations();
    }

    public void onClickAddOperation(View view){
        UIGenericUtils.GoToActivity(OperationsActivity.this, AddOperationActivity.class);
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void loadOperations(){
        // before loading the operations, we show a progress bar
        final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
        // to refresh the operations list, we first remove all old operations
        //mLinearLayoutOperations.removeAllViews();
        try {
            DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(this)).getOperations(OPERATIONS_PER_PAGE, mOperationsLoaded, new ICompletitionCallback<List<Operation>>() {
                @Override
                public void onResponse(final List<Operation> operations, final String errorMessage) {
                    // onResponse, we remove the progressbar from the activity
                    mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                    // now we handle the response
                    if(errorMessage != null){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIGenericUtils.ShowAlert(OperationsActivity.this, getString(R.string.exc_msg_getting_operations_error),getString(R.string.str_error) + ": " + errorMessage);
                            }
                        });
                        return;
                    }
                    // if there not was an error, load operations into the activity
                    int operationsLoaded = 0;
                    for(Operation operation : operations){
                        /*if(operation.getState() == null || operation.getState() == Operation.EnumOperationState.CLOSED || operation.getState() == Operation.EnumOperationState.NOT_ACCEPTED){
                            continue;
                        }*/
                        // adjust operation startdatetime and enddatetime with device timezone
                        int timezoneOffsetInMilliseconds = Calendar.getInstance().getTimeZone().getRawOffset();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(operation.getStartDatetime());
                        cal.add(Calendar.MILLISECOND, timezoneOffsetInMilliseconds);
                        operation.setStartDatetime(cal.getTime());
                        cal.setTime(operation.getEndDatetime());
                        cal.add(Calendar.MILLISECOND, timezoneOffsetInMilliseconds);
                        operation.setEndDatetime(cal.getTime());
                        // add operation to the activity
                        mLinearLayoutOperations.addView(getOperationView(operation));
                        operationsLoaded++;
                    }
                    mOperationsLoaded += operationsLoaded;
                }
            });
        } catch (NoAuthenticatedException e) {
            UIGenericUtils.ShowToast(OperationsActivity.this, getString(R.string.exc_msg_auth_to_see_operations));
            return;
        }
    }

    // return the view that has to be added to the linearLayoutOperations
    private View getOperationView(final Operation operation){
        CardView cardView = new CardView(this);
        cardView.setBackgroundColor(getColor(R.color.white));
        final LinearLayout linearLayoutRoot =  new LinearLayout(this);
        cardView.addView(linearLayoutRoot);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, UIGenericUtils.ConvertDPToPX(this, 10));
        cardView.setLayoutParams(new LinearLayout.LayoutParams(layoutParams));

        int black = Color.rgb(0, 0, 0);
        int grey = Color.rgb(100, 100, 100);
        int green = Color.rgb(92, 184, 92);
        int blue = Color.rgb(2, 117, 216);
        int red = Color.rgb(217, 83, 79);
        int yellow = Color.rgb(240, 173, 78);
        int stateColor = grey;
        Integer stateDrawableId = null;
        if(operation.getState() == null){
            stateColor = black;
        }else if(operation.getState() == Operation.EnumOperationState.PROPOSED){
            stateColor = blue;
            stateDrawableId = R.drawable.ic_help_outline_white_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.NOT_ACCEPTED){
            stateColor = red;
            stateDrawableId = R.drawable.ic_close_black_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.ACCEPTED){
            stateColor = green;
            stateDrawableId = R.drawable.ic_done_black_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.ACTIVATED){
            stateColor = green;
            stateDrawableId = R.drawable.ic_power_settings_new_black_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.NONCONFORMING){
            stateColor = yellow;
            stateDrawableId = R.drawable.ic_power_settings_new_black_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.ROGUE){
            stateColor = red;
            stateDrawableId = R.drawable.ic_power_settings_new_black_24dp;
        }else if(operation.getState() == Operation.EnumOperationState.CLOSED){
            stateColor = grey;
            stateDrawableId = R.drawable.ic_done_black_24dp;
        }


        ImageView imageViewState = new ImageView(this);
        imageViewState.setColorFilter(stateColor);
        imageViewState.setPadding(20, 20, 20, 20);
        if(stateDrawableId != null){
            imageViewState.setImageResource(stateDrawableId);
        }
        imageViewState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIGenericUtils.ShowToast(OperationsActivity.this, getString(R.string.str_state) + ": " + operation.getState());
            }
        });

        TextView textViewDescription = new TextView(this);
        textViewDescription.setTextColor(stateColor);
        textViewDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
        textViewDescription.setPadding(30, 30, 30, 0);
        textViewDescription.setText(operation.getDescription());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        TextView textViewStartDate = new TextView(this);
        textViewStartDate.setTextColor(stateColor);
        textViewStartDate.setPadding(30, 0, 30, 0);
        textViewStartDate.setText("Desde: " + sdf.format(operation.getStartDatetime()));

        TextView textViewEndDate = new TextView(this);
        textViewEndDate.setTextColor(stateColor);
        textViewEndDate.setPadding(30, 0, 30, 30);
        textViewEndDate.setText("Hasta: " + sdf.format(operation.getEndDatetime()));

        LinearLayout linearLayoutOperationData = new LinearLayout(this);
        linearLayoutOperationData.setLayoutParams(new TableLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        linearLayoutOperationData.setOrientation(LinearLayout.VERTICAL);
        linearLayoutOperationData.addView(textViewDescription);
        linearLayoutOperationData.addView(textViewStartDate);
        linearLayoutOperationData.addView(textViewEndDate);

        ImageButton imageButtonMenu = new ImageButton(new ContextThemeWrapper(this, R.style.FlatButton), null, 0);
        imageButtonMenu.setColorFilter(stateColor);
        imageButtonMenu.setImageResource(R.drawable.ic_more_vert_black_24dp);
        imageButtonMenu.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        imageButtonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            PopupMenu popupMenu = new PopupMenu(OperationsActivity.this, v);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if(menuItem.getItemId() == R.id.item_borrar_operacion){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            UIGenericUtils.ShowConfirmationAlert(
                                OperationsActivity.this,
                                getString(R.string.str_delete_operation),
                                getString(R.string.question_delete_operation, operation.getDescription()),
                                getString(R.string.str_delete),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // show progress bar while deleting operation
                                        final LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
                                        // delete the operation
                                        DronfiesUssServices.getInstance(SharedPreferencesUtils.getUTMEndpoint(OperationsActivity.this)).deleteOperation(operation.getId(), new ICompletitionCallback<String>() {
                                        @Override
                                        public void onResponse(String s, final String errorMessage) {
                                            // remove progress bar
                                            mRelativeLayoutRoot.removeView(linearLayoutProgressBar);
                                            // handle response
                                            if(errorMessage != null){
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    UIGenericUtils.ShowAlert(OperationsActivity.this, "Error borrando operación","Error: " + errorMessage);
                                                }
                                            });
                                            return;
                                        }
                                        // if operation was removed, delete view from the activity
                                        mLinearLayoutOperations.removeView(linearLayoutRoot);
                                            // show message indicating the operation was deleted
                                        UIGenericUtils.ShowAlert(OperationsActivity.this, "Operación Borrada", "La operación '"+operation.getDescription()+"' ha sido borrada correctamente");
                                        }
                                    });
                                    }
                                },
                                getString(R.string.str_cancel)
                            );
                            }
                        });
                        return true;
                    }else{
                        return false;
                    }
                }
            });
            popupMenu.inflate(R.menu.operation_menu);
            popupMenu.show();
            }
        });
        linearLayoutRoot.setGravity(Gravity.CENTER_VERTICAL);
        linearLayoutRoot.setOrientation(LinearLayout.HORIZONTAL);
        // --- <clickable effect> ---
        TypedValue typedValue = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        linearLayoutRoot.setBackgroundResource(typedValue.resourceId);
        linearLayoutRoot.setClickable(true);
        // --- </clickable effect> ---
        linearLayoutRoot.addView(imageViewState);
        linearLayoutRoot.addView(linearLayoutOperationData);
        if(nextAction == null) linearLayoutRoot.addView(imageButtonMenu);
        linearLayoutRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nextAction != null){
                    if(nextAction.equals("FREE_FLIGHT")){
                        if(operation.getState() != Operation.EnumOperationState.ACTIVATED){
                            UIGenericUtils.ShowAlert(OperationsActivity.this, getString(R.string.str_operation_is_not_activated), getString(R.string.exc_msg_operation_not_activated));
                            return;
                        }
                        Intent intent = new Intent(OperationsActivity.this, FlightActivity.class);
                        intent.putExtra("OPERATION_ID", operation.getId());
                        intent.putExtra("OPERATION_MAX_ALTITUDE", operation.getMaxAltitude());
                        // we do not pass the last coordinate, because the first and the last coordinate of the polygon are the same
                        String[] vecPolygonCoordinates = new String[operation.getPolygon().size() - 1];
                        for(int i = 0; i < operation.getPolygon().size() - 1; i++){
                            GPSCoordinates gpsCoordinates = operation.getPolygon().get(i);
                            vecPolygonCoordinates[i] = gpsCoordinates.getLatitude() + ";" + gpsCoordinates.getLongitude();
                        }
                        intent.putExtra("OPERATION_POLYGON", vecPolygonCoordinates);
                        startActivity(intent);
                    }
                }else{
                    // if next action is null, it means that the user wants to see the details of the operation
                    // serialize the polygon coordinates
                    String strPolygon = "[";
                    if(operation.getPolygon() != null){
                        for(GPSCoordinates coordinates : operation.getPolygon()){
                            strPolygon += "("+coordinates.getLatitude()+";"+coordinates.getLongitude()+"),";
                        }
                    }
                    if(strPolygon.endsWith(",")){
                        strPolygon = strPolygon.substring(0, strPolygon.length()-1);
                    }
                    strPolygon += "]";
                    // generate the intent and go to the new activity
                    Intent intent = new Intent(OperationsActivity.this, OperationActivity.class);
                    if(operation.getState() != null && operation.getState().equals(Operation.EnumOperationState.NOT_ACCEPTED)){
                        if(operation.getFlightComments() != null){
                            if(operation.getFlightComments().trim().equalsIgnoreCase("ANOTHER_OPERATION")){
                                intent.putExtra(OperationActivity.FLIGHT_COMMENTS, getString(R.string.exc_msg_another_operation));
                            }else if(operation.getFlightComments().trim().equalsIgnoreCase("UVR")){
                                intent.putExtra(OperationActivity.FLIGHT_COMMENTS, getString(R.string.exc_msg_uvr));
                            }
                        }
                    }
                    intent.putExtra(OperationActivity.DESCRIPTION_KEY, operation.getDescription());
                    intent.putExtra(OperationActivity.START_KEY, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(operation.getStartDatetime()));
                    intent.putExtra(OperationActivity.END_KEY, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(operation.getEndDatetime()));
                    intent.putExtra(OperationActivity.MAX_ALTITUDE_KEY, operation.getMaxAltitude() + " " + getString(R.string.str_meters));
                    intent.putExtra(OperationActivity.DRONE_KEY, operation.getDroneDescription());
                    intent.putExtra(OperationActivity.PILOT_KEY, operation.getPilotName());
                    intent.putExtra(OperationActivity.POLYGON_KEY, strPolygon);
                    startActivity(intent);
                }
            }
        });
        return cardView;
    }
}
