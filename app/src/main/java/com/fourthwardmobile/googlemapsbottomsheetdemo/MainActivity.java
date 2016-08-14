package com.fourthwardmobile.googlemapsbottomsheetdemo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, PlaceSelectionListener {

    /**********************************************************************************************/
    /*                                     Constants                                              */
    /**********************************************************************************************/
    private static final String TAG = MainActivity.class.getSimpleName();

    /**********************************************************************************************/
    /*                                     Local Data                                             */
    /**********************************************************************************************/
    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private LatLng mCurrentLocation;
    private Marker mCurrentMarker;

    //Bottom Sheet Views
    TextView mAddressTextView;
    TextView mCityTextView;

    //Identifier for the permission request
    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check for permissions for API 23 (Marshmallow)
        getPermissionAccessFineLocation();

        mAddressTextView = (TextView)findViewById(R.id.address_textview);
        mCityTextView = (TextView)findViewById(R.id.city_textview);

        //Get map fragment
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred.
        autocompleteFragment.setOnPlaceSelectedListener(this);

        //Build Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);

        //Remove the default "zoom to current location" button
        View mapView = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getView();
        View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
        locationButton.setVisibility(View.GONE);

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mGoogleMap.setOnMapLongClickListener(this);

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        updateLocation(latLng);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG, "onConnected()");
        Location location = null;

        if(haveLocationPermission()) {
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {

                handleNewLocation(location);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPlaceSelected(Place place) {

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),
                16));

        Log.e(TAG,"Place = " + place.getName() + " location = " + place.getLatLng().toString());
        updateLocation(place.getLatLng());

    }

    @Override
    public void onError(Status status) {

    }

    /**********************************************************************************************/
    /*                                      Private Methods                                       */
    /**********************************************************************************************/
    /**
     * Make changes to the data when the location has changed
     *
     * @param latLng new location to set
     */
    private void updateLocation(LatLng latLng) {
        mCurrentLocation = latLng;

        addMarker(); //Add marker to new location
        setStreetAddress(); //Update the street address
    }

    /**
     * Add a marker on the map at selected location
     */
    private void addMarker() {

        if (mCurrentMarker != null)
            mCurrentMarker.remove();

        MarkerOptions options = new MarkerOptions()
                .position(mCurrentLocation);
        mCurrentMarker = mGoogleMap.addMarker(options);

    }
    /**
     * Make changes to data and map when a new location has been set
     */
    private void handleNewLocation(Location location) {

        mCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        setStreetAddress();
//
//        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
//        addMarker();
        //map.moveCamera(CameraUpdateFactory.newLatLng(latLng));


        zoomToCurrentLocation();
    }
    /**
     * Zoom map camera to current location
     */

    private void zoomToCurrentLocation() {

        Log.e(TAG, "Zooming to current location " + mCurrentLocation.toString());
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation,
                16));
    }

    /**
     * Make sure the user grants location permission
     */
    private void getPermissionAccessFineLocation() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Check if we have permission to access Location Services using FINE LOCATION
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        ACCESS_FINE_LOCATION_PERMISSION_REQUEST);

            }
        }
    }
    /**
     * Make sure we have permission for location services
     * @return if have permission
     */
    private boolean haveLocationPermission() {

        //Check if we have permission to access Location Services using FINE LOCATION
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //Have Location permission
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Get the street address of latitude/longitude
     *
     * @param latLng  latitude/longitude
     * @return street address of latitude/longitude
     * @throws IOException
     */
    private Address getStreetAddress(LatLng latLng) throws IOException {

        //Get Address of current location
        Address currentAddress = null;

        if (Geocoder.isPresent()) {
            Geocoder gcd = new Geocoder(this);

            List<Address> addresses = gcd.getFromLocation(latLng.latitude,
                    latLng.longitude, 1);

            if (addresses.size() > 0)
                currentAddress = addresses.get(0);
        }

        return currentAddress;
    }

    private void setStreetAddress() {

        try {
            Address currentAddress = getStreetAddress(mCurrentLocation);
            //Just set the first choice on the address;
            mAddressTextView.setText(currentAddress.getAddressLine(0));

            //Get the city from the local of the address
            String currentCity = currentAddress.getLocality() + ", " + currentAddress.getAdminArea();
            mCityTextView.setText(currentCity);

        } catch (IOException e) {
            mAddressTextView.setText(getString(R.string.unknown_street_address));
        }
    }

}
