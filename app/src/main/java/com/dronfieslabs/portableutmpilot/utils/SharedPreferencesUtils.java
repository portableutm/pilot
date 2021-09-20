package com.dronfieslabs.portableutmpilot.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {

    private static final String SHARED_PREFERENCES_NAME = "com.dronfieslabs.portableutmpilot.SHARED_PREFERENCES";

    private static final String UTM_ENABLE_KEY = "UTM_ENABLE";
    private static final String UTM_ENDPOINT_KEY = "UTM_ENDPOINT";
    private static final String USERNAME_KEY = "USERNAME";
    private static final String PASSWORD_KEY = "PASSWORD";
    private static final String USER_IS_DRONE_OP_KEY = "USER_IS_DRONE_OP";
    private static final String APP_LOCALE_KEY = "APP_LOCALE";
    private static final String EXPRESS_RADIUS = "EXPRESS_RADIUS";
    private static final String EXPRESS_DURATION = "EXPRESS_DURATION";
    private static final String EXPRESS_VEHICLE = "EXPRESS_VEHICLE";


    // this method save into the sharedpreferences the string that represents the locale used by the app
    // this method do not update locale of the app
    public static void updateAppLocale(Context context, String newAppLocale){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(APP_LOCALE_KEY, newAppLocale);
        editor.commit();
    }

    public static String getAppLocale(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(APP_LOCALE_KEY, "en");
    }

    public static void updateUTMEndpoint(Context context, String newUTMEndpoint){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(UTM_ENDPOINT_KEY, newUTMEndpoint);
        editor.commit();
    }

    public static String getUTMEndpoint(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(UTM_ENDPOINT_KEY, "");
    }

    public static void updateUsername(Context context, String newUsername){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USERNAME_KEY, newUsername);
        editor.commit();
    }

    public static String getUsername(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(USERNAME_KEY, "");
    }

    public static void updatePassword(Context context, String newPassword){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PASSWORD_KEY, newPassword);
        editor.commit();
    }

    public static String getPassword(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(PASSWORD_KEY, "");
    }

    public static void updateUTMEnable(Context context, boolean enable){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(UTM_ENABLE_KEY, enable);
        editor.commit();
    }

    public static boolean getUTMEnable(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(UTM_ENABLE_KEY, true);
    }

    public static void updateUserIsDroneOperator(Context context, boolean isDroneOperator){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(USER_IS_DRONE_OP_KEY, isDroneOperator);
        editor.commit();
    }

    public static boolean getUserIsDroneOperator(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(USER_IS_DRONE_OP_KEY, true);
    }

    public static void updateExpressRadius(Context context, int newRadius){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(EXPRESS_RADIUS, newRadius);
        editor.commit();
    }

    public static int getExpressRadius(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(EXPRESS_RADIUS, 1);
    }

    public static void updateExpressDuration(Context context, int newDuration){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(EXPRESS_DURATION, newDuration);
        editor.commit();
    }

    public static int getExpressDuration(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(EXPRESS_DURATION, 1);
    }

    public static void updateExpressVehicle(Context context, String newVehicle){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(EXPRESS_VEHICLE, newVehicle);
        editor.commit();
    }

    public static String getExpressVehicle(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(EXPRESS_VEHICLE, "6acf14ec-8e33-4d3d-a4d9-2fa5708dcb46");
    }



    private static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
