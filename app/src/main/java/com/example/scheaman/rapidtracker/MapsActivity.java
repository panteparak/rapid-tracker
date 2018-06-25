package com.example.scheaman.rapidtracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSIONS_REQUEST = 1;

    private GoogleMap mMap;
    final String path = "location/" + FirebaseAuth.getInstance().getCurrentUser().getUid();
    final String users = "location/";
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference(users);
    ValueEventListener refListener = null;
    ValueEventListener userRefListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        } else {
            requestLocationUpdates();
        }
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        refListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                GenericTypeIndicator<Map<String,Object>> t = new GenericTypeIndicator<Map<String,Object>>(){};
                Map<String,Object> location = dataSnapshot.getValue(t);
                Log.d("Retrieved", "Value is: " + location);

                // Add a marker and move the camera
                double lat = Double.parseDouble(location.get("latitude").toString());
                double lng = Double.parseDouble(location.get("longitude").toString());
                LatLng current = new LatLng(lat, lng);



                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(current).title("ME"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Ignore
            }
        });

        userRefListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String,Map<String,Object>>> t = new GenericTypeIndicator<Map<String,Map<String,Object>>>(){};

                Map<String,Map<String,Object>> location = dataSnapshot.getValue(t);

                for(Object node : location.keySet()){
                    if(! node.toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){

                        Double lat = Double.parseDouble(location.get(node).get("latitude").toString());
                        Double lng = Double.parseDouble(location.get(node).get("longitude").toString());

                        LatLng current = new LatLng(lat, lng);
                        mMap.addMarker(new MarkerOptions()
                                .position(current)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .title(node.toString()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Ignore
            }
        });
    }



    private void requestLocationUpdates() {
        final String path = "location/" + FirebaseAuth.getInstance().getCurrentUser().getUid();
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            LocationRequest request = new LocationRequest();
            request.setInterval(10000);
            request.setFastestInterval(5000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();

                    if (location != null) {
                        ref.setValue(location);
                    }
                }
            }, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (userRefListener != null && usersRef != null){
            Log.d("MapDestroy", "usersRef");
            usersRef.removeEventListener(userRefListener);
        }

        if (refListener != null && ref != null){
            Log.d("MapDestroy", "ref");
            ref.removeEventListener(refListener);
        }
    }
}
