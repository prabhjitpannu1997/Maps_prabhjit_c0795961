package com.example.maps_prabhjitpannu_c0795961;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.maps_prabhjitpannu_c0795961.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    class MarkerClass {
        Marker presentMarker;
        String id;

        MarkerClass(String id) {
            this.id = id;
        }
    }


    Polygon shape;
    private LatLng myLocation;
    private Marker destinationMarker;
    private Polyline line;
    private int LOCATION_REQUEST_CODE = 1;
    private boolean initialPositionGetter = true;
    private int numberOfMarkers = 0;
    private boolean wasLocationRemoved = false;
    private int positionOfRemoval = -5;
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private MarkerClass markers[];
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        // creating array of markers for markers on map to make polygon
        markers = new MarkerClass[4];
        markers[0] = new MarkerClass("A");
        markers[1] = new MarkerClass("B");
        markers[2] = new MarkerClass("C");
        markers[3] = new MarkerClass("D");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                Marker temp = mMap.addMarker(new MarkerOptions().position(myLocation).title("Current Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                temp.setTag("Current Location");
                if (initialPositionGetter) {
                    initialPositionGetter = !initialPositionGetter;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,18));
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

        if (!isPermissionGranted()) {
            requestPermission();
        } else {
            setListenerToLocation();
        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                longPressListener(latLng);
            }
        });
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {

            }

            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                float distance = distanceBetweenMarkers(myLocation,marker.getPosition());
                distance = distance/1000;
                marker.setSnippet("Distance: "+distance+" Km");
                marker.setTag(locationAddress(marker.getPosition()));
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {

            }
        });
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(@NonNull Polyline polyline) {
                Toast toast = Toast.makeText(getApplicationContext(),"Distance : "+polyline.getTag()+" km",Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Toast toast = Toast.makeText(getApplicationContext(),marker.getTag()+"",Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
        });
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(@NonNull Polygon polygon) {
                float distance = 0;
                for (int i = 0;i<polylines.size();i++){
                    distance += Float.parseFloat(polylines.get(i).getTag().toString());
                }
                Toast toast = Toast.makeText(getApplicationContext(),"Total distance of route (A-B-C-D) is : "+distance+" Km",Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    void longPressListener(LatLng latLng){
        if (wasLocationRemoved){
            wasLocationRemoved = false;
            markers[positionOfRemoval].presentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(markers[positionOfRemoval].id)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            markers[positionOfRemoval].presentMarker.setTag(locationAddress(latLng));
            float distance = distanceBetweenMarkers(myLocation,latLng);
            distance = distance/1000;
            markers[positionOfRemoval].presentMarker.setSnippet("Distance is: "+distance+" Km");
            markers[positionOfRemoval].presentMarker.setDraggable(true);
            if (markers.length == numberOfMarkers) {
                drawPolygonOnMap();
            }
            positionOfRemoval = -1;
            return;
        }
        if (numberOfMarkers > 0 ){
            for (int i = 0; i < numberOfMarkers;i++){
                Marker temp = markers[i].presentMarker;
                if (distanceBetweenMarkers(temp.getPosition(),latLng) < 50){
                    temp.remove();
                    if (shape != null){
                        shape.remove();
                        shape = null;
                    }
                    for (int j = 0;j<polylines.size();j++)
                        polylines.get(j).remove();
                    polylines.clear();
                    wasLocationRemoved = true;
                    positionOfRemoval = i;
                    return;
                }
            }
        }
        drawMakerOfPolygon(latLng);
    }

    private void drawMakerOfPolygon(LatLng latLng) {
        if (markers.length == numberOfMarkers ) {
            for (int i = 0;i<markers.length;i++)
                markers[i].presentMarker.remove();
            shape.remove();
            shape = null;
            numberOfMarkers = 0;
            for (int i = 0; i<polylines.size();i++)
                polylines.get(i).remove();
            polylines.clear();
        }
        markers[numberOfMarkers].presentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(markers[numberOfMarkers].id)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        markers[numberOfMarkers].presentMarker.setTag(locationAddress(latLng));
        markers[numberOfMarkers].presentMarker.setDraggable(true);
        float distance = distanceBetweenMarkers(myLocation,latLng);
        distance = distance/1000;
        markers[numberOfMarkers].presentMarker.setSnippet("Distance: "+distance+" Km");
        numberOfMarkers += 1;
        if (markers.length == numberOfMarkers)
            drawPolygonOnMap();
    }

    private void drawPolygonOnMap() {
        PolygonOptions options = new PolygonOptions()
                .fillColor(0x6100ff00)
                .strokeColor(Color.RED)
                .strokeWidth(1);
        for (int i=0; i<numberOfMarkers; i++) {
            if (i<numberOfMarkers-1){
                drawLine(markers[i].presentMarker,markers[i+1].presentMarker);
            }
            markers[i].presentMarker.setDraggable(false);
            options.add(markers[i].presentMarker.getPosition());
        }
        drawLine(markers[3].presentMarker,markers[0].presentMarker);

        shape = mMap.addPolygon(options);
        shape.setClickable(true);
    }

    private void drawLine(Marker start,Marker end){

        line = mMap.addPolyline(new PolylineOptions()
                .color(Color.RED)
                .width(10)
                .add(start.getPosition(), end.getPosition()));
        line.setClickable(true);
        float distance = distanceBetweenMarkers(start.getPosition(),end.getPosition());
        distance = distance/1000;
        line.setTag(distance);
        polylines.add(line);
    }

    float distanceBetweenMarkers(LatLng first,LatLng second){
        float[] distance = new float[1];
        Location.distanceBetween(first.latitude,first.longitude,
                second.latitude,second.longitude,distance);
        return distance[0];
    }

    String locationAddress(LatLng latLng){
        String addressString = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            if (addressList != null && addressList.size() > 0) {
                if (addressList.get(0).getThoroughfare() != null )
                    addressString += "Street is: "+addressList.get(0).getThoroughfare()+"\n";
                if ( addressList.get(0).getSubAdminArea() != null )
                    addressString += "City is: "+addressList.get(0).getSubAdminArea()+"\n";
                if (addressList.get(0).getLocality() != null )
                    addressString += "Postal Code is: "+addressList.get(0).getLocality()+"\n";
                if (addressList.get(0).getAdminArea() != null )
                    addressString += "Province is "+addressList.get(0).getAdminArea()+"\n";
            }else{
                addressString += "\nAddress not found";
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return addressString;
    }

    boolean isPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    void setListenerToLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            setListenerToLocation();
        }
    }
}