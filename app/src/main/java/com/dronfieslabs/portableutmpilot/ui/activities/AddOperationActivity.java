package com.dronfieslabs.portableutmpilot.ui.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class AddOperationActivity extends AppCompatActivity {

    // consts
    public static final String DATA_SEPARATOR = "#@@#";
    public static final int REQUEST_CODE_SELECT_DRONE = 1;

    // state
    private String vehicleId = null;
    private String vehicleName = null;

    // views
    private Button buttonStartDate;
    private Button buttonStartTime;
    private Button buttonEndDate;
    private Button buttonEndTime;
    private Button buttonMaxHeight;
    private SeekBar seekBarMaxHeight;
    private EditText editTextPilot;
    private TextView textViewSelectDrone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_operation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.str_add_operation);

        // bind views
        buttonStartDate = findViewById(R.id.button_start_date);
        buttonStartTime = findViewById(R.id.button_start_time);
        buttonEndDate = findViewById(R.id.button_end_date);
        buttonEndTime = findViewById(R.id.button_end_time);
        buttonMaxHeight = findViewById(R.id.button_max_height);
        seekBarMaxHeight = findViewById(R.id.seekbar_max_height);
        buttonMaxHeight.setText(seekBarMaxHeight.getProgress() + " " + getString(R.string.str_meters));
        seekBarMaxHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                buttonMaxHeight.setText(progress + " " + getString(R.string.str_meters));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        editTextPilot = findViewById(R.id.edit_text_pilot);
        final String username = SharedPreferencesUtils.getUsername(this);
        editTextPilot.setText(username);
        textViewSelectDrone = findViewById(R.id.text_view_select_drone);
        textViewSelectDrone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSelectDrone();
            }
        });

        // set default values
        Calendar calNow = Calendar.getInstance();
        buttonStartDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(calNow.getTime()));
        buttonStartTime.setText(new SimpleDateFormat("HH:mm").format(calNow.getTime()));
        calNow.add(Calendar.HOUR_OF_DAY, 2);
        buttonEndDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(calNow.getTime()));
        buttonEndTime.setText(new SimpleDateFormat("HH:mm").format(calNow.getTime()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_DRONE){
            if(resultCode == RESULT_OK){
                vehicleId = data.getStringExtra(SelectDroneActivity.PARAM_VEHICLE_ID_KEY);
                vehicleName = data.getStringExtra(SelectDroneActivity.PARAM_VEHICLE_NAME_KEY);
                textViewSelectDrone.setText(vehicleName);
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickSelectDrone(){
        Intent intent = new Intent(this, SelectDroneActivity.class);
        if(vehicleId != null){
            intent.putExtra(SelectDroneActivity.PARAM_VEHICLE_ID_KEY, vehicleId);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_DRONE);
    }

    public void onClickMaxHeight(View view){
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(10);
        numberPicker.setMaxValue(120);
        numberPicker.setValue(seekBarMaxHeight.getProgress());
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(numberPicker);
        builder.setTitle(R.string.str_maximum_altitude)
            .setPositiveButton(
                getText(R.string.str_establish),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        seekBarMaxHeight.setProgress(numberPicker.getValue());
                    }
                }
            )
            .setNegativeButton(
                getText(R.string.str_cancel),
                null
            )
            .create()
            .show();
    }

    public void onClickSetStartDate(View view){
        // get current value
        final Calendar calCurrentValue = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try{
            calCurrentValue.setTime(sdf.parse(buttonStartDate.getText().toString()));
        }catch(Exception ex){}
        // open datepickerdialog, showing the current value
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    // set the selected date on the buttonStartDate
                    Calendar calSelectedDate = Calendar.getInstance();
                    calSelectedDate.set(year, month, dayOfMonth);
                    buttonStartDate.setText(sdf.format(calSelectedDate.getTime()));
                }
            },
            calCurrentValue.get(Calendar.YEAR),
            calCurrentValue.get(Calendar.MONTH),
            calCurrentValue.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    public void onClickSetStartTime(View view){
        // get current value
        final Calendar calCurrentValue = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        try{
            calCurrentValue.setTime(sdf.parse(buttonStartTime.getText().toString()));
        }catch(Exception ex){}
        // open timepickerdialog, showing the current value
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    Calendar calSelectedTime = Calendar.getInstance();
                    calSelectedTime.set(0,0,0, hourOfDay, minute);
                    buttonStartTime.setText(sdf.format(calSelectedTime.getTime()));
                }
            },
            calCurrentValue.get(Calendar.HOUR_OF_DAY),
            calCurrentValue.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }

    public void onClickSetEndDate(View view){
        // get current value
        final Calendar calCurrentValue = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try{
            calCurrentValue.setTime(sdf.parse(buttonEndDate.getText().toString()));
        }catch(Exception ex){}
        // open datepickerdialog, showing the current value
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // set the selected date on the buttonEndDate
                        Calendar calSelectedDate = Calendar.getInstance();
                        calSelectedDate.set(year, month, dayOfMonth);
                        buttonEndDate.setText(sdf.format(calSelectedDate.getTime()));
                    }
                },
                calCurrentValue.get(Calendar.YEAR),
                calCurrentValue.get(Calendar.MONTH),
                calCurrentValue.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    public void onClickSetEndTime(View view){
        // get current value
        final Calendar calCurrentValue = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        try{
            calCurrentValue.setTime(sdf.parse(buttonEndTime.getText().toString()));
        }catch(Exception ex){}
        // open timepickerdialog, showing the current value
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Calendar calSelectedTime = Calendar.getInstance();
                        calSelectedTime.set(0,0,0, hourOfDay, minute);
                        buttonEndTime.setText(sdf.format(calSelectedTime.getTime()));
                    }
                },
                calCurrentValue.get(Calendar.HOUR_OF_DAY),
                calCurrentValue.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    public void onClickDefinePolygon(View view){
        // close keyboard
        closeKeyboard();

        // get the information from the activity
        String description = null;
        Date startDatetime = null;
        Date endDatetime = null;
        int maxAltitude = -1;
        String pilotName = null;
        String contactPhone = null;
        try{
            description = getDescription();
            startDatetime = getStartDatetime();
            endDatetime = getEndDatetime();
            // to the utm backend, we need to pass utc time, so we adjust the startdatetime using the device timezone
            int timeZoneOffsetInMilliseconds = Calendar.getInstance().getTimeZone().getRawOffset();
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDatetime);
            cal.add(Calendar.MILLISECOND, -timeZoneOffsetInMilliseconds);
            startDatetime = cal.getTime();
            cal = Calendar.getInstance();
            cal.setTime(endDatetime);
            cal.add(Calendar.MILLISECOND, -timeZoneOffsetInMilliseconds);
            endDatetime = cal.getTime();
            if(!endDatetime.after(startDatetime)){
                throw new Exception(getString(R.string.exc_msg_invalid_period));
            }
            maxAltitude = getMaxAltitude();
            pilotName = getPilotName();
            contactPhone = getContactPhone();
            if(vehicleId == null){
                throw new Exception(getString(R.string.exc_msg_drone_not_selected));
            }
        }catch(Exception ex){
            UIGenericUtils.ShowAlert(AddOperationActivity.this, getText(R.string.str_invalid_data) + "", ex.getMessage());
            return;
        }

        // serialize the information to send it to DefinePolygonOnMapActivity
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        final String data = description + DATA_SEPARATOR +
                sdf.format(startDatetime) + DATA_SEPARATOR +
                sdf.format(endDatetime) + DATA_SEPARATOR +
                maxAltitude + DATA_SEPARATOR +
                pilotName + DATA_SEPARATOR +
                contactPhone + DATA_SEPARATOR +
                vehicleId;

        // show a dialog for the user to choose the method for defining the polygon
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(50, 50,50,50);
        linearLayout.setLayoutParams(param);

        LinearLayout.LayoutParams buttonParam = new LinearLayout.LayoutParams(width, height, weight);
        buttonParam.setMargins(0,0,0,50);
        Button buttonDefineManually = new Button(new ContextThemeWrapper(this, R.style.RaisedButton), null, 0);
        buttonDefineManually.setText(R.string.str_coordinates);
        buttonDefineManually.setLayoutParams(buttonParam);
        linearLayout.addView(buttonDefineManually);
        buttonDefineManually.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickDefineManually(data);
            }
        });

        Button buttonGoogleMaps = new Button(new ContextThemeWrapper(this, R.style.RaisedButton), null, 0);
        buttonGoogleMaps.setText(R.string.str_map);
        linearLayout.addView(buttonGoogleMaps);
        buttonGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickGoogleMaps(data);
            }
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.str_define_polygon)
                .setMessage(getString(R.string.miscellaneous_def_polygon_options))
                .setView(linearLayout)
                .show();
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
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void onClickDefineManually(String data){
        UIGenericUtils.GoToActivity(
            this,
            DefinePolygonManuallyActivity.class,
            Arrays.asList(DefinePolygonManuallyActivity.PARAM_KEY, DefinePolygonManuallyActivity.DATA_SEPARATOR_KEY),
            Arrays.asList(data, DATA_SEPARATOR)
        );
    }

    private void onClickGoogleMaps(String data){
        UIGenericUtils.GoToActivity(
            this,
                DefinePolygonOnMapActivity.class,
            Arrays.asList(DefinePolygonOnMapActivity.PARAM_KEY, DefinePolygonOnMapActivity.DATA_SEPARATOR_KEY),
            Arrays.asList(data, DATA_SEPARATOR)
        );
    }

    private String getDescription() throws Exception {
        String retorno = ((EditText)findViewById(R.id.edit_text_description)).getText().toString();
        if(retorno.contains(DATA_SEPARATOR)){
            throw new Exception(getText(R.string.exc_msg_invalid_operation_description) + " " + DATA_SEPARATOR);
        }
        if(retorno.length() < 1 || retorno.length() > 50){
            throw new Exception(getString(R.string.exc_msg_invalid_operation_description__2));
        }
        return retorno;
    }

    private Date getStartDatetime() throws Exception {
        String strStartDatetime = buttonStartDate.getText() + " " + buttonStartTime.getText();
        Date ret = null;
        try{
            ret = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(strStartDatetime);
        }catch(Exception ex){
            throw new Exception(getText(R.string.exc_msg_invalid_operation_datetime) + "", ex);
        }
        return ret;
    }

    private Date getEndDatetime() throws Exception {
        String strEndDatetime = buttonEndDate.getText() + " " + buttonEndTime.getText();
        Date ret = null;
        try{
            ret = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(strEndDatetime);
        }catch(Exception ex){
            throw new Exception(getText(R.string.exc_msg_invalid_operation_datetime) + "", ex);
        }
        return ret;
    }

    private int getMaxAltitude() {
        return seekBarMaxHeight.getProgress();
    }

    private String getPilotName() throws Exception {
        String retorno = ((EditText)findViewById(R.id.edit_text_pilot)).getText().toString();
        if(retorno.contains(DATA_SEPARATOR)){
            throw new Exception(getText(R.string.exc_msg_invalid_operation_pilot) + " " + DATA_SEPARATOR);
        }
        if(retorno.length() < 1 || retorno.length() > 20){
            throw new Exception(getString(R.string.exc_msg_invalid_operation_pilot__2));
        }
        return retorno;
    }

    private String getContactPhone() throws Exception {
        String retorno = ((EditText)findViewById(R.id.edit_text_contact_phone)).getText().toString();
        if(retorno.contains(DATA_SEPARATOR)){
            throw new Exception(getText(R.string.exc_msg_invalid_operation_contact_phone) + " " + DATA_SEPARATOR);
        }

        if (retorno.length() < 1){
            throw new Exception(getString(R.string.exc_msg_invalid_operation_contact_phone__2));
        }

        try {
            int i = Integer.parseInt(retorno);
        }catch (NumberFormatException nfe){
            throw new Exception(getString(R.string.exc_msg_invalid_operation_contact_phone__3));
        }

        return retorno;
    }

    private void closeKeyboard(){
        View view = this.getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
