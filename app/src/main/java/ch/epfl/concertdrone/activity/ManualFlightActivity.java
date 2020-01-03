package ch.epfl.concertdrone.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.lang.reflect.Array;

import ch.epfl.concertdrone.BuildConfig;
import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.WearService;
import ch.epfl.concertdrone.drone.BebopDrone;
import ch.epfl.concertdrone.preprogrammed.BebopDroneRoutine;
import ch.epfl.concertdrone.view.BebopVideoView;

public class ManualFlightActivity extends AppCompatActivity implements LocationListener {

    //Pour comunication avec la Montre
    //For GPS location
    public static final String MESSAGE = "MESSAGE";

    public static final String RECEIVED_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String ALTITUDE = "ALTITUDE";

    //For accelerometer sensor
    public static final String RECEIVED_ACCELERATION = "RECEIVED_ACCELERATION";
    public static final String ACCELERATIONVAR = "ACCELERATIONVAR";
    public static final String MOUVEMENT = "MOUVEMENT";

    //Listener of the intent from the watch
    private LocationBroadcastReceiver locationBroadcastReceiver;//NECESSARRY
    private AccelerationBroadcastReceiver accelerationBroadcastReceiver;//NECESSARRY

    //Global variables of the accelerometer (default optional)
    private double acceleration =0;
    private boolean mouvement =false;

    //For the send a message (string) to the watch (optional)
    public static final String DEBUG_ACTIVTY_SEND = "DEBUG_ACTIVTY_SEND";


    // Attractive/Repulsive Behaviour variables declaration
    // Defining the number of iterations (over which we will take the mean of the acceleration values)
    //-------------
    int Niter = 10;
    //-------------
    private static int iter = 1;
    private static double sum_acc = 0;
    private static double acc_average = 0;

    //Fontion to send a string to the wacht via Wear Service and intent
    public void sendMessage(String mensaje) {//C'est moi qui l'ai faite
        Intent intent_send = new Intent(this, WearService.class);
        intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING_DUBUG.name());//This is for debug
        //intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING.name());//For the Autonomus flight Original (no debug)
        intent_send.putExtra(DEBUG_ACTIVTY_SEND, mensaje);
        startService(intent_send);
    }

    //Is the fonction called to start the sensor-->It will call the Recording Activity from the wacht
    private  void startRecordingOnWear(){
        Log.i(TAG, "Launch smartwatch Sensor reading");
        Intent intentStartRec = new Intent(this, WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());//Call Command of the Wear Service
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_recordingactivity);//Start the Activity described
        this.startService(intentStartRec);
    }

    //Definition of the focntion called in onStop
    public void stopRecordingOnWear() {
        //It will call the comand STOPACTIVTY declared in the WaerActivity to stop the specified Actiivity
        Intent intentStopRec = new Intent(this, WearService.class);
        intentStopRec.setAction(WearService.ACTION_SEND.STOPACTIVITY.name());
        intentStopRec.putExtra(WearService.ACTIVITY_TO_STOP, BuildConfig.W_recordingactivity);
        startService(intentStopRec);
    }



    //Senesor Recived Acceleration Necesarry
    private class AccelerationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Show HR in a TextView
            acceleration = intent.getDoubleExtra(ACCELERATIONVAR, -1);//Get the value of the mAccel
            //----------------
            // Taking the mean of the absolute value of the acceleration over some iterations Niter
            sum_acc += Math.abs(acceleration);

            iter += 1;

            if (iter == Niter) {

                acc_average = sum_acc / Niter;

                iter = 1;
                sum_acc = 0;
            }
            //----------------
            mBebopDrone.set_acc_mean_watch(acc_average);

            mouvement = intent.getBooleanExtra(MOUVEMENT, false);//Get the value of the mouvement
            Log.i(TAG, (String.format("Received Acceleration --> Accel: %s Mouve: %s", acceleration,mouvement)));

            TextView accelTextView = findViewById(R.id.textViewAcceleration);
            if(mouvement) accelTextView.setTextColor(Color.RED);
            else accelTextView.setTextColor(Color.GREEN);
            accelTextView.setText(String.valueOf(acceleration));


        }
    }

    //Buton to try the correct communication between Watch and Tablet
    public void onClickTryComunication(View view) {
        Toast.makeText(getApplicationContext(), "Sending", Toast.LENGTH_SHORT).show();//Debug
        sendMessage("Conexion Etablie");//Send that string to the wacht to be sure that wrork
        //For debugging, it stop the Sensors comunication
        stopRecordingOnWear();
    }
    //When click in the button Start Recording Activity--> The sensor start to get Data (for heart and location Sensor)
    public void onClickStartSensors(View view) {
        startRecordingOnWear();
    }


    //Sensor Received Location (NECESSARRY)
    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the variables of the location from the wach.
            double longitude = intent.getDoubleExtra(LONGITUDE, -1);
            double latitude = intent.getDoubleExtra(LATITUDE, -1);
            double altitude = intent.getDoubleExtra(ALTITUDE, -1);
            Log.i(TAG, (String.format("Recived Location-->Lat: %s Long: %s  Alt; %s", latitude, longitude,altitude)));

            mBebopDrone.set_lat_watch(latitude);
            mBebopDrone.set_long_watch(longitude);
            mBebopDrone.set_alt_watch(altitude);

            //TODO mettre des textView

            //Update the text view for debugging
            TextView longitudeTextView = findViewById(R.id.textViewLongitude);
            longitudeTextView.setText(String.valueOf(longitude));

            TextView latitudeTextView = findViewById(R.id.textViewLatitude);
            latitudeTextView.setText(String.valueOf(latitude));

            TextView altitudeTextView = findViewById(R.id.textViewAltitude);
            altitudeTextView.setText(String.valueOf(altitude));


        }
    }


    // Declarations Antho for autonomous paths
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //private final int power = 15;
    //private final int duration = 4000; // [ms]
    private final int cycles_button = 2;
    // Boolean to eventually exit paths
    boolean keepGoing = false;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "ManualFlightActivity";
    private BebopDrone mBebopDrone;

    //test
    private static final int START_DEVICE_LIST = 1;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private BebopVideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private Button mDownloadBt;

    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;
    private final BebopDrone.Listener mBebopListener = new BebopDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            Log.i(TAG, "entering onDroneConnectionChanged ManualFlightActivity");

            switch (state) {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            Log.i(TAG, "entering onBatteryChargeChanged ManualFlightActivity");

            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            Log.i(TAG, "entering onPilotingStateChanged ManualFlightActivity");

            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setText("Take off");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setText("Land");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(false);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
                    mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "entering onPictureTaken ManualFlightActivity");

            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            Log.i(TAG, "entering configureDecoder ManualFlightActivity");

            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            //Log.i(TAG, "entering onFrameReceived ManualFlightActivity");

            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            Log.i(TAG, "entering onMatchingMediasFound ManualFlightActivity");

            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(ManualFlightActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            Log.i(TAG, "entering onDownloadProgressed ManualFlightActivity");

            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            Log.i(TAG, "entering onDownloadComplete ManualFlightActivity");

            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_flight);
        Log.i(TAG, "entering onCreate ManualFlightActivity");

        initIHM();

        startRecordingOnWear();//initialization of the sensor of the watch YANN

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);


        ////////////////////////////////////////////////////////////////////////////////////////////
//        // Testing the autonomous paths in onCreate of ManualFlightActivity
//        Button TestPathButton = findViewById(R.id.button_test_path);
//        TestPathButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i(TAG, "executePath 1 clicking 'TestPathButton'.");
//                executePath();
//            }
//        });
//
//        Button TestPathButton_2 = findViewById(R.id.button_test_path_2);
//        TestPathButton_2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i(TAG, "executePath 2 clicking 'TestPathButton'.");
//                executePath_2();
//            }
//        });
//
//        Button TestPathButton_3 = findViewById(R.id.button_test_path_3);
//        TestPathButton_3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i(TAG, "executePath 3 clicking 'TestPathButton'.");
//                executePath_3();
//            }
//        });
        ////////////////////////////////////////////////////////////////////////////////////////////

    }

    @Override
    protected void onResume() {
        super.onResume();
        //ADDED for comunication of the watch YANN
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
        //ADDED for comunication of the watch YANN
        super.onPause();
        //Stop to get sensor data-->(Save Energy)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accelerationBroadcastReceiver);
    }

    //Autogenerated (Empty fontion) //ADDED for comunication of the watch YANN
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

    @Override
    protected  void onStop(){
        //ADDED for comunication of the watch YANN
        super.onStop();
        stopRecordingOnWear();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "entering onStart ManualFlightActivity");

        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState()))) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "entering onBackPressed ManualFlightActivity");

        if (mBebopDrone != null) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "entering onDestroy ManualFlightActivity");

        mBebopDrone.dispose();
        super.onDestroy();
    }

    private void initIHM() {
        Log.i(TAG, "entering initIHM ManualFlightActivity");

        mVideoView = (BebopVideoView) findViewById(R.id.videoView);

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        mTakeOffLandBt = (Button) findViewById(R.id.takeOffOrLandBt);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "clicking takeOffOrLandBt ManualFlightActivity");

                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mBebopDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        break;
                    default:
                }
            }
        });

        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
            }
        });

        mDownloadBt = (Button) findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "clicking downloadBt ManualFlightActivity");

                mBebopDrone.getLastFlightMedias();

                mDownloadProgressDialog = new ProgressDialog(ManualFlightActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(true);
                mDownloadProgressDialog.setMessage("Fetching medias");
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        });

        findViewById(R.id.gazUpBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking gazUpBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.gazDownBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking gazDownBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking yawLeftBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking yawRightBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.forwardBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking forwardBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.backBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking backBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.rollLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking rollLeftBt ManualFlightActivity");


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.rollRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "clicking rollRightBt ManualFlightActivity");

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.startAutonomousBt).setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mVideoView.deInit();
                Intent intentStartActivity = new Intent(ManualFlightActivity.this, AutonomousFlightActivity.class);
                startActivityForResult(intentStartActivity, START_DEVICE_LIST);
            }
        });

        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);
    }













    // For XML callback "executePath" --> write "public void executePath(View view)" instead of "public void executePath()"
    // Methods to test simple autonomous path
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
//
//    private static long endTime_wait;
//
//    // Simple path to go right and left a few times
//    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
//    public void executePath() {
//
//
//        // Going middle way on the right
//        long endTime_begin_right = System.currentTimeMillis() + duration / 2;
//        while ((System.currentTimeMillis() < endTime_begin_right) && (keepGoing)) {
//            mBebopDrone.setRoll((byte) power);
//            mBebopDrone.setFlag((byte) 1);
//
//        }
//        // Wait a bit...
//        endTime_wait = System.currentTimeMillis() + 1000;
//        while ((System.currentTimeMillis() < endTime_wait) && (keepGoing)) {
//            mBebopDrone.setRoll((byte) 0);
//            mBebopDrone.setFlag((byte) 0);
//        }
//
//
//        // Going full path left and then full path right, etc.
//        int j = 1;
//        while (j <= cycles) {  // infinite loop --> simply set "while(true)"
//
//            // Going full path LEFT
//            long endTime_left = System.currentTimeMillis() + duration;
//            while ((System.currentTimeMillis() < endTime_left) && (keepGoing)) {
//                mBebopDrone.setRoll((byte) -power);
//                mBebopDrone.setFlag((byte) 1);
//
//
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while ((System.currentTimeMillis() < endTime_wait) && (keepGoing)) {
//                mBebopDrone.setRoll((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            // Going full path RIGHT
//            long endTime_right = System.currentTimeMillis() + duration;
//            while ((System.currentTimeMillis() < endTime_right) && (keepGoing)) {
//                mBebopDrone.setRoll((byte) power);
//                mBebopDrone.setFlag((byte) 1);
//
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while ((System.currentTimeMillis() < endTime_wait) && (keepGoing)) {
//                mBebopDrone.setRoll((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            j = j + 1;
//        }
//
//        // Going middle way back on the left
//        long endTime_end_left = System.currentTimeMillis() + duration / 2;
//        while ((System.currentTimeMillis() < endTime_end_left) && (keepGoing)) {
//            mBebopDrone.setRoll((byte) -power);
//            mBebopDrone.setFlag((byte) 1);
//
//        }
//
//
//        mBebopDrone.setRoll((byte) 0);
//        mBebopDrone.setFlag((byte) 0);
//
//
//
//    }
//
//
//
//
//
//
//
//    // Simple path to go up and down a few times
//    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
//    public void executePath_2() {
//
//        // Going full path up and then full path down, etc.
//        int j = 1;
//        while (j <= cycles) {  // infinite loop --> simply set "while(true)"
//
//            // Going full path UP
//            long endTime_up = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_up) {
//                mBebopDrone.setGaz((byte) power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                mBebopDrone.setGaz((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            // Going full path DOWN
//            long endTime_down = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_down) {
//                // Going left
//                mBebopDrone.setGaz((byte) -power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                // Going right
//                mBebopDrone.setGaz((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            j = j + 1;
//        }
//
//
//        mBebopDrone.setGaz((byte) 0);
//        mBebopDrone.setFlag((byte) 0);
//
//
//    }
//
//
//
//
//
//    // Simple path to go square a few times
//    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
//    public void executePath_3() {
//
//
//        // Going full path right, up, left, down, etc.
//        int j = 1;
//        while (j <= cycles) {  // infinite loop --> simply set "while(true)"
//
//            // Going full path RIGHT
//            long endTime_right = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_right) {
//                mBebopDrone.setRoll((byte) power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                mBebopDrone.setRoll((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            // Going full path UP
//            long endTime_up = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_up) {
//                mBebopDrone.setGaz((byte) power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                mBebopDrone.setGaz((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            // Going full path LEFT
//            long endTime_left = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_left) {
//                mBebopDrone.setRoll((byte) -power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                mBebopDrone.setRoll((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//            // Going full path DOWN
//            long endTime_down = System.currentTimeMillis() + duration;
//            while (System.currentTimeMillis() < endTime_down) {
//                // Going left
//                mBebopDrone.setGaz((byte) -power);
//                mBebopDrone.setFlag((byte) 1);
//            }
//            // Wait a bit...
//            endTime_wait = System.currentTimeMillis() + 1000;
//            while (System.currentTimeMillis() < endTime_wait) {
//                // Going right
//                mBebopDrone.setGaz((byte) 0);
//                mBebopDrone.setFlag((byte) 0);
//            }
//
//
//
//
//            j = j + 1;
//        }
//
//
//        mBebopDrone.setGaz((byte) 0);
//        mBebopDrone.setFlag((byte) 0);
//
//    }
//
//
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



    private boolean enable_path_1_button = false;
    public void onClick_enable_path_1(View view) {
        // Enabling path 1 in any case
        if (enable_path_1_button == false) {
            enable_path_1_button = true;
        }
        mBebopDrone.set_path_1(enable_path_1_button, cycles_button);

    }



    private boolean enable_path_2_button = false;
    public void onClick_enable_path_2(View view) {
        // Enabling path 2 in any case
        if (enable_path_2_button == false) {
            enable_path_2_button = true;
        }
        mBebopDrone.set_path_2(enable_path_2_button, cycles_button);

    }



    private boolean enable_path_3_button = false;
    public void onClick_enable_path_3(View view) {
        // Enabling path 3 in any case
        if (enable_path_3_button == false) {
            enable_path_3_button = true;
        }
        mBebopDrone.set_path_3(enable_path_3_button, cycles_button);
    }






////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    public void exitPath(View view) {
        // Setting the Boolean "keepGoing" to false
        if (keepGoing == true) {
            keepGoing = false;
        }

        mBebopDrone.set_exitPath(keepGoing);
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////







    // onClicks to enable/disable autonomous modes
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean enable_autonom_yaw = false;
    public void onClick_enable_autonom_yaw(View view) {
        // Toggling the button
        if (enable_autonom_yaw == false) {
            enable_autonom_yaw = true;
        } else {
            enable_autonom_yaw = false;
        }
        mBebopDrone.set_autonom_yaw(enable_autonom_yaw);
    }

    private boolean enable_autonom_att_rep = false;
    public void onClick_enable_autonom_att_rep(View view) {
        // Toggling the button
        if (enable_autonom_att_rep == false) {
            enable_autonom_att_rep = true;
        } else {
            enable_autonom_att_rep = false;
        }
        mBebopDrone.set_autonom_att_rep(enable_autonom_att_rep);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////







}

