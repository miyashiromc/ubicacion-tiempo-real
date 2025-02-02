package com.example.sem11;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marcadorActual;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;

    private EditText edtxtLatitud, edtxtLongitud; // Referencias a los EditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar referencias a los EditText
        edtxtLatitud = findViewById(R.id.edtxt_latitud);
        edtxtLongitud = findViewById(R.id.edtxt_longitud);

        // Inicializar botón para cambiar a monitoreo
        Button btnMonitoreo = findViewById(R.id.btn_monitoreo);
        btnMonitoreo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Abrir el Activity de monitoreo
                Intent intent = new Intent(MainActivity.this, monitoreo.class);
                startActivity(intent);
            }
        });

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Coordenadas");

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar el fragmento de Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configurar actualizaciones de ubicación
        configurarActualizacionesDeUbicacion();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Mover la cámara a una ubicación predeterminada
        LatLng defaultLocation = new LatLng(-1.0126968, -79.4695096);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    private void configurarActualizacionesDeUbicacion() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10); // Actualizaciones cada 1 segundo

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    double latitud = location.getLatitude();
                    double longitud = location.getLongitude();

                    // Actualizar la base de datos de Firebase
                    actualizarFirebase(latitud, longitud);

                    // Actualizar los EditText con las coordenadas actuales
                    actualizarEditText(latitud, longitud);

                    // Actualizar el marcador en el mapa
                    actualizarMarcador(latitud, longitud);
                }
            }
        };

        // Solicitar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void actualizarFirebase(double latitud, double longitud) {
        databaseReference.child("latitud").setValue(latitud)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseUpdate", "Latitud actualizada correctamente");
                    } else {
                        Log.e("FirebaseUpdate", "Error al actualizar la latitud", task.getException());
                    }
                });

        databaseReference.child("longitud").setValue(longitud)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseUpdate", "Longitud actualizada correctamente");
                    } else {
                        Log.e("FirebaseUpdate", "Error al actualizar la longitud", task.getException());
                    }
                });
    }

    private void actualizarEditText(double latitud, double longitud) {
        edtxtLatitud.setText(String.valueOf(latitud));
        edtxtLongitud.setText(String.valueOf(longitud));
    }

    private void actualizarMarcador(double latitud, double longitud) {
        LatLng nuevaPosicion = new LatLng(latitud, longitud);
        if (marcadorActual == null) {
            marcadorActual = mMap.addMarker(new MarkerOptions()
                    .position(nuevaPosicion)
                    .title("Posición Actual"));
        } else {
            marcadorActual.setPosition(nuevaPosicion);
        }

        // Obtener el nivel de zoom actual
        float zoomActual = mMap.getCameraPosition().zoom;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, zoomActual));
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
