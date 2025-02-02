package com.example.sem11;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
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

public class monitoreo extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marker;
    private EditText edtxtLatitud, edtxtLongitud;
    private DatabaseReference coordinatesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_monitoreo);

        // Ajustar padding para Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias a los EditText (asegúrate de que sean de solo lectura en el layout si así lo deseas)
        edtxtLatitud = findViewById(R.id.edtxt_latitud);
        edtxtLongitud = findViewById(R.id.edtxt_longitud);

        // Inicializar el fragmento del mapa y configurar la llamada asíncrona
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar la referencia a Firebase (suponiendo que las coordenadas se guardan en "Coordenadas")
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        coordinatesRef = mDatabase.child("Coordenadas");

        // Agregar un listener para actualizar los datos y el mapa en tiempo real
        coordinatesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("latitud").getValue(Double.class);
                Double lng = snapshot.child("longitud").getValue(Double.class);
                if (lat != null && lng != null) {
                    // Actualizar los EditText
                    edtxtLatitud.setText(String.valueOf(lat));
                    edtxtLongitud.setText(String.valueOf(lng));
                    // Actualizar el marcador en el mapa
                    actualizarMarcador(lat, lng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("monitoreo", "Error al leer datos de Firebase", error.toException());
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Configurar el mapa: tipo, controles, etc.
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Posicionar la cámara en una ubicación predeterminada
        LatLng defaultLocation = new LatLng(-1.0126968, -79.4695096);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    /**
     * Actualiza o crea el marcador en el mapa según las nuevas coordenadas.
     *
     * @param lat Latitud obtenida de Firebase.
     * @param lng Longitud obtenida de Firebase.
     */
    private void actualizarMarcador(double lat, double lng) {
        LatLng nuevaPosicion = new LatLng(lat, lng);
        if (marker == null) {
            marker = mMap.addMarker(new MarkerOptions().position(nuevaPosicion).title("Posición Actual"));
        } else {
            marker.setPosition(nuevaPosicion);
        }
        // Mantener el zoom actual o usar un valor fijo si se prefiere
        float zoomActual = mMap.getCameraPosition().zoom;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, zoomActual));
    }
}
