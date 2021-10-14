package com.dronfieslabs.portableutmpilot.ui.activities;

import static com.dronfieslabs.portableutmpilot.utils.UtilsOps.getDronfiesUssServices;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dronfies.portableutmandroidclienttest.Endpoint;
import com.dronfieslabs.portableutmpilot.R;
import com.dronfies.portableutmandroidclienttest.DronfiesUssServices;
import com.dronfies.portableutmandroidclienttest.entities.ICompletitionCallback;
import com.dronfieslabs.portableutmpilot.ui.Constants;
import com.dronfieslabs.portableutmpilot.ui.utils.UIGenericUtils;
import com.dronfieslabs.portableutmpilot.utils.SharedPreferencesUtils;
import com.dronfieslabs.portableutmpilot.utils.UtilsOps;
import com.google.gson.Gson;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    // views
    private RelativeLayout mRelativeLayoutRoot;
    private EditText mEditTextUsername;
    private EditText mEditTextPassword;
    private Button mButtonSignIn;
    private TextView mTextViewSignUp;
    private TextView mTextViewSkipLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // views binding
        mRelativeLayoutRoot = findViewById(R.id.relative_layout_root);
        mEditTextUsername = findViewById(R.id.edit_text_username);
        mEditTextPassword = findViewById(R.id.edit_text_password);
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

    private void onClickSignIn(){
        LinearLayout progressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
        final String username = mEditTextUsername.getText().toString();
        final String password = mEditTextPassword.getText().toString();
        String utmEndpoint = SharedPreferencesUtils.getUTMEndpoint(this);
        if(utmEndpoint == null || utmEndpoint.trim().isEmpty()){
            // we have to find the endpoint
            new Thread(() -> {
                DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(getResources().getString(R.string.portableUTMMainEndpoint));
                try{
                    String userEndpoint = dronfiesUssServices.getEndpoint(username);
                    SharedPreferencesUtils.updateUTMEndpoint(this, userEndpoint);
                    dronfiesUssServices = UtilsOps.getDronfiesUssServices(userEndpoint);
                    login(dronfiesUssServices, username, password, progressBar);
                }catch (Exception ex){
                    runOnUiThread(() -> {
                        mRelativeLayoutRoot.removeView(progressBar);
                        UIGenericUtils.ShowToast(LoginActivity.this, ex.getMessage());
                    });
                }
            }).start();
        }else{
            // we already have the endpoint, so we can login
            DronfiesUssServices dronfiesUssServices = UtilsOps.getDronfiesUssServices(utmEndpoint);
            if(dronfiesUssServices == null){
                runOnUiThread(() -> mRelativeLayoutRoot.removeView(progressBar));
                Toast.makeText(this, "No se pudo establecer la conexión con el UTM", Toast.LENGTH_LONG).show();
                return;
            }
            login(dronfiesUssServices, username, password, progressBar);
        }
    }

    private void onClickSignUp(){
        LinearLayout linearLayoutProgressBar = UIGenericUtils.ShowProgressBar(mRelativeLayoutRoot);
        new Thread(() -> {
            try{
                List<Endpoint> endpoints = UtilsOps.getDronfiesUssServices(getResources().getString(R.string.portableUTMMainEndpoint)).getEndpoints();
                runOnUiThread(() -> mRelativeLayoutRoot.removeView(linearLayoutProgressBar));
                if(endpoints == null || endpoints.isEmpty()){
                    runOnUiThread(() -> {UIGenericUtils.ShowAlert(this, getString(R.string.str_connection_error),getString(R.string.exc_msg_error_getting_endpoints));});
                    return;
                }
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                String[] endpointsArray = new String[endpoints.size()];
                for(int i = 0; i < endpoints.size(); i++){
                    endpointsArray[i] = new Gson().toJson(endpoints.get(i));
                }
                intent.putExtra(Constants.UTM_ENDPOINTS_KEY, endpointsArray);
                startActivity(intent);
            }catch (Exception ex){
                Log.d(LoginActivity.class.getName() + "_Logs", ex.getMessage(), ex);
                runOnUiThread(() -> {UIGenericUtils.ShowToast(LoginActivity.this, getString(R.string.exc_msg_another_operation));});
            }
        }).start();
    }

    private void onClickSkipLogin(){
        UIGenericUtils.GoToActivity(this, MainActivity.class);
    }

    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------
    //---------------------------------------- EVENT HANDLERS ----------------------------------------
    //------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------

    private void login(DronfiesUssServices dronfiesUssServices, String username, String password, LinearLayout progressBar){
        dronfiesUssServices.login(username, password, new ICompletitionCallback<String>() {
            @Override
            public void onResponse(String result, String errorMessage) {
                runOnUiThread(() -> mRelativeLayoutRoot.removeView(progressBar));
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
                // go to main activity
                UIGenericUtils.GoToActivity(LoginActivity.this, MainActivity.class);
            }
        });
    }

}