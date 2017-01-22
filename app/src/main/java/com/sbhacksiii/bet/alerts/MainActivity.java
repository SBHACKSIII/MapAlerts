package com.sbhacksiii.bet.alerts;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;




public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnMarkerClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mGoogleMap;
    private DrawerLayout drawer;
    private HashMap<String, Marker> markers;
    private DatabaseReference database;
    private String userUID;
    private FirebaseAuth auth;

    // Added by Eddie
    private Location mLastLocation;
    TextView mLat;
    TextView mLong;
    Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


//        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
//        mapFragment.getMapAsync(this);
        // hashmap to hold the markers
        markers = new HashMap<>();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // create database ref
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        Bundle extras = getIntent().getExtras();

        if(extras != null)
        {
            userUID = extras.getString("USERUID");
        }

// Added by Eddie


        GoogleMapOptions options = new GoogleMapOptions();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) this)
                    .addApi(LocationServices.API)
                    .build();
        }

//        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();

//        mLat = (TextView) findViewById(R.id.mLatText);
//        mLong = (TextView) findViewById(R.id.mLongText);


        options.mapType(GoogleMap.MAP_TYPE_NORMAL).compassEnabled(true);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        // set to respond to this class
        mGoogleMap.setOnMarkerClickListener(this);
        //user can long press to add marker
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            @Override
            public void onMapLongClick(LatLng latLng)
            {
                // empty edit fields from the old data
                ((EditText) findViewById(R.id.marker_title)).setText("");
                ((EditText) findViewById(R.id.marker_desc)).setText("");


                // create marker at that spot
                final Marker marker =  mGoogleMap.addMarker(new MarkerOptions().position(latLng));
                // add marker to hashmap
                markers.put(marker.getId(), marker);
                drawer =(DrawerLayout) findViewById(R.id.drawer_layout);
                // when user creates marker, open drawer to edit it
                drawer.openDrawer(GravityCompat.START);
                // add marker data to firebase, this means (for now) user can have blank title and description
                addMarkerInfoToFireBase(marker.getId(), userUID, "", "", latLng);
                //******************************************************************************************************************************
                // may not need to do id check here since only the user creating it can get to this delete button function
                //******************************************************************************************************************************
                // reference delete button
                Button delete_marker = (Button) findViewById(R.id.marker_delete_button);
                // when marker drawer opens, user can delete there marker
                delete_marker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        //remove marker from hashmap based on marker id
                        Marker temp_marker = markers.remove(marker.getId());
                        temp_marker.remove();
                        // remove marker from firebase
                        removeMarkerInfoToFireBase(temp_marker.getId());
                        // close drawer
                        drawer.closeDrawer(GravityCompat.START);
                    }
                });

                Button save_button = (Button) findViewById(R.id.marker_save_button);
                // when marker drawer opens, user can save there marker data
                save_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // reference data from edit text fields
                        String title = ((EditText) findViewById(R.id.marker_title)).getText().toString();
                        String desc = ((EditText) findViewById(R.id.marker_desc)).getText().toString();
                        // update current marker with data based on id
                        updateMarkerInfoToFireBase(marker.getId(), title, desc);
                        // close drawer
                        drawer.closeDrawer(GravityCompat.START);
                    }
                });
            }
        });


        // Add a marker in Sydney and move the camera
        // Added by Eddie
        LatLng sydney = new LatLng(-34, 151);
        mGoogleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


    }

    private void addMarkerInfoToFireBase(String id, String userUID, String title, String desc, LatLng coordinates)
    {
        // add data to marker field in database
        MarkerInfo marker_info = new MarkerInfo();

        marker_info.setTitle(title);
        marker_info.setDesc(desc);
        marker_info.setLatLng(coordinates);
        marker_info.setUserUID(userUID);

        database.child("markers").child(id).setValue(marker_info);
    }

    private void updateMarkerInfoToFireBase(String id, String title, String desc)
    {
        // use hashmap to update multiple fields at once
        Map newdata = new HashMap();
        newdata.put("title", title);
        newdata.put("desc", desc);

        database.child("markers").child(id).updateChildren(newdata);
    }

    private void removeMarkerInfoToFireBase(String id)
    {
        //remove marker from markers field in database
        database.child("markers").child(id).removeValue();
    }

    @Override
    public void onBackPressed() {
        // close drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // when user clicks on marker already on map
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // check if marker already exist
        if(markers.get(marker.getId()) != null)
        {
            // open drawer
            drawer.openDrawer(GravityCompat.START);
            // reference edit fields
            final EditText title = ((EditText) findViewById(R.id.marker_title));
            final EditText desc = ((EditText) findViewById(R.id.marker_desc));
            // this is firebase's way to READ data
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // grab data into hashmap or child fields
                    HashMap<String, Object> data = (HashMap<String, Object>) dataSnapshot.child("markers").child(marker.getId()).getValue();
                    // gran data
                    String title_str = (String) data.get("title");
                    String desc_set = (String) data.get("desc");
                    // set it in edit fields
                    // this is due to needing to display data for that marker each time a new one is clicked
                    title.setText(title_str);
                    desc.setText(desc_set);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            // check if current user is owner of that marker
            if(userUID.equals(getUserUID(marker.getId())))
            {
                // if yes, set delete button
                Button delete_marker = (Button) findViewById(R.id.marker_delete_button);
                // when user wants to delete marker
                delete_marker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // remove form hashma
                        Marker temp_marker = markers.remove(marker.getId());
                        temp_marker.remove();
                        // remove from firebase
                        removeMarkerInfoToFireBase(temp_marker.getId());
                        // close drawer
                        drawer.closeDrawer(GravityCompat.START);
                    }
                });

                Button save_button = (Button) findViewById(R.id.marker_save_button);
                // when user wants to update marker
                save_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // grab fields to be saved
                        String title = ((EditText) findViewById(R.id.marker_title)).getText().toString();
                        String desc = ((EditText) findViewById(R.id.marker_desc)).getText().toString();
                        // update fields in firebase
                        updateMarkerInfoToFireBase(marker.getId(), title, desc);
                        // close drawer
                        drawer.closeDrawer(GravityCompat.START);
                    }
                });

            }
            // if not the owner
            else
            {
                // hide button
                Button delete_marker = (Button) findViewById(R.id.marker_delete_button);
                delete_marker.setVisibility(View.GONE);
                Button save_button = (Button) findViewById(R.id.marker_save_button);
                save_button.setVisibility(View.GONE);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            auth.signOut();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private String getUserUID(final String id)
    {
        final String[] user_id = {null};

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // grab datato get user id
                HashMap<String, Object> data = (HashMap<String, Object>) dataSnapshot.child("markers").child(id).getValue();
                // grab data
                user_id[0] = (String) data.get("userUID");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        return user_id[0];
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
            Toast.makeText(MainActivity.this, "connected!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Did not connect!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    /**
     * Dispatch onStop() to all fragments.  Ensure all loaders are stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(MapsActivity.this, "Permission check failed", Toast.LENGTH_SHORT).show();
            return;
        }


        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLat.setText(String.valueOf(mLastLocation.getLatitude()));
            mLong.setText(String.valueOf(mLastLocation.getLongitude()));

        }
        if (mLastLocation == null) {

            mLat.setText("not working");
            mLong.setText("not working");
        }

    }



    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void refresh(View v) {
        if (mLastLocation == null) {
            mLat.setText("not working");
            mLong.setText("not working");
        } else {
            mLat.setText("not working");
            mLong.setText("not working");
        }
    }



}