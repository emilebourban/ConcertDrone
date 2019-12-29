package ch.epfl.concertdronewear;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class RecordingActivity extends WearableActivity implements SensorEventListener {

    public static final String STOP_ACTIVITY = "STOP_ACTIVITY";
    private static final String TAG = "RecordingActivity";
    //Objetc to create fto get the Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        //OPTIONAL (Heart Sensor
        final SensorManager sensorManager =
                (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        Sensor hr_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE); //Definition of the sensor
        sensorManager.registerListener( this, hr_sensor, SensorManager.SENSOR_DELAY_UI); //Sensor Register Listener

        //ASK permision to use sensor Heart  (when activate it) (OPTIONAL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission("android.permission.BODY_SENSORS")
                == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.BODY_SENSORS"}, 0);
        }

        //Ask for permision for localisation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission("android" + ""
                + ".permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") ==
                        PackageManager.PERMISSION_DENIED || checkSelfPermission("android" + "" +
                ".permission.INTERNET") == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android"
                    + ".permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }
        //To Unregister the Sensorsss(both) when close the activity from the tablette
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sensorManager.unregisterListener(RecordingActivity.this);
                finish();
            }
        }, new IntentFilter(STOP_ACTIVITY));


        //GPS
        fusedLocationClient = new FusedLocationProviderClient(this);

        // Location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                TextView textViewLocation = findViewById(R.id.Location);
                for (Location location : locationResult.getLocations()) {
                    Log.i(TAG, (String.format("Wach Location-->Lat: %s \nLong: %s\nAlt; %s", location.getLatitude(), location.getLongitude(),location.getAltitude())));

                    textViewLocation.setText(String.format("Lat: %s \nLong: %s\nAlt; %s", location.getLatitude(), location.getLongitude(),location.getAltitude()));
                    //Creation of the Intent to WearService that will send to the tablette
                    Intent intent = new Intent(RecordingActivity.this, WearService.class);
                    intent.setAction(WearService.ACTION_SEND.LOCATION.name());
                    intent.putExtra(WearService.LONGITUDE, location.getLongitude());//LONGITUDE
                    intent.putExtra(WearService.LATITUDE, location.getLatitude());//LATITUDE
                    intent.putExtra(WearService.ALTITUDE,location.getAltitude()); //ALTITUDE
                    startService(intent);
                }
            }
        };

    }
    //OPTIONAL for Heart Sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        TextView textViewHR = findViewById(R.id.hrSensor);//It show on the watch
        int heartRate = (int) event.values[0];
        if (textViewHR != null) textViewHR.setText(String.valueOf(event.values[0]));

        //Intnet to send the heart pulse to the Wear Service
        Intent intent = new Intent(RecordingActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.HEART_RATE.name());
        intent.putExtra(WearService.HEART_RATE, heartRate);//Heart Pulse
        startService(intent);
    }

    //To start the Location upodates
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest()
                .setInterval(5)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    //To stop the location updates
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {//When On, take the location
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {//When stop, Stop the location updates
        super.onPause();
        stopLocationUpdates();
    }
}
