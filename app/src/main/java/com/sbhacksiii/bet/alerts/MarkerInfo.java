package com.sbhacksiii.bet.alerts;


import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.Exclude;

public class MarkerInfo
{
    private String title;
    private LatLng latLng;
    private String desc;
    private String userUID;
    private Marker marker;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    public LatLng getLatLng() {
        return latLng;
    }


    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }


    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
