package com.sbhacksiii.bet.alerts;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;




public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mGoogleMap;
    private DrawerLayout drawer;
    private HashMap<String, Marker> markers;
    private DatabaseReference database;
    private String userUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);
        // hashmap to hold the markers
        markers = new HashMap<>();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // create database ref
        database = FirebaseDatabase.getInstance().getReference();

        Bundle extras = getIntent().getExtras();

        if(extras != null)
        {
            userUID = extras.getString("user_UID");
        }


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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}