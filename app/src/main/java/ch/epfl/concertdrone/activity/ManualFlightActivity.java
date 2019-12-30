package ch.epfl.concertdrone.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.drone.BebopDrone;
import ch.epfl.concertdrone.preprogrammed.BebopDroneRoutine;
import ch.epfl.concertdrone.view.BebopVideoView;

public class ManualFlightActivity extends AppCompatActivity {

    // Declarations Antho for autonomous paths
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final int power = 15;
    private final int duration = 4000; // [ms]
    private final int cycles = 2;
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

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);


        ////////////////////////////////////////////////////////////////////////////////////////////
        // Testing the autonomous paths in onCreate of ManualFlightActivity
        Button TestPathButton = findViewById(R.id.button_test_path);
        TestPathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "executePath 1 clicking 'TestPathButton'.");
                executePath();
            }
        });

        Button TestPathButton_2 = findViewById(R.id.button_test_path_2);
        TestPathButton_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "executePath 2 clicking 'TestPathButton'.");
                executePath_2();
            }
        });

        Button TestPathButton_3 = findViewById(R.id.button_test_path_3);
        TestPathButton_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "executePath 3 clicking 'TestPathButton'.");
                executePath_3();
            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////////

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


    private static long endTime_wait;

    // Simple path to go right and left a few times
    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
    public void executePath() {

        // Going middle way on the right
        long endTime_begin_right = System.currentTimeMillis() + duration / 2;
        while (System.currentTimeMillis() < endTime_begin_right) {
            mBebopDrone.setRoll((byte) power);
            mBebopDrone.setFlag((byte) 1);

        }
        // Wait a bit...
        endTime_wait = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < endTime_wait) {
            mBebopDrone.setRoll((byte) 0);
            mBebopDrone.setFlag((byte) 0);
        }


        // Going full path left and then full path right, etc.
        int j = 1;
        while (j <= cycles) {  // infinite loop --> simply set "while(true)"

            // Going full path LEFT
            long endTime_left = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_left) {
                mBebopDrone.setRoll((byte) -power);
                mBebopDrone.setFlag((byte) 1);


            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setRoll((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            // Going full path RIGHT
            long endTime_right = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_right) {
                mBebopDrone.setRoll((byte) power);
                mBebopDrone.setFlag((byte) 1);

            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setRoll((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            j = j + 1;
        }

        // Going middle way back on the left
        long endTime_end_left = System.currentTimeMillis() + duration / 2;
        while (System.currentTimeMillis() < endTime_end_left) {
            mBebopDrone.setRoll((byte) -power);
            mBebopDrone.setFlag((byte) 1);

        }


        mBebopDrone.setRoll((byte) 0);
        mBebopDrone.setFlag((byte) 0);



    }







    // Simple path to go up and down a few times
    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
    public void executePath_2() {


        // Going full path up and then full path down, etc.
        int j = 1;
        while (j <= cycles) {  // infinite loop --> simply set "while(true)"

            // Going full path UP
            long endTime_up = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_up) {
                mBebopDrone.setGaz((byte) power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setGaz((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            // Going full path DOWN
            long endTime_down = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_down) {
                // Going left
                mBebopDrone.setGaz((byte) -power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                // Going right
                mBebopDrone.setGaz((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            j = j + 1;
        }


        mBebopDrone.setGaz((byte) 0);
        mBebopDrone.setFlag((byte) 0);


    }





    // Simple path to go square a few times
    // If the drone is NOT HOVERING, we shouldn't have pressed on the button
    public void executePath_3() {


        // Going full path right, up, left, down, etc.
        int j = 1;
        while (j <= cycles) {  // infinite loop --> simply set "while(true)"

            // Going full path RIGHT
            long endTime_right = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_right) {
                mBebopDrone.setRoll((byte) power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setRoll((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            // Going full path UP
            long endTime_up = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_up) {
                mBebopDrone.setGaz((byte) power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setGaz((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            // Going full path LEFT
            long endTime_left = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_left) {
                mBebopDrone.setRoll((byte) -power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                mBebopDrone.setRoll((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }


            // Going full path DOWN
            long endTime_down = System.currentTimeMillis() + duration;
            while (System.currentTimeMillis() < endTime_down) {
                // Going left
                mBebopDrone.setGaz((byte) -power);
                mBebopDrone.setFlag((byte) 1);
            }
            // Wait a bit...
            endTime_wait = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < endTime_wait) {
                // Going right
                mBebopDrone.setGaz((byte) 0);
                mBebopDrone.setFlag((byte) 0);
            }




            j = j + 1;
        }


        mBebopDrone.setGaz((byte) 0);
        mBebopDrone.setFlag((byte) 0);

    }




////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////




}

