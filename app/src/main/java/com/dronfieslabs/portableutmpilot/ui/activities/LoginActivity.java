package com.dronfieslabs.portableutmpilot.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;

public class LoginActivity extends AppCompatActivity {

    // state
    private boolean mDroneOperator = true;

    // views
    private EditText mEditTextUsername;
    private EditText mEditTextPassword;
    private TextView mTextViewDroneOperator;
    private TextView mTextViewParaglidingPilot;
    private Button mButtonSignIn;
    private TextView mTextViewSignUp;
    private TextView mTextViewSkipLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // views binding
        mEditTextUsername = findViewById(R.id.edit_text_username);
        mEditTextPassword = findViewById(R.id.edit_text_password);
        mTextViewDroneOperator = findViewById(R.id.text_view_drone_operator);
        mTextViewDroneOperator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickDroneOperator();
            }
        });
        mTextViewParaglidingPilot = findViewById(R.id.text_view_paraglinding_pilot);
        mTextViewParaglidingPilot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickParaglidingPilot();
            }
        });
        mButtonSignIn = findViewById(R.id.button_sign_in);
        mButtonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSignIn();
            }
        });
        mTextViewSignUp = findViewById(R.id.text_view_sign_up);
        mTextViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSignUp();
            }
        });
        mTextViewSkipLogin = findViewById(R.id.text_view_skip_login);
        mTextViewSkipLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSkipLogin();
            }
        });
    }

    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------
    //---------------------------------------- EVENT HANDLERS ----------------------------------------
    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------

    private void onClickDroneOperator(){
        mDroneOperator = true;
        updateDroneParaglidingButtons();
    }

    private void onClickParaglidingPilot(){
        mDroneOperator = false;
        updateDroneParaglidingButtons();
    }

    private void onClickSignIn(){
        String utmEndpoint = SharedPreferencesUtils.getUTMEndpoint(this);
        final String username = mEditTextUsername.getText().toString();
        final String password = mEditTextPassword.getText().toString();
        DronfiesUssServices dronfiesUssServices = DronfiesUssServices.getInstance(utmEndpoint);
        if(dronfiesUssServices == null){
            Toast.makeText(this, "No se pudo establecer la conexión con el UTM", Toast.LENGTH_LONG).show();
            return;
        }
        dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
            @Override
            public void onResponse(String result, String errorMessage) {
                if(errorMessage != null){
                    if(errorMessage.equals("401")){
                        Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                        return;
                    }else{
                        Toast.makeText(LoginActivity.this, "Unable to connect to the UTM (error code: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                // save credentials and user type
                SharedPreferencesUtils.updateUsername(LoginActivity.this, username);
                SharedPreferencesUtils.updatePassword(LoginActivity.this, password);
                SharedPreferencesUtils.updateUserIsDroneOperator(LoginActivity.this, mDroneOperator);
                // go to main activity
                UIGenericUtils.GoToActivity(LoginActivity.this, MainActivity.class);
            }
        });
    }

    private void onClickSignUp(){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://161.35.12.214/registration"));
        startActivity(browserIntent);
    }

    private void onClickSkipLogin(){
        UIGenericUtils.GoToActivity(this, MainActivity.class);
    }

    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------
    //---------------------------------------- EVENT HANDLERS ----------------------------------------
    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------

    private void updateDroneParaglidingButtons(){
        if(mDroneOperator){
            mTextViewDroneOperator.setBackground(getDrawable(R.drawable.i_am_a_dron_operator_background_active));
            mTextViewDroneOperator.setTextColor(getColor(R.color.white));
            mTextViewParaglidingPilot.setBackground(getDrawable(R.drawable.i_am_a_paragliding_pilot_background));
            mTextViewParaglidingPilot.setTextColor(getColor(R.color.colorPrimary));
        }else{
            mTextViewDroneOperator.setBackground(getDrawable(R.drawable.i_am_a_dron_operator_background));
            mTextViewDroneOperator.setTextColor(getColor(R.color.colorPrimary));
            mTextViewParaglidingPilot.setBackground(getDrawable(R.drawable.i_am_a_paragliding_pilot_background_active));
            mTextViewParaglidingPilot.setTextColor(getColor(R.color.white));
        }
    }

}