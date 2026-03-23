package com.example.areaname;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextToSpeech tts;
    private android.widget.TextView areaTextView;
    private String lastArea = "";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        areaTextView = findViewById(R.id.areaText);

        // 1. Wake up the voice engine
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
            }
        });

        // 2. Wake up the GPS tracker
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 3. FORCE THE POP-UP for Notifications and Location
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            }, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }

        // 4. Start tracking if permission is already granted
        // 4. Always wire the buttons. Check permission INSIDE the start button.
        findViewById(R.id.btnStart).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                areaTextView.setText("Grant permission first, bro.");
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(v -> {
            stopService(new Intent(this, LocationService.class));
            areaTextView.setText("Tracker is OFF.");
        });
    }

    @SuppressLint("SetTextI18n")
    private void startTracking() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        areaTextView.setText("Tracker running in background.");
    }

    private void checkAreaChange(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {

                String currentArea = addresses.get(0).getSubLocality(); // Gets neighborhood name
                if (currentArea != null) {
                    areaTextView.setText("Current Area: " + currentArea);
                } else {
                    areaTextView.setText("Searching for GPS signal...");
                }
                // If it's a valid area and
                //
                //
                // different from the last one, speak!
                if (currentArea != null && !currentArea.equals(lastArea)) {
                    tts.speak("You are now in " + currentArea, TextToSpeech.QUEUE_FLUSH, null, null);
                    lastArea = currentArea; // Update memory
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}