package com.dronfieslabs.portableutmpilot.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.BuildConfig;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private TextView textViewUTMEndpoint;
    private TextView textViewUsername;
    private TextView textViewPassword;
    private TextView textViewUserType;
    private TextView textViewExpressRadius;
    private TextView textViewExpressDuration;
    private TextView textViewExpressVehicle;

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //--------------------------------------------- OVERRIDED METHODS ---------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.str_settings));

        String versionName = BuildConfig.VERSION_NAME;
        ((TextView)findViewById(R.id.tv_version_data)).setText(versionName);

        SwitchCompat switchCompatUTMEnable = findViewById(R.id.switch_compat_utm_enable);
        switchCompatUTMEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesUtils.updateUTMEnable(SettingsActivity.this, isChecked);
            }
        });
        textViewUTMEndpoint = findViewById(R.id.text_view_utm_endpoint);
        textViewUsername = findViewById(R.id.text_view_username);
        textViewPassword = findViewById(R.id.text_view_password);
        textViewUserType = findViewById(R.id.text_view_user_type);

        textViewExpressRadius = findViewById(R.id.text_view_express_radius);
        textViewExpressDuration = findViewById(R.id.text_view_express_duration);
        textViewExpressVehicle = findViewById(R.id.text_view_express_vehicle);

        // set UTM values
        switchCompatUTMEnable.setChecked(SharedPreferencesUtils.getUTMEnable(this));
        textViewUTMEndpoint.setText(SharedPreferencesUtils.getUTMEndpoint(this));
        textViewUsername.setText(SharedPreferencesUtils.getUsername(this));
        textViewPassword.setText(SharedPreferencesUtils.getPassword(this));
        updateUserType();

        textViewExpressDuration.setText(String.valueOf(SharedPreferencesUtils.getExpressDuration(this)));
        textViewExpressRadius.setText(String.valueOf(SharedPreferencesUtils.getExpressRadius(this)));
        textViewExpressVehicle.setText(SharedPreferencesUtils.getExpressVehicle(this));

        setCurrentLanguage();
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
    public void onDestroy(){
        super.onDestroy();
    }



    @Override
    public void onBackPressed() {
        // we go to projects activity, because this is the only activity from we can enter to the settings activity
        Intent intent = new Intent(this, MainActivity.class);
        // we remove the settingsActivity from the stack of activities
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- ONCLICK METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    public void onClickTestConnection(View view){
        String utmEndpoint = SharedPreferencesUtils.getUTMEndpoint(this);
        String username = SharedPreferencesUtils.getUsername(this);
        String password = SharedPreferencesUtils.getPassword(this);
        DronfiesUssServices dronfiesUssServices = DronfiesUssServices.getUnsafeInstanceDONOTUSE(utmEndpoint);
        if(dronfiesUssServices == null){
            Toast.makeText(this, "No se pudo establecer la conexión con el UTM", Toast.LENGTH_LONG).show();
            return;
        }
        dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
            @Override
            public void onResponse(String result, String errorMessage) {
                if(errorMessage == null){
                    Toast.makeText(SettingsActivity.this, "Successfully connected!", Toast.LENGTH_LONG).show();
                }else{
                    if(errorMessage.equals("401")){
                        Toast.makeText(SettingsActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(SettingsActivity.this, "Unable to connect to the UTM (error code: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
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
        final EditText editTextUsername_dialog = new EditText(this);
        editTextUsername_dialog.setText(textViewUsername.getText());
        linearLayout.addView(editTextUsername_dialog);
        new AlertDialog.Builder(this)
            .setTitle(R.string.str_change_username)
            .setView(linearLayout)
            .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // if we change the username, we have to logout first (in case we can)
                    String utmEndpoint = textViewUTMEndpoint.getText().toString();
                    DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(utmEndpoint);
                    if(dronfiesUssServices != null){
                        dronfiesUssServices.logout();
                    }
                    // change the username
                    String newUsername = editTextUsername_dialog.getText().toString();
                    SharedPreferencesUtils.updateUsername(SettingsActivity.this, newUsername);
                    textViewUsername.setText(newUsername);
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
        final EditText editTextPassword_dialog = new EditText(this);
        editTextPassword_dialog.setText(textViewPassword.getText());
        editTextPassword_dialog.setTransformationMethod(PasswordTransformationMethod.getInstance());

        linearLayout.addView(editTextPassword_dialog);
        new AlertDialog.Builder(this)
            .setTitle(R.string.str_change_password)
            .setView(linearLayout)
            .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // if we change the password, we have to logout first
                    String utmEndpoint = textViewUTMEndpoint.getText().toString();
                    DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(utmEndpoint);
                    if(dronfiesUssServices != null){
                        dronfiesUssServices.logout();
                    }
                    // change the password
                    String newPassword = editTextPassword_dialog.getText().toString();
                    SharedPreferencesUtils.updatePassword(SettingsActivity.this, newPassword);
                    textViewPassword.setText(newPassword);
                }
            })
            .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    public void onClickEditUserType(View view){
        final AlertDialog[] alertDialog = {null};

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);

        final Button buttonDroneOperator = new Button(this);
        buttonDroneOperator.setText(R.string.str_drone_operator);
        buttonDroneOperator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferencesUtils.updateUserIsDroneOperator(SettingsActivity.this, true);
                updateUserType();
                alertDialog[0].dismiss();
            }
        });
        linearLayout.addView(buttonDroneOperator);

        final Button buttonParaglidingPilot = new Button(this);
        buttonParaglidingPilot.setText(R.string.str_paragliding_pilot);
        buttonParaglidingPilot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferencesUtils.updateUserIsDroneOperator(SettingsActivity.this, false);
                updateUserType();
                alertDialog[0].dismiss();
            }
        });
        linearLayout.addView(buttonParaglidingPilot);
        int dp20 = UIGenericUtils.ConvertDPToPX(this, 20);
        linearLayout.setPadding(dp20, dp20, dp20, dp20);

        alertDialog[0] = new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_user_type)
                .setView(linearLayout)
                .show();
    }

    public void onClickEditUTMEndpoint(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(45, 10,50,10);
        linearLayout.setLayoutParams(param);
        final EditText editTextUTMEndpoint_dialog = new EditText(this);
        editTextUTMEndpoint_dialog.setText(textViewUTMEndpoint.getText());
        linearLayout.addView(editTextUTMEndpoint_dialog);
        new AlertDialog.Builder(this)
            .setTitle(R.string.str_change_utm_endpoint)
            .setView(linearLayout)
            .setPositiveButton(R.string.str_change, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String newUTMEndpoint = editTextUTMEndpoint_dialog.getText().toString();
                    SharedPreferencesUtils.updateUTMEndpoint(SettingsActivity.this, newUTMEndpoint);
                    textViewUTMEndpoint.setText(newUTMEndpoint);
                }
            })
            .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    public void onClickEditLanguage(View view){
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        float weight = 1.0f;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(width, height, weight);
        linearLayout.setPadding(50, 50,50,50);
        linearLayout.setLayoutParams(param);

        LinearLayout linearLayoutEnglish = new LinearLayout(this);
        linearLayoutEnglish.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutEnglish.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(linearLayoutEnglish);

        RadioGroup radioGroup = new RadioGroup(this);
        linearLayout.addView(radioGroup);

        RadioButton radioButtonEnglish = new RadioButton(this);
        radioButtonEnglish.setText("    " + getString(R.string.str_english));
        Drawable img = ContextCompat.getDrawable(this, R.drawable.flag_united_kingdom);
        img.setBounds( 0, 0, 70, 47 );
        radioButtonEnglish.setCompoundDrawables( img, null, null, null );
        LinearLayout.LayoutParams paramsRadioButton = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsRadioButton.setMargins(0, 20, 0, 20);
        radioButtonEnglish.setLayoutParams(paramsRadioButton);
        radioGroup.addView(radioButtonEnglish);

        final RadioButton radioButtonSpanish = new RadioButton(this);
        radioButtonSpanish.setText("    " + getString(R.string.str_spanish));
        img = ContextCompat.getDrawable(this, R.drawable.flag_spain);
        img.setBounds( 0, 0, 70, 47 );
        radioButtonSpanish.setLayoutParams(paramsRadioButton);
        radioButtonSpanish.setCompoundDrawables(img, null, null, null );

        radioGroup.addView(radioButtonSpanish);

        // set selected the radio button that corresponds
        String language = SharedPreferencesUtils.getAppLocale(this);
        if(language.equals("es")){
            radioButtonSpanish.setChecked(true);
        }else{
            radioButtonEnglish.setChecked(true);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.str_change_language)
                .setView(linearLayout)
                .setPositiveButton(R.string.str_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String language = "en";
                        if(radioButtonSpanish.isChecked()){
                            language = "es";
                        }
                        setAppLocale(language);
                        restartActivity();
                    }
                })
                .setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    // get the current language from sharedPreferences and shows it in the activity
    private void setCurrentLanguage(){
        String currentLocale = SharedPreferencesUtils.getAppLocale(this);
        if(currentLocale.equals("es")){
            // spanish
            ((TextView)findViewById(R.id.tv_language_data)).setText(R.string.str_spanish);
        }else{
            // english
            ((TextView)findViewById(R.id.tv_language_data)).setText(R.string.str_english);
        }
    }

    private void restartActivity(){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void setAppLocale(String localeCode){
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            conf.setLocale(new Locale(localeCode.toLowerCase()));
        }else{
            conf.locale = new Locale(localeCode.toLowerCase());
        }
        res.updateConfiguration(conf, dm);
        SharedPreferencesUtils.updateAppLocale(this, localeCode);
    }

    private void updateUserType(){
        if(SharedPreferencesUtils.getUserIsDroneOperator(this)){
            textViewUserType.setText(R.string.str_drone_operator);
        }else{
            textViewUserType.setText(R.string.str_paragliding_pilot);
        }
    }
}
