package com.dronfieslabs.portableutmpilot.ui.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dronfieslabs.portableutmpilot.R;

import java.util.List;

public class UIGenericUtils {


    //-----------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------- PUBLIC METHODS  ---------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------

    public static void ShowToast(Context context, String message){
        String m = "null";
        if(message != null){
            m = message;
        }
        Toast.makeText(context, m, Toast.LENGTH_LONG).show();
    }

    //show an alert with a message and a title
    public static void ShowAlert(Context context, String title, String message){
        ShowAlert(context, title, message, null);
    }

    public static void ShowAlert(Context context, String title, String message, DialogInterface.OnDismissListener onDismissListener){
        Builder builder = new Builder(context);
        if(title != null){
            builder.setTitle(title);
        }
        if(message != null){
            builder.setMessage(message);
        }
        if(onDismissListener != null){
            builder.setOnDismissListener(onDismissListener);
        }
        builder.show();
    }

    //show an alert with the error icon next to the title
    public static void ShowErrorAlertWithOkButton(Context context, String title, String message, String okButtonText, OnClickListener okButtonListener){
        ShowAlertWithOkCancelButtonsAndIcon(context, title, message, okButtonText, okButtonListener, null, null, R.drawable.ic_error_black_24dp);
    }

    public static void ShowConfirmationAlert(Context context, String title, String message, String okButtonText, OnClickListener okListener, String cancelButtonText){
        ShowAlertWithOkCancelButtonsAndIcon(context, title, message, okButtonText, okListener, cancelButtonText, null, null);
    }

    public static void ShowConfirmationAlert(Context context, String title, String message, String okButtonText, OnClickListener okListener, String cancelButtonText, OnClickListener cancelListener){
        ShowAlertWithOkCancelButtonsAndIcon(context, title, message, okButtonText, okListener, cancelButtonText, cancelListener, null);
    }

    public static void GoToActivity(Context context, Class activityClass){
        GoToActivity(context, activityClass, null, null);
    }

    public static void GoToActivity(Context context, Class activityClass, List<String> paramNames, List<String> paramValues){
        Intent intent = new Intent(context, activityClass);
        if(paramNames != null){
            if(paramValues != null){
                if(paramNames.size() != paramValues.size()){
                    throw new RuntimeException("paramNames and paramValues have different lenght");
                }
                for(int i = 0; i < paramNames.size(); i++){
                    String paramName = paramNames.get(i);
                    String paramValue = paramValues.get(i);
                    intent.putExtra(paramName, paramValue);
                }
            }else{
                throw new RuntimeException("paramNames and paramValues have different lenght");
            }
        }
        context.startActivity(intent);
    }

    // add on top of the relativeLayout received a linearLayout with a progress bar, and return it so the user
    // can remove this linearLayout when the wait finished
    public static LinearLayout ShowProgressBar(final RelativeLayout relativeLayout){
        LinearLayout linearLayout = new LinearLayout(relativeLayout.getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setBackgroundColor(Color.BLACK);
        linearLayout.setClickable(true);
        linearLayout.setAlpha(0.5f);
        // we set a high elevation to guarantee that the layout is going to be putted over the rest of the views
        linearLayout.setElevation(100);
        ProgressBar progressBar = new ProgressBar(relativeLayout.getContext());
        LinearLayout.LayoutParams lyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lyParams.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(lyParams);
        linearLayout.addView(progressBar);
        relativeLayout.addView(linearLayout);
        return linearLayout;
    }

    //-----------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------- PRIVATE METHODS ---------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------------


    //show an alert with a message and an ok button
    //if iconId == null, show alert without icon
    private static void ShowAlertWithOkCancelButtonsAndIcon(Context context, String title, String message, String okButtonText, OnClickListener okButtonListener, String cancelButtonText, OnClickListener cancelListener, Integer iconId){
        Builder builder = new Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(okButtonText, okButtonListener);
        //agrego el boton de cancelacion
        if(cancelButtonText != null && !cancelButtonText.trim().isEmpty()){
            builder.setNegativeButton(cancelButtonText, cancelListener);
        }
        //creo el dialog
        AlertDialog alertDialog = builder.create();
        //agrego el title
        if(title != null && !title.trim().isEmpty()){
            alertDialog.setTitle(title);
        }
        //agrego el icon
        if(iconId != null){
            alertDialog.setIcon(iconId);
        }
        alertDialog.show();
    }
}
