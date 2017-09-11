package com.jakdor.gpsspeedometer;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Class providing access to gps module
 */
class GpsLocator {

    private final long LOCATION_REFRESH_TIME = 0;
    private final float LOCATION_REFRESH_DISTANCE = 0;

    private Context context;
    private LocationManager locationManager;
    private Criteria criteria;
    private String bestProvider;
    static private Location location;

    /**
     * provides access to gps location manager
     * default setting: best accuracy / gps module
     */
    GpsLocator(Context context){
        this.context = context;
        locationManager = (LocationManager) this.context.getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = locationManager.getBestProvider(criteria, false);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, mLocationListener);
            location = locationManager.getLastKnownLocation(bestProvider);
        }
        catch (SecurityException e){
            Log.e("Exception", "GPS permission problem: " + e.toString());
        }
    }

    double getLatitude(){
        double output = 0;
        try {
            output = location.getLatitude();
        }
        catch (NullPointerException e){
            Log.e("Exception", "Problem with getting gps pos: " + e.toString());
        }

        return output;
    }

    double getLongitude(){
        double output = 0;
        try {
            output = location.getLongitude();
        }
        catch (NullPointerException e){
            Log.e("Exception", "Problem with getting gps pos: " + e.toString());
        }

        return output;
    }

    double getAltitude(){
        double output = 0;
        try {
            output = location.getAltitude();
        }
        catch (NullPointerException e){
            Log.e("Exception", "Problem with getting gps pos: " + e.toString());
        }

        return output;
    }

    double getAccuracy(){
        double output = 0;
        try {
            output = location.getAccuracy();
        }
        catch (NullPointerException e){
            Log.e("Exception", "Problem with getting gps accuracy: " + e.toString());
        }

        return output;
    }

    /**
     * Listener for gps changes
     */
    private final LocationListener mLocationListener = new LocationListener(){
        @Override
        public void onLocationChanged(final Location location) {
            try {
                GpsLocator.location = locationManager.getLastKnownLocation(bestProvider);
            }
            catch (SecurityException e){
                Log.e("Exception", "GPS permission problem: " + e.toString());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}
