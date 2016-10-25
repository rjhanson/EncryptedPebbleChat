package com.ssuser.ss.sunstonechat;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Eric on 3/24/2015.
 */
public class SwipeDetector implements View.OnTouchListener {

    public static enum Action {
        LR,     // Left to Right
        RL,     // Right to left
        TB,     // Top to Bottom
        BT,     // Bottom to Top
        None    // When no Action
    }

    private static final String TAG = "SwipeDetector";
    private static final int MIN_DISTANCE = 100;
    private float downX, downY, upX, upY;
    private Action mSwipeDetected = Action.None;

    public boolean swipeDetected(){ return mSwipeDetected != Action.None; }

    public Action getAction(){ return mSwipeDetected; }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch( event.getAction() ){
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                mSwipeDetected = Action.None;
                return false; // allow other events like Click to be processed
            }
            case MotionEvent.ACTION_MOVE: {
                upX = event.getX();
                upY = event.getY();

                float deltaX = downX - upX;
                float deltaY = downY - upY;

                // horizontal swipe detection
                if(Math.abs(deltaX) > MIN_DISTANCE){
                    // Left or Right
                    if(deltaX < 0){
                        mSwipeDetected = Action.LR;
                        return true;
                    }
                    if(deltaX > 0){
                        mSwipeDetected = Action.RL;
                        return true;
                    }
                } else
                    // Vertical Swipe Detection
                    if( Math.abs(deltaY) > MIN_DISTANCE ){
                        // top or down
                        if(deltaY < 0){
                            mSwipeDetected = Action.TB;
                            return false;
                        }
                        if(deltaY > 0){
                            mSwipeDetected = Action.BT;
                            return false;
                        }
                    }
                return true;
            }
        }

        return false;
    }
}
