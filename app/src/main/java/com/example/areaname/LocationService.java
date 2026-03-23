package com.example.areaname;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedClient;
    private TextToSpeech tts;
    private String lastArea = "";

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("en", "IN"));
        });
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Create a BRAND NEW channel so Android registers the HIGH importance
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("loc_channel_alert", "Area Alerts", NotificationManager.IMPORTANCE_HIGH);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // 2. Build the initial sticky notification using the new channel
        Notification notification = new NotificationCompat.Builder(this, "loc_channel_alert")
                .setContentTitle("Area Tracker Active")
                .setContentText("Running in background...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Forces the pop-up
                .build();

        startForeground(1, notification);
        startTracking();

        return START_STICKY;
    }

    private void startTracking() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build();
        try {
            fusedClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    for (Location loc : result.getLocations()) checkArea(loc.getLatitude(), loc.getLongitude());
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void checkArea(double lat, double lon) {
        try {
            List<Address> addresses = new Geocoder(this, Locale.getDefault()).getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String area = addresses.get(0).getSubLocality();
                if (area != null && !area.equals(lastArea)) {

                    // 1. Speak the area out loud
                    tts.speak("You are now in " + area, TextToSpeech.QUEUE_FLUSH, null, null);
                    lastArea = area;

                    // 2. Overwrite the sticky notification with the new area
                    // Replace the old NotificationCompat.Builder inside checkArea with this:
                    android.app.Notification updatedNotif = new androidx.core.app.NotificationCompat.Builder(this, "loc_channel_alert")
                            .setContentTitle("Driving Tracker")
                            .setContentText("Current Area: " + area)
                            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                            .setPriority(NotificationCompat.PRIORITY_HIGH) // Forces the pop-up every time the area changes
                            .build();

                    // Pushes the update to the notification bar (ID 1 matches our foreground service)
                    getSystemService(android.app.NotificationManager.class).notify(1, updatedNotif);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override


    public void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}