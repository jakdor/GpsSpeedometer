package com.jakdor.gpsspeedometer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Class defining MainActivity(main screen) behaviour
 */
public class MainActivity extends AppCompatActivity {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = () -> hide();

    /**
     * Touch listener handling setting button clicks, lunches settings activity
     */
    private static boolean settingsClick = true;
    public static void settingsClickHandler(){
        settingsClick = true;
    }

    private final View.OnTouchListener mDelayHideTouchListener = (View view, MotionEvent motionEvent) -> {
        if (settingsClick) {
            Intent preferencesIntent = new Intent(MainActivity.this, AppPreferencesActivity.class);
            startActivity(preferencesIntent);
            settingsClick = false;
        }
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }

        return false;
    };

    private GpsLocator gpsLocator;
    private LocationCalculator locationCalculator;
    private SharedPreferences preferences;

    /**
     * GUI updates scheduled every 1000ms
     */
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() { //main GUI update loop
            updateGUI();
            timerHandler.postDelayed(this, 1000);
        }
    };

    /**
     * Loads saved user settings
     */
    int prefUnitSystem;

    private void updatePreferences(){
        prefUnitSystem = Integer.valueOf(preferences.getString("unit_system", "0"));
    }

    /**
     * Updates GUI with current info
     */
    private void updateGUI(){
        ((TextView)mContentView).setText(String.format(Locale.ENGLISH, "%f\n%f\n%f\n\n%f\n%f\n%d\n\n%s",
                gpsLocator.getLatitude(),
                gpsLocator.getLongitude(),
                gpsLocator.getAltitude(),
                locationCalculator.getSpeed(prefUnitSystem == 1),
                locationCalculator.getDistanceSum(false),
                locationCalculator.getTimer(),
                locationCalculator.getAccelerometerData()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener((View view) -> toggle());

        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        gpsLocator = new GpsLocator(this);
        locationCalculator = new LocationCalculator(gpsLocator, this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        timerHandler.postDelayed(timerRunnable, 0);

        updatePreferences();
    }

    /**
     * Hides system UI shortly after lunch
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide()
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler.postDelayed(timerRunnable, 0);
        updatePreferences();
    }
}
