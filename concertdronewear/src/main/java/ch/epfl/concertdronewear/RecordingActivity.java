package ch.epfl.concertdronewear;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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


    //Control all the sensor
    private SensorManager sensorManager;

    private Sensor hr_sensor;//Optional

    //For Mouvement detection
    // Start variables
    private Sensor accelerometer;
    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;
    private int Maxcounter;
    private int Mincounter;
    private boolean mouvement;

    Button ButtonDebug;
    TextView textViewLocation;
    TextView textMouve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        ButtonDebug = findViewById(R.id.buttonDebug1);//Pour le buton debug
        textViewLocation = findViewById(R.id.Location);
        textViewLocation.setVisibility(View.INVISIBLE);

        textMouve = findViewById(R.id.textViewMouve);
        textMouve.setTextColor(Color.GREEN);
        textMouve.setVisibility(View.INVISIBLE);

        //OPTIONAL (Heart Sensor
         sensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        hr_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE); //Definition of the sensor

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

                for (Location location : locationResult.getLocations()) {
                    Log.i(TAG, (String.format("Watch Location-->Lat: %s Long: %s  Alt; %s", location.getLatitude(), location.getLongitude(),location.getAltitude())));

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



        //For motion sensor
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.0;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        Maxcounter =0;
        Mincounter=0;
        mouvement=false;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {//OPTIONAL for Heart Sensor
            TextView textViewHR = findViewById(R.id.hrSensor);//It show on the watch
            int heartRate = (int) event.values[0];
            if (textViewHR != null) textViewHR.setText(String.valueOf(event.values[0]));

            //Intnet to send the heart pulse to the Wear Service
            Intent intent = new Intent(RecordingActivity.this, WearService.class);
            intent.setAction(WearService.ACTION_SEND.HEART_RATE.name());
            intent.putExtra(WearService.HEART_RATE, heartRate);//Heart Pulse
            startService(intent);
        }

        //For mouvement sensor

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (double)Math.sqrt(x*x + y*y + z*z);
            double delta = mAccelCurrent - mAccelLast;

            if(Math.abs(delta)>0.5) {//Pour ne pas actualiser toutes les ms
                //Seul les chagements
                mAccel = mAccel * 0.9 + delta;
                // Make this higher or lower according to how much
                // motion you want to detect

                if (mAccel > 5) {
                    Maxcounter++;
                    Mincounter--;
                } else Mincounter++;


                if (Maxcounter > 20 ){
                    mouvement=true;
                    textMouve.setTextColor(Color.RED);
                    Maxcounter = 10;
                    Mincounter = 0;
                }
                //if (Maxcounter%5==0 && Mincounter > 0) Mincounter--;

                if (Mincounter > 150) {
                    mouvement=false;
                    textMouve.setTextColor(Color.GREEN);
                    Mincounter = 50;
                    Maxcounter = 0;
                }
                //if (Mincounter%10 == 0 && Maxcounter > 0) Maxcounter--;
                Log.i(TAG, String.format("Accel: [%s]-->Pos X: [%s] Y: [%s]  Z; [%s]", mAccel, x, y, z));
                Log.i(TAG, String.format("Debug Counter MaxCounter +[%s] MinCounter -[%s]", Maxcounter, Mincounter));
                //textMouve.setText(String.format("X: [%s]\n Y: [%s]\n  Z; [%s]", x, y,z));

                textMouve.setText(String.format("Accel: [%s]", mAccel));

                //INTENT
                //Creation of the Intent to WearService that will send to the tablette
                Intent intentAccel = new Intent(this, WearService.class);
                intentAccel.setAction(WearService.ACTION_SEND.ACCELERATION.name());
                intentAccel.putExtra(WearService.ACCELERATIONVAR, mAccel);//Acceleartion
                intentAccel.putExtra(WearService.MOUVEMENT, mouvement);//LATITUDE
                startService(intentAccel);
            }

         }

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
        sensorManager.registerListener( this, hr_sensor, SensorManager.SENSOR_DELAY_UI); //Sensor Register Listener
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {//When stop, Stop the location updates
        super.onPause();
        sensorManager.unregisterListener(RecordingActivity.this);//Unregister all the sensor
//        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        sensorManager.unregisterListener(RecordingActivity.this);
        stopLocationUpdates();
    }

    public void onClickDebug(View view) {
        ButtonDebug.setVisibility(View.INVISIBLE);
        textViewLocation.setVisibility(View.VISIBLE);
        textMouve.setVisibility(View.VISIBLE);
    }
}
