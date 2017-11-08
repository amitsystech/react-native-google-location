package com.timhagn.rngloc;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by benjakuben on 12/17/14.
 */
public class LocationProvider implements android.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {



    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int TEN_MINUTES = 1000 * 60 * 2;
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000;
    private static final long INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 1000;
    private static final float MIN_DISTANCE_BETWEEN_UPDATES = 10.0f;

    private int TASK_DELAY = TEN_MINUTES;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private LocationManager locationManager;

    private Runnable task;

    /**
     * Location Callback interface to be defined in Module
     */
    public abstract interface LocationCallback {
        public abstract void handleNewLocation(Location location);
    }

    // Unique Name for Log TAG
    public static final String TAG = LocationProvider.class.getSimpleName();
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Location Callback for later use
    private LocationCallback mLocationCallback;
    // Context for later use
    private Context mContext;
    // Main Google API CLient (Google Play Services API)
    private GoogleApiClient mGoogleApiClient;
    // Location Request for later use
    private LocationRequest mLocationRequest;
    // Are we Connected?
    public Boolean connected;

    public LocationProvider(Context context, LocationCallback updateCallback) {
        // Save current Context
        mContext = context;
        // Save Location Callback
        this.mLocationCallback = updateCallback;
        // Initialize connection "state"
        connected = false;

        // First we need to check availability of play services
        if (checkPlayServices()) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            }

        locationManager = (LocationManager)mContext.getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Method to verify google play services on the device
     * */
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Log.i(TAG, GooglePlayServicesUtil.getErrorString(resultCode));
            } else {
                Log.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    /**
     * Connects to Google Play Services - Location
     */
    public void connect() {
        requestLocationUpdates();
    }

    /**
     * Disconnects to Google Play Services - Location
     */
    public void disconnect() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

//    @Override
//    public void onConnected(Bundle bundle) {
//        Log.i(TAG, "Location services connected.");
//        // We are Connected!
//        connected = true;
//        // First, get Last Location and return it to Callback
//        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if (location != null) {
//            mLocationCallback.handleNewLocation(location);
//        }
//
//        // Create the LocationRequest object
//        mLocationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setInterval(1000)        // 10 seconds, in milliseconds
//                .setFastestInterval(1000);     // 1 second, in milliseconds
//
//        // Now request continuous Location Updates
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.i(TAG, "Location services suspended...");
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        /*
//         * Google Play services can resolve some errors it detects.
//         * If the error has a resolution, try sending an Intent to
//         * start a Google Play services activity that can resolve
//         * error.
//         */
//        if (connectionResult.hasResolution() && mContext instanceof Activity) {
//            try {
//                Activity activity = (Activity)mContext;
//                // Start an Activity that tries to resolve the error
//                connectionResult.startResolutionForResult(activity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
//            /*
//             * Thrown if Google Play services canceled the original
//             * PendingIntent
//             */
//            } catch (IntentSender.SendIntentException e) {
//                // Log the error
//                e.printStackTrace();
//            }
//        } else {
//            /*
//             * If no resolution is available, display a dialog to the
//             * user with the error.
//             */
//            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
//        }
//    }


    private void requestLocationUpdates() {
        if (isGooglePlayServicesAvailable() && isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (googleApiClient != null && !googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
                googleApiClient.connect();

            } else if (googleApiClient != null && googleApiClient.isConnected()) {
                requestGoogleLocationUpdates();
            }

        } else {
            requestAndroidLocationUpdates();
        }

    }

    private boolean isGooglePlayServicesAvailable() {
        return ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
    }

    @Override
    public void onLocationChanged(Location location) {
        uploadLocationData(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestGoogleLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        requestAndroidLocationUpdates();
    }


    private void requestGoogleLocationUpdates() {
        if (locationRequest == null) {
            locationRequest = new LocationRequest();
            locationRequest.setInterval(INTERVAL);
            locationRequest.setFastestInterval(FASTEST_INTERVAL);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        //removeGoogleLocationUpdates();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                locationRequest,
                this);
    }

    private void requestAndroidLocationUpdates() {
        if (locationManager == null) {
            return;
        }

        String bestProvider;
        if (isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            bestProvider = LocationManager.NETWORK_PROVIDER;

        } else if (isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            bestProvider = LocationManager.GPS_PROVIDER;

        } else if (isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            bestProvider = LocationManager.PASSIVE_PROVIDER;

        } else if (locationManager.getBestProvider(new Criteria(), true) != null) {
            bestProvider = locationManager.getBestProvider(new Criteria(), true);

        } else {
            Location betterLocation;
            Location lastKnownGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (isBetterLocation(lastKnownGPSLocation, lastKnownNetworkLocation)) {
                betterLocation = lastKnownGPSLocation;

            } else {
                betterLocation = lastKnownNetworkLocation;
            }

            uploadLocationData(betterLocation);
            return;
        }

        removeAndroidLocationUpdates();
        locationManager.requestLocationUpdates(
                bestProvider,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_BETWEEN_UPDATES,
                this);
    }

    private void removeAndroidLocationUpdates() {
        if (locationManager == null) {
            return;
        }

        locationManager.removeUpdates(this);
    }

    private void removeGoogleLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    private boolean isProviderEnabled(String provider) {
        return locationManager != null && locationManager.isProviderEnabled(provider);
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(
                location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;

        } else if (isNewer && !isLessAccurate) {
            return true;

        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }

        return false;
    }

    private void uploadLocationData(Location location) {
        if (location == null) {
            return;
        }
        mLocationCallback.handleNewLocation(location);

    }
}
