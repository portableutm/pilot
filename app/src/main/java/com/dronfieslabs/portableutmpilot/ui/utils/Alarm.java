package com.dronfieslabs.portableutmpilot.ui.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dronfieslabs.portableutmpilot.R;
import com.dronfieslabs.portableutmpilot.services.geometry.Polygon;
import com.dronfieslabs.portableutmpilot.ui.activities.FlightActivity;

public class Alarm {

    public enum EnumVehiclePosition{
        INSIDE_OPERATION_FAR_FROM_THE_EDGE,
        INSIDE_OPERATION_CLOSE_TO_THE_EDGE,
        OUTSIDE_OPERATION
    }

    private Activity mActivity;
    private RelativeLayout mRelativeLayoutAlarm;
    private Button mButtonDismissAlarm;
    private Polygon mOperationPolygonMercator;
    private double mOperationMaxAltitude;
    private EnumVehiclePosition mVehicleLastPosition;
    private EnumVehiclePosition mVehicleCurrentPosition;
    private ValueAnimator mValueAnimatorAlarm;
    private MediaPlayer mMediaPlayerBeepBuffer;
    private MediaPlayer mMediaPlayerBeepOutside;
    
    public Alarm(Activity activity, RelativeLayout relativeLayoutAlarm, Button buttonDismissAlarm, Polygon operationPolygonMercator, double operationMaxAltitude){
        mActivity = activity;
        mRelativeLayoutAlarm = relativeLayoutAlarm;
        mButtonDismissAlarm = buttonDismissAlarm;
        mOperationPolygonMercator = operationPolygonMercator;
        mOperationMaxAltitude = operationMaxAltitude;
        mVehicleLastPosition = null;
        mVehicleCurrentPosition = null;
        mValueAnimatorAlarm = new ValueAnimator();
        mValueAnimatorAlarm.setDuration(1000);
        mValueAnimatorAlarm.setEvaluator(new ArgbEvaluator());
        mValueAnimatorAlarm.setIntValues(0, 100, 0);
        mValueAnimatorAlarm.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(mVehicleCurrentPosition == EnumVehiclePosition.OUTSIDE_OPERATION){
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 0,0));
                }else{
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb((int)animation.getAnimatedValue(), 255, 255,0));
                }
            }
        });
        mValueAnimatorAlarm.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimatorAlarm.setRepeatMode(ValueAnimator.RESTART);
        mMediaPlayerBeepBuffer = MediaPlayer.create(mActivity, R.raw.beep);
        mMediaPlayerBeepBuffer.setLooping(true);
        mMediaPlayerBeepOutside = MediaPlayer.create(mActivity, R.raw.beep2);
        mMediaPlayerBeepOutside.setLooping(true);
    }
    
    public void updateVehiclePosition(double vehicleLat, double vehicleLng, double vehicleAlt){
        mVehicleLastPosition = mVehicleCurrentPosition;

        if(Utils.pointIsInsideOperationVolume(vehicleLat, vehicleLng, vehicleAlt, mOperationPolygonMercator, mOperationMaxAltitude)){
            // calculate distance between dron and the polygon sides
            final double minDistanceBetweenDronePositionAnPolygonSidesInMeters = Utils.getMinDistanceBetweenPointAndVolumeSides(vehicleLat, vehicleLng, vehicleAlt, mOperationPolygonMercator, mOperationMaxAltitude);
            if (minDistanceBetweenDronePositionAnPolygonSidesInMeters < 10) {
                mVehicleCurrentPosition = EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE;
            }else{
                mVehicleCurrentPosition = EnumVehiclePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE;
            }
        }else{
            mVehicleCurrentPosition = EnumVehiclePosition.OUTSIDE_OPERATION;
        }

        // if mVehicleLastPosition is null, it means it is the first time this method is executed
        // if mVehicleLastPosition is null and mVehicleCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
        // we have to play beep buffer and show dismiss button
        if(mVehicleLastPosition == null && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
            playBeep(mMediaPlayerBeepBuffer);
            startValueAnimatorAlarm();
            changeDismissButtonVisibility(View.VISIBLE);
        }
        // if mVehicleLastPosition is null and mVehicleCurrentPosition is OUTSIDE_OPERATION
        // we have to play beep outside
        else if(mVehicleLastPosition == null && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
            playBeep(mMediaPlayerBeepOutside);
            startValueAnimatorAlarm();
        }
        // if mVehicleLastPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE and mVehicleCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
        // we have to play beep buffer and show dismiss button
        else if(mVehicleLastPosition == EnumVehiclePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
            playBeep(mMediaPlayerBeepBuffer);
            startValueAnimatorAlarm();
            changeDismissButtonVisibility(View.VISIBLE);
        }
        // if mVehicleLastPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE and mVehicleCurrentPosition is OUTSIDE_OPERATION
        // we have to play beep outside
        else if(mVehicleLastPosition == EnumVehiclePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE && mVehicleCurrentPosition == EnumVehiclePosition.OUTSIDE_OPERATION){
            playBeep(mMediaPlayerBeepOutside);
            startValueAnimatorAlarm();
        }
        // if mVehicleLastPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE and mVehicleCurrentPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE
        // we have to stop beep buffer and hide dismiss button
        else if(mVehicleLastPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE){
            stopBeep(mMediaPlayerBeepBuffer);
            stopValueAnimatorAlarm();
            changeDismissButtonVisibility(View.INVISIBLE);
        }
        // if mVehicleLastPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE and mVehicleCurrentPosition is OUTSIDE_OPERATION
        // we have to stop beep buffer, play beep outside and hide dismiss button
        else if(mVehicleLastPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE && mVehicleCurrentPosition == EnumVehiclePosition.OUTSIDE_OPERATION){
            stopBeep(mMediaPlayerBeepBuffer);
            playBeep(mMediaPlayerBeepOutside);
            startValueAnimatorAlarm();
            changeDismissButtonVisibility(View.INVISIBLE);
        }
        // if mVehicleLastPosition is OUTSIDE_OPERATION and mVehicleCurrentPosition is INSIDE_OPERATION_CLOSE_TO_THE_EDGE
        // we have to stop beep outside, play beep buffer and show dismiss button
        else if(mVehicleLastPosition == EnumVehiclePosition.OUTSIDE_OPERATION && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_CLOSE_TO_THE_EDGE){
            stopBeep(mMediaPlayerBeepOutside);
            playBeep(mMediaPlayerBeepBuffer);
            startValueAnimatorAlarm();
            changeDismissButtonVisibility(View.VISIBLE);
        }
        // if mVehicleLastPosition is OUTSIDE_OPERATION and mVehicleCurrentPosition is INSIDE_OPERATION_FAR_FROM_THE_EDGE
        // we have to stop beep outside
        else if(mVehicleLastPosition == EnumVehiclePosition.OUTSIDE_OPERATION && mVehicleCurrentPosition == EnumVehiclePosition.INSIDE_OPERATION_FAR_FROM_THE_EDGE){
            stopBeep(mMediaPlayerBeepOutside);
            stopValueAnimatorAlarm();
        }
    }

    public void dismiss(){
        stopBeep(mMediaPlayerBeepBuffer);
        stopValueAnimatorAlarm();
        changeDismissButtonVisibility(View.INVISIBLE);
    }

    public void stopBeep(){
        stopBeep(mMediaPlayerBeepBuffer);
        stopBeep(mMediaPlayerBeepOutside);
    }

    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //---------------------------------------------- PRIVATE METHODS ----------------------------------------------
    //-------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void changeDismissButtonVisibility(final int visibility){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonDismissAlarm.setVisibility(visibility);
            }
        });
    }

    private void playBeep(final MediaPlayer mediaPlayer){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }

    private void stopBeep(final MediaPlayer mediaPlayer){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    private void startValueAnimatorAlarm(){
        if(!mValueAnimatorAlarm.isRunning()){
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mValueAnimatorAlarm.start();
                }
            });
        }
    }

    private void stopValueAnimatorAlarm(){
        if(mValueAnimatorAlarm.isRunning()){
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mValueAnimatorAlarm.cancel();
                    mRelativeLayoutAlarm.setBackgroundColor(Color.argb(0, 0, 0,0));
                }
            });
        }
    }

}
