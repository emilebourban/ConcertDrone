package ch.epfl.concertdrone.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import ch.epfl.concertdrone.BuildConfig;
import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.WearService;

//Copy of the AutonomusFLight to debug //TODO We need to add the implements when passe to the true activity
public class DebugAutonomousFlightActivity extends AppCompatActivity implements LocationListener {

    //ID of the variables recive or transmited from/to the wach
    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String HEART_RATE = "HEART_RATE";

    public static final String RECEIVED_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String ALTITUDE = "ALTITUDE";

    public static final String RECEIVED_ACCELERATION = "RECEIVED_ACCELERATION";
    public static final String ACCELERATIONVAR = "ACCELERATIONVAR";
    public static final String MOUVEMENT = "MOUVEMENT";


    //Reciver of the Heart and Location Snensor Objetc creation
    private HeartRateBroadcastReceiver heartRateBroadcastReceiver;//OPTIONAL
    private LocationBroadcastReceiver locationBroadcastReceiver;//NECESSARRY
    private AccelerationBroadcastReceiver accelerationBroadcastReceiver;//NECESSARRY

    //Variable with the value of the Heart Rate (OPTIONAL)
    private int heartRateWatch = 0;
    private double acceleration =0;
    private boolean mouvement =false;


    //For the comunication of the wacht
    public static final String DEBUG_ACTIVTY_SEND =
            "DEBUG_ACTIVTY_SEND";
    private static final String TAG = "DebugAutonomousFlight" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_autonomous_flight);
        setContentView(R.layout.activity_debug_autonomous_flight);//Is a copy of the true activity
    }

    //Buton To try the correct comunication between Wacht and Tablette
    public void onClickTryComunication(View view) {
        Toast.makeText(getApplicationContext(), "Sending", Toast.LENGTH_SHORT).show();//Debug
        sendMessage("Conexion Etablie");//Send that string to the wacht to be sure that wrork
        //For debugging, it stop the Sensors comunication
        stopRecordingOnWear();
    }


    //Fontion to send a string to the wacht via Wear Service and intent
    public void sendMessage(String mensaje) {//C'est moi qui l'ai faite
        Intent intent_send = new Intent(this, WearService.class);
        intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING_DUBUG.name());//This is for debug
        //intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING.name());//For the Autonomus flight Original (no debug)
        intent_send.putExtra(DEBUG_ACTIVTY_SEND, mensaje);
        startService(intent_send);
    }

    //When click in the button Start Recording Activity--> The sensor start to get Data (for heart and location Sensor)
    public void onClickStartSensors(View view) {
        startRecordingOnWear();
    }

    //Is the fonction called to start the sensor-->It will call the Recording Activity from the wacht
    private  void startRecordingOnWear(){
        Log.i(TAG, "Launch smartwatch Sensor reading");
        Intent intentStartRec = new Intent(this, WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());//Call Command of the Wear Service
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_recordingactivity);//Start the Activity described
        this.startService(intentStartRec);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Get the HR data back from the watch
        heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver
                (heartRateBroadcastReceiver, new IntentFilter
                        (RECEIVE_HEART_RATE));
        //Get the location back from the watch
        locationBroadcastReceiver = new LocationBroadcastReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(locationBroadcastReceiver, new
                IntentFilter(RECEIVED_LOCATION));

        accelerationBroadcastReceiver = new AccelerationBroadcastReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(accelerationBroadcastReceiver, new
                IntentFilter(RECEIVED_ACCELERATION));
    }


    @Override
    protected void onPause() {
        super.onPause();
        //Stop to get sensor data-->(Save Energy)
        LocalBroadcastManager.getInstance(this).unregisterReceiver
                (heartRateBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accelerationBroadcastReceiver);
    }


    //To stop comunicating when closing the mobile app
    @Override
    protected  void onStop(){
        super.onStop();
        stopRecordingOnWear();
    }

    //Definition of the focntion called in onStop
    public void stopRecordingOnWear() {
        //It will call the comand STOPACTIVTY declared in the WaerActivity to stop the specified Actiivity
        Intent intentStopRec = new Intent(this, WearService.class);
        intentStopRec.setAction(WearService.ACTION_SEND.STOPACTIVITY.name());
        intentStopRec.putExtra(WearService.ACTIVITY_TO_STOP, BuildConfig.W_recordingactivity);
        startService(intentStopRec);
    }

    //Autogenerated (Empty fontion)
    @Override
    public void onLocationChanged(Location location) {

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

    //Senesor Recived HP (OPTIONAL)
    private class HeartRateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Show HR in a TextView
            heartRateWatch = intent.getIntExtra(HEART_RATE, -1);//Get the value of the HR
            TextView hrTextView = findViewById(R.id.textViewHP);
            hrTextView.setText(String.valueOf(heartRateWatch));
        }
    }

    //Senesor Recived Acceleration Necesarry
    private class AccelerationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Show HR in a TextView
            acceleration = intent.getDoubleExtra(ACCELERATIONVAR, -1);//Get the value of the mAccel
            mouvement = intent.getBooleanExtra(MOUVEMENT, false);//Get the value of the mouvement
            Log.i(TAG, (String.format("Recived Acceleration-->Accel: %s Mouve: %s", acceleration,mouvement)));

            TextView accelTextView = findViewById(R.id.textViewAcceleration);
            if(mouvement) accelTextView.setTextColor(Color.RED);
            else accelTextView.setTextColor(Color.GREEN);
            accelTextView.setText(String.valueOf(acceleration));
        }
    }

    //Sensor Recived Location (NECESSARRY)
    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the variables of the location from the wach.
            double longitude = intent.getDoubleExtra(LONGITUDE, -1);
            double latitude = intent.getDoubleExtra(LATITUDE, -1);
            double altitude = intent.getDoubleExtra(ALTITUDE, -1);
            Log.i(TAG, (String.format("Recived Location-->Lat: %s Long: %s  Alt; %s", latitude, longitude,altitude)));

            //Update the text view for debugging
            TextView longitudeTextView = findViewById(R.id.textViewLongitude);
            longitudeTextView.setText(String.valueOf(longitude));

            TextView latitudeTextView = findViewById(R.id.textViewLatitude);
            latitudeTextView.setText(String.valueOf(latitude));

            TextView altitudeTextView = findViewById(R.id.textViewAltitude);
            altitudeTextView.setText(String.valueOf(altitude));
        }
    }
}
