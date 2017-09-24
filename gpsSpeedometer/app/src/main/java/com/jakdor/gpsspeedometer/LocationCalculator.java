package com.jakdor.gpsspeedometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for processing raw gps data
 */
class LocationCalculator implements SensorEventListener {

    private final int UPDATE_TIME = 1000;

    private double lastLatitude = 0;
    private double lastLongitude = 0;

    private int altitudeCounter = 0;
    private double altitudeSum = 0;
    private double avrAltitude = 0;
    private double lastAvrAltitude = 0;

    private int speedCounter = 0;
    private double speedSum = 0;
    private double avrCurrentSpeed = 0;
    private int speedStopCounter = 0;

    private double distance;
    private double distanceSum = 0;

    private long timer = 0;

    private boolean stopLock = false;

    /**
     * Uses GpsLocator class to get raw data, lunches update method on set rate
     */
    LocationCalculator(final GpsLocator gpsLocator, Context context){

        //initialize accelerometer
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                update(gpsLocator.getLatitude(),
                        gpsLocator.getLongitude(),
                        gpsLocator.getAltitude());
            }
        }, UPDATE_TIME, UPDATE_TIME);
    }

    /**
     * Main update loop;
     * - lunches distance calculation method
     * - lunches speed calculation method
     * - discards random gps input jitter
     * - starts and stops timers
     */
    private void update(double latitude, double longitude, double altitude){
        if(lastLatitude == 0 && lastLongitude == 0){
            lastLatitude = latitude;
            lastLongitude = longitude;
            return;
        }

        if(latitude != lastLatitude && longitude != lastLongitude) { //todo take gps accuracy into account
            //calculateDistanceSimple(latitude, longitude);
            calculateDistanceAdvance(latitude, longitude);
            calculateSpeed();

            if(stopLock && accelerating){ //take accelerometer into account
                stopLock = false;
            }

            if(distance > 0.4 && !stopLock) { //discard random jitter
                distanceSum += distance;
                calculateTime();
            }

            lastLatitude = latitude;
            lastLongitude = longitude;

            if (altitudeCounter < 4) { //experimental
                altitudeSum += altitude;
            } else {
                lastAvrAltitude = avrAltitude;
                avrAltitude = altitudeSum / altitudeCounter;
                altitudeCounter = 0;
                altitudeSum = 0;
            }
            ++altitudeCounter;
        }
        else{
            ++speedStopCounter;
            if(speedStopCounter == 4){ //full stop
                avrCurrentSpeed = 0;
                speedStopCounter = 0;
                stopLock = true;
            }
        }
    }

    /**
     * Simple approximate distance calculation methode
     * <Currently replaced by calculateDistanceAdvance()>
     */
    private void calculateDistanceSimple(double latitude, double longitude){
        double earthRadius = 6371000;
        double dLat = Math.toRadians(latitude - lastLatitude);
        double dLng = Math.toRadians(longitude - lastLongitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lastLatitude)) * Math.cos(Math.toRadians(latitude)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double b = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        distance = earthRadius * b;

        double altitudeDifference = Math.abs(lastAvrAltitude - avrAltitude); //experimental
        if(avrAltitude != 0 && lastAvrAltitude != 0 && altitudeDifference > 10){
            distance = Math.sqrt(Math.pow(altitudeDifference, 2.0) + Math.pow(distance, 2.0));
        }
    }

    /**
     * Advance distance calculation method based on WGS-84 ellipsoid modeling
     */
    private void calculateDistanceAdvance(double latitude, double longitude){
        double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
        double L = Math.toRadians(longitude - lastLongitude);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lastLatitude)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(latitude)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
        double lambda = L, lambdaP, interLimit = 100;

        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
                    + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0) {
                return; // co-incident points
            }

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;

            if (Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
            }

            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1 - C) * f * sinAlpha
                    * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --interLimit > 0);

        if (interLimit == 0) {
            distance = Double.NaN;
            return; // formula failed to converge
        }

        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B
                * sinSigma
                * (cos2SigmaM + B
                / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
                * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        distance = b * A * (sigma - deltaSigma);
    }

    /**
     * Simple speed calculation method based on distance and update timer
     * - averages speed output for smooth reads
     * - discards averages speed under 1km/h due to random gps jitter
     */
    private void calculateSpeed(){
        double speed = distance / (UPDATE_TIME / 1000);
        if(speedCounter < 2){
            speedSum += speed;
        }
        else {
            speedSum += speed;
            avrCurrentSpeed = speedSum / speedCounter;
            speedCounter = 0;
            speedSum = 0;
        }
        ++speedCounter;

        if(avrCurrentSpeed < 1.0){ //discard random jitter
            avrCurrentSpeed = 0.0;
        }
    }

    private void calculateTime(){
        timer += UPDATE_TIME / 1000;
    }

    double getSpeed(boolean retardedSystem){
        if(retardedSystem){ //imperial unit system conversion
            return avrCurrentSpeed * 2.2369362912;
        }
        return avrCurrentSpeed * 3.6;
    }

    double getDistanceSum(boolean retardedSystem){
        if(retardedSystem){ //imperial unit system conversion
            return distanceSum * 0.0032808399;
        }
        return distanceSum;
    }

    long getTimer(){
        return timer;
    }

    String getAccelerometerData(){
        return String.format(Locale.ENGLISH, "x: %f\ny: %f\nz: %f \n acc: %b",
                accValues[0], accValues[1], accValues[2], accelerating);
    }

    /**
     * Detect acceleration
     */
    private boolean accelerating = false;
    private float accValues[] = { 0.0f, 0.0f, 0.0f };
    private float lastAccValues[] = { 0.0f, 0.0f, 0.0f };
    private long lastUpdate = 0;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final long ACCELERATION_THRESHOLD = 20;

        Sensor sensor = sensorEvent.sensor;

        if(sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accValues[0] = sensorEvent.values[0];
            accValues[1] = sensorEvent.values[1];
            accValues[2] = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(accValues[0] + accValues[1] + accValues[2] -
                        lastAccValues[0] - lastAccValues[1] - lastAccValues[2])/ diffTime * 10000;


                accelerating = speed > ACCELERATION_THRESHOLD;

                lastAccValues[0] = accValues[0];
                lastAccValues[1] = accValues[1];
                lastAccValues[2] = accValues[2];
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
