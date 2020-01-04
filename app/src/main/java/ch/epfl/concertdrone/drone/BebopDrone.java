package ch.epfl.concertdrone.drone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
//import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED_SENSORNAME_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.arutils.ARUTILS_DESTINATION_ENUM;
import com.parrot.arsdk.arutils.ARUTILS_FTP_TYPE_ENUM;
import com.parrot.arsdk.arutils.ARUtilsException;
import com.parrot.arsdk.arutils.ARUtilsManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.epfl.concertdrone.BuildConfig;
import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.WearService;
import ch.epfl.concertdrone.activity.ManualFlightActivity;

import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_START;
import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_STOP;
import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_ENUM.ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_SNAPSHOT;

public class BebopDrone {
    private static final String TAG = "BebopDrone";
    private final List<Listener> mListeners;
    private final Handler mHandler;
    private final Context mContext;
    private final SDCardModule.Listener mSDCardModuleListener = new SDCardModule.Listener() {
        @Override
        public void onMatchingMediasFound(final int nbMedias) {
            Log.d(TAG, "entering onMatchingMediasFound of class BebopDrone");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyMatchingMediasFound(nbMedias);
                }
            });
        }

        @Override
        public void onDownloadProgressed(final String mediaName, final int progress) {
            Log.d(TAG, "entering onDownloadProgressed of class BebopDrone");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadProgressed(mediaName, progress);
                }
            });
        }

        @Override
        public void onDownloadComplete(final String mediaName) {
            Log.i(TAG, "entering onDownloadComplete of class BebopDrone");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadComplete(mediaName);
                }
            });
        }
    };
    private final ARDeviceControllerStreamListener mStreamListener = new ARDeviceControllerStreamListener() {
        @Override
        public ARCONTROLLER_ERROR_ENUM configureDecoder(ARDeviceController deviceController, final ARControllerCodec codec) {
            Log.i(TAG, "entering configureDecoder of class BebopDrone");

            notifyConfigureDecoder(codec);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public ARCONTROLLER_ERROR_ENUM onFrameReceived(ARDeviceController deviceController, final ARFrame frame) {
            //Log.i(TAG, "entering onFrameReceived of class BebopDrone");

            notifyFrameReceived(frame);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public void onFrameTimeout(ARDeviceController deviceController) {
            Log.i(TAG, "entering onFrameTimeout of class BebopDrone");

        }
    };
    private ARDeviceController mDeviceController;
    private SDCardModule mSDCardModule;
    private ARCONTROLLER_DEVICE_STATE_ENUM mState;
    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM mFlyingState;
    private String mCurrentRunId;








////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


    ////// Declarations of the received measurements
    // Declare drone GPS coordinates
    private static double lat_bebop;
    private static double long_bebop;
    private static double alt_bebop;

    // Declare watch GPS coordinates
    private static double lat_watch;
    private static double long_watch;
    private static double alt_watch;
    private static double acc_mean_watch;

    public void set_lat_watch(double lat){
        lat_watch = lat;
    }

    public void set_long_watch(double longitude){
        long_watch = longitude;
    }

    public void set_alt_watch(double alt){
        alt_watch = alt;
    }

    public void set_acc_mean_watch(double acc_mean){
        acc_mean_watch = acc_mean;
    }


    // In order to enable or disable the autonomous modes
    private static boolean enable_autonom_yaw;
    public void set_autonom_yaw(boolean enable_autonom_yaw_button){
        enable_autonom_yaw = enable_autonom_yaw_button;
    }
    private static boolean enable_autonom_att_rep;
    public void set_autonom_att_rep(boolean enable_autonom_att_rep_button){
        enable_autonom_att_rep = enable_autonom_att_rep_button;
    }



    // enable path 1
    private static boolean enable_path_1;
    public void set_path_1(boolean enable_path_1_button, int cycles_button){
        keepGoing = true;
        enable_path_1 = enable_path_1_button;
        cycles = cycles_button;
        endTime_left = System.currentTimeMillis() + duration;
        endTime_wait = endTime_left + pause;
        endTime_right = endTime_wait + duration;
        endTime_wait_2 = endTime_right + pause;
    }

    // enable path 2
    private static boolean enable_path_2;
    public void set_path_2(boolean enable_path_2_button, int cycles_button) {
        keepGoing = true;
        enable_path_2 = enable_path_2_button;
        cycles = cycles_button;
        endTime_up = System.currentTimeMillis() + duration;
        endTime_wait = endTime_up + pause;
        endTime_down = endTime_wait + duration;
        endTime_wait_2 = endTime_down + pause;
    }


    // enable path 3
    private static boolean enable_path_3;
    public void set_path_3(boolean enable_path_3_button, int cycles_button) {
        keepGoing = true;
        enable_path_3 = enable_path_3_button;
        cycles = cycles_button;
        endTime_left_square = System.currentTimeMillis() + duration;
        endTime_wait_square = endTime_left_square + pause;
        endTime_up_square = endTime_wait_square + duration;
        endTime_wait_2_square = endTime_up_square + pause;
        endTime_right_square = endTime_wait_2_square + duration;
        endTime_wait_3_square = endTime_right_square + pause;
        endTime_down_square = endTime_wait_3_square + duration;
        endTime_wait_4_square = endTime_down_square + pause;
    }

    // exit paths
    private static boolean keepGoing;
    public void set_exitPath(boolean keepGoing_button){
        keepGoing = keepGoing_button;
    }



    ////// Declarations for yaw controller
    // Declare yaw value of drone
    private static float yaw_bebop;

    // Other declarations
    private static float yaw_target;

    // PD controller constants
    /////////////////////////////
    // Tests of values:
    // works quite well:
    // KP = 0.2, KD = 0.00001
    // KP = 0.3, KD = 0.00001
    // KP = 0.5, KD = 0.00001
    // KP = 0.5, KD = 0.5
    // KP = 0.4, KD = 0.05 --> error of ~ 2 degrees after stabilization
    // KP = 0.4, KD = 0.001 --> error of ~ 1.7 degree after stabilization
    // KP = 0.4, KD = 0.0001 --> error of ~ 1.5 degrees after stabilization
    // KP = 0.4, KD = 0.00001 --> error of ~ 2 degrees after stabilization
    // KP = 0.3, KD = 0.00001 --> error of ~ 2 degrees after stabilization
    //
    private final double KP = 0.3;
    private final double KD = 0.00001;
    private static float bias = 0;
    /////////////////////////////

    private static double error;
    private static double error_prior;
    private static double derivative;
    private static int input;
    private static byte input_byte;

    private final int iteration_time = 250; // we get yaw values from the bebop every 250[ms]




    ////// Declarations for autonomous attractive/repulsive behaviour
    private static byte pitch_byte;
    private static double dist_drone_watch;
    private final int Radius = 6371000; // Earth radius [m]
    private final double minDist = 2; // min distance drone-watch of 2[m]
    private final double maxDist = 15; // max distance drone-watch of 15[m]


    ////// Declarations for the paths
    private static int cycles;
    private final int duration = 4500; // [ms]
    private final int pause = 750;     // [ms]
    private final int power = 10;
    private final int drift_correction = 1;
    private static long endTime_left;
    private static long endTime_wait;
    private static long endTime_right;
    private static long endTime_wait_2;
    private static long endTime_up;
    private static long endTime_down;

    private static long endTime_left_square;
    private static long endTime_wait_square;
    private static long endTime_up_square;
    private static long endTime_wait_2_square;
    private static long endTime_right_square ;
    private static long endTime_wait_3_square;
    private static long endTime_down_square;
    private static long endTime_wait_4_square;




    // Declaration Antho Picture - Timelapse - Video
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean takeVideoButton_pressed = true;
    private static byte timelapse_enabled;   // 0 = disabled, 1 = enabled
    private static long startingTime;        // starting time of the timelapse
    private static long timelapseLaunchTime;
    private static int timelapse_interval;
    private static boolean pictureNotTakenYet = true;
    ////////////////////////////////////////////////////////////////////////////////////////////////




////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


    //getter of nomber of cycles (Yann)
    public int getCycles(){
        return cycles;
    }




    private final ARDeviceControllerListener mDeviceControllerListener = new ARDeviceControllerListener() {
        @Override
        public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
            Log.i(TAG, "entering onStateChanged of class BebopDrone");


            mState = newState;
            if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState)) {
                mDeviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 1);
            } else if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState)) {
                mSDCardModule.cancelGetFlightMedias();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyConnectionChanged(mState);
                }
            });
        }

        @Override
        public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {
            Log.i(TAG, "entering onExtensionStateChanged of class BebopDrone");

        }

        @Override
        public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
            Log.i(TAG, "entering onCommandReceived of class BebopDrone");

            // if event received is the battery update
            if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final int battery = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyBatteryChanged(battery);
                        }
                    });
                }
            }
            // if event received is the flying state update
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mFlyingState = state;
                            notifyPilotingStateChanged(state);
                        }
                    });
                }
            }
            // if event received is the picture notification
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error = ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR));
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyPictureTaken(error);
                        }
                    });
                }
            }
            // if event received is the run id
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final String runID = (String) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED_RUNID);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentRunId = runID;
                        }
                    });
                }
            }

            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PICTURESETTINGSSTATE_TIMELAPSECHANGED) && (elementDictionary != null)){
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    byte timelapse_enabled = (byte)((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PICTURESETTINGSSTATE_TIMELAPSECHANGED_ENABLED)).intValue();
                    float interval = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PICTURESETTINGSSTATE_TIMELAPSECHANGED_INTERVAL)).doubleValue();
                    float minInterval = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PICTURESETTINGSSTATE_TIMELAPSECHANGED_MININTERVAL)).doubleValue();
                    float maxInterval = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PICTURESETTINGSSTATE_TIMELAPSECHANGED_MAXINTERVAL)).doubleValue();
                    Log.i(TAG,"timelapse enabled? "+timelapse_enabled); // 1 if timelapse is enabled, 0 otherwise
                }
            }


            // Retrieving GPS coordinates
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED) && (elementDictionary != null)){
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    lat_bebop = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_LATITUDE);
                    long_bebop = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_LONGITUDE);
                    alt_bebop = (double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_POSITIONCHANGED_ALTITUDE);

                    Log.i(TAG, "GPS coordinates --> latitude: "+lat_bebop+"; longitude: "+long_bebop+"; altitude: "+alt_bebop);
                }
            }

            // Getting sensor name and state
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED) && (elementDictionary != null)){
                Iterator<ARControllerArgumentDictionary<Object>> itr = elementDictionary.values().iterator();
                while (itr.hasNext()) {
                    ARControllerArgumentDictionary<Object> args = itr.next();
                    if (args != null) {
                        ARCOMMANDS_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED_SENSORNAME_ENUM sensorName = ARCOMMANDS_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED_SENSORNAME_ENUM.getFromValue((Integer)args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED_SENSORNAME));
                        byte sensorState = (byte)((Integer)args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_SENSORSSTATESLISTCHANGED_SENSORSTATE)).intValue();

                        Log.i(TAG, "Sensor name: "+sensorName+" - sensor state: "+sensorState);
                    }
                }
            }

            // Getting number of satellites
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_GPSSTATE_NUMBEROFSATELLITECHANGED) && (elementDictionary != null)){
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    byte numberOfSatellite = (byte)((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_GPSSTATE_NUMBEROFSATELLITECHANGED_NUMBEROFSATELLITE)).intValue();

                    Log.i(TAG, "Number of satellites: "+numberOfSatellite);
                }
            }

            // Getting roll, pitch, yaw current values
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED) && (elementDictionary != null)){
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    //float roll_bebop = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_ROLL)).doubleValue();
                    //float pitch_bebop = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_PITCH)).doubleValue();
                    yaw_bebop = (float)((Double)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED_YAW)).doubleValue();


                    Log.i(TAG, "Yaw yaw_degree_1 Yaw [degree]: "+yaw_bebop); // Originally: 0° = facing North; 90° = facing East; +180° or -180° = facing South; -90° = facing West
                                                                  // For me: 0° = facing East; 90° = facing North

                    if (enable_autonom_yaw) {

                        yaw_bebop = (float) (yaw_bebop*180/Math.PI - 90)*(-1);
                        if (yaw_bebop > 180 && yaw_bebop < 270) {
                            yaw_bebop = (float) -180 + (yaw_bebop-180);
                        }

                        Log.i(TAG, "Yaw yaw_degree Yaw [degree]: "+yaw_bebop); // Originally: 0° = facing North; 90° = facing East; +180° or -180° = facing South; -90° = facing West
                        // For me: 0° = facing East; 90° = facing North

                        // Yaw Controller
                        // The drone will orient in the direction you want (cf. "yaw_target")
                        ////////////////////////////////////////////////////////////////////////////////
                        ////////////////////////////////////////////////////////////////////////////////

                        // Continuous computation of yaw_target
                        Log.i(TAG, "GPS DRONE: "+lat_bebop+" "+long_bebop);
                        Log.i(TAG, "GPS WATCH: "+lat_watch+" "+long_watch);

                        double diff_y = lat_watch - lat_bebop;
                        double diff_x = long_watch - long_bebop;
                        yaw_target = (float) ((float) Math.atan2(diff_y,diff_x)*180.0/Math.PI);
                        Log.i(TAG, "test_yaw_target YAW TARGET: "+yaw_target);
                        Log.i(TAG, "test_yaw_target YAW BEBOP: "+yaw_bebop);

                        error = yaw_target - yaw_bebop;
                        Log.i(TAG, "Yaw yaw_error error yawController: " + error);


                        derivative = (error - error_prior)/iteration_time;

                        // Controller input calculation
                        input = (int) (KP*error + KD*derivative + bias);
                        //input = (int) (KP*error);
                        if (input > 100) {
                            input = 100;
                        }
                        if (input < -100) {
                            input = -100;
                        }
                        Log.i(TAG, "Yaw yaw_input input yawController: " + input);

                        // Adapting input
                        input = input*(-1);

                        // Conversion from int to byte
                        input_byte = (byte) input;

                        //"setYaw((byte) input);" or:
                        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw(input_byte);
                        }

                        error_prior = error;
                        ////////////////////////////////////////////////////////////////////////////////
                        ////////////////////////////////////////////////////////////////////////////////
                    }






                    if (enable_autonom_att_rep) {
                        // Autonomous Attractive / Repulsive behaviour
                        ////////////////////////////////////////////////////////////////////////////////
                        ////////////////////////////////////////////////////////////////////////////////
                        Log.i(TAG,"entering enable_autonom_att_rep");


                        //-------------------------------------
                        // Defining constant K
                        //--------
                        double K = 8; // 7.5
                        //--------

                        // Defining mean_range (the approximate mean of the possible accelerometer values)
                        //--------------------
                        double mean_range = 4.5; // 2.75, 3.25
                        //--------------------

                        // Calculating motor input for "mBebopDrone.setPitch((byte) n)"
                        double pitch_input = (acc_mean_watch - mean_range)*(-K);

                        // Conversion from double to byte
                        pitch_byte = (byte) pitch_input;
                        //-------------------------------------

                        double diff_angle_y = lat_watch - lat_bebop;
                        double diff_angle_x = long_watch - long_bebop;

                        dist_drone_watch = Math.sqrt(Math.pow(diff_angle_y*(Math.PI/180)*Radius,2.0)+Math.pow(diff_angle_x*(Math.PI/180)*Radius,2.0));
                        Log.i(TAG, "distance drone - watch: "+dist_drone_watch);


                        // setting distance limits between the drone and the watch in [m]
                        // if (dist_drone_watch > minDist) && (dist_drone_watch < maxDist)) {...}
                        setPitch((byte) pitch_byte);
                        setFlag((byte) 1);

                        Log.i(TAG, "    acc_mean_watch: "+ acc_mean_watch+"    pitch_byte: "+ pitch_byte);


//                        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
//                             // && (dist_drone_watch > 2) && (dist_drone_watch < 15)
//                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch(pitch_byte);
//                        }

                        ////////////////////////////////////////////////////////////////////////////////
                        ////////////////////////////////////////////////////////////////////////////////
                    }







                    // Paths
                    ////////////////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////
                    // Enabling path 1
                    if (enable_path_1) {
                        Log.i(TAG, "entered enable_path_1: "+keepGoing);

                        // Going full path LEFT
                        Log.i(TAG, "CurrentTime: "+System.currentTimeMillis()+ "    EndTimeLeft: "+endTime_left);
                        if ((System.currentTimeMillis() < endTime_left) && (keepGoing)) {
                            Log.i(TAG, "entered going left");

                            setRoll((byte) -power);
                            // Correction so that the drone doesn't go too forward
                            setPitch((byte) -drift_correction);
                            setFlag((byte) 1);
                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_left) && (System.currentTimeMillis() < endTime_wait) && (keepGoing)) {
                            Log.i(TAG, "entered wait1");

                            setRoll((byte) 0);
                            setFlag((byte) 0);
                        }


                        // Going full path RIGHT
                        if ((System.currentTimeMillis() > endTime_wait) && (System.currentTimeMillis() < endTime_right) && (keepGoing)) {
                            Log.i(TAG, "entered going right");
                            setRoll((byte) power);
                            // Correction so that the drone doesn't go too forward
                            setPitch((byte) -drift_correction);
                            setFlag((byte) 1);

                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_right) && (System.currentTimeMillis() < endTime_wait_2) && (keepGoing)) {
                            Log.i(TAG, "entered wait2");

                            setRoll((byte) 0);
                            setFlag((byte) 0);
                        }


                        if (System.currentTimeMillis() > endTime_wait_2) {
                            Log.i(TAG, "cycle over");

                            if (cycles > 1) {
                                set_path_1(true, cycles-1);
                            } else {
                                enable_path_1 = false;
                                keepGoing = false;
                            }
                        }

                    }



                    // Enabling path 2
                    if (enable_path_2) {
                        Log.i(TAG, "entered enable_path_2: "+keepGoing);

                        // Going full path UP
                        Log.i(TAG, "CurrentTime: "+System.currentTimeMillis()+ "    EndTimeUp: "+endTime_up);
                        if ((System.currentTimeMillis() < endTime_up) && (keepGoing)) {
                            Log.i(TAG, "entered going up");

                            setGaz((byte) power);
                            setFlag((byte) 1);
                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_up) && (System.currentTimeMillis() < endTime_wait) && (keepGoing)) {
                            Log.i(TAG, "entered wait1");

                            setGaz((byte) 0);
                            setFlag((byte) 0);
                        }


                        // Going full path DOWN
                        if ((System.currentTimeMillis() > endTime_wait) && (System.currentTimeMillis() < endTime_down) && (keepGoing)) {
                            Log.i(TAG, "entered going down");
                            setGaz((byte) -power);
                            setFlag((byte) 1);

                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_down) && (System.currentTimeMillis() < endTime_wait_2) && (keepGoing)) {
                            Log.i(TAG, "entered wait2");

                            setGaz((byte) 0);
                            setFlag((byte) 0);
                        }


                        if (System.currentTimeMillis() > endTime_wait_2) {
                            Log.i(TAG, "cycle over");

                            if (cycles > 1) {
                                set_path_2(true, cycles-1);
                            } else {
                                enable_path_2 = false;
                                keepGoing = false;
                            }
                        }

                    }



                    // Enabling path 3
                    if (enable_path_3) {
                        Log.i(TAG, "entered enable_path_3: "+keepGoing);

                        // Going full path LEFT
                        Log.i(TAG, "CurrentTime: "+System.currentTimeMillis()+ "    EndTimeLeft: "+endTime_left);
                        if ((System.currentTimeMillis() < endTime_left_square) && (keepGoing)) {
                            Log.i(TAG, "entered going left");

                            setRoll((byte) -power);
                            // Correction so that the drone doesn't go too forward
                            setPitch((byte) -drift_correction);
                            setFlag((byte) 1);
                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_left_square) && (System.currentTimeMillis() < endTime_wait_square) && (keepGoing)) {
                            Log.i(TAG, "entered wait1");

                            setRoll((byte) 0);
                            setFlag((byte) 0);
                        }


                        // Going full path UP
                        Log.i(TAG, "CurrentTime: "+System.currentTimeMillis()+ "    EndTimeUp: "+endTime_up);
                        if ((System.currentTimeMillis() > endTime_wait_square) && (System.currentTimeMillis() < endTime_up_square) && (keepGoing)) {
                            Log.i(TAG, "entered going up");

                            setGaz((byte) power);
                            setFlag((byte) 1);
                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_up_square) && (System.currentTimeMillis() < endTime_wait_2_square) && (keepGoing)) {
                            Log.i(TAG, "entered wait1");

                            setGaz((byte) 0);
                            setFlag((byte) 0);
                        }


                        // Going full path RIGHT
                        if ((System.currentTimeMillis() > endTime_wait_2_square) && (System.currentTimeMillis() < endTime_right_square) && (keepGoing)) {
                            Log.i(TAG, "entered going right");
                            setRoll((byte) power);
                            // Correction so that the drone doesn't go too forward
                            setPitch((byte) -drift_correction);
                            setFlag((byte) 1);

                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_right_square) && (System.currentTimeMillis() < endTime_wait_3_square) && (keepGoing)) {
                            Log.i(TAG, "entered wait2");

                            setRoll((byte) 0);
                            setFlag((byte) 0);
                        }


                        // Going full path DOWN
                        if ((System.currentTimeMillis() > endTime_wait_3_square) && (System.currentTimeMillis() < endTime_down_square) && (keepGoing)) {
                            Log.i(TAG, "entered going down");
                            setGaz((byte) -power);
                            setFlag((byte) 1);

                        }

                        // Wait a bit...
                        if ((System.currentTimeMillis() > endTime_down_square) && (System.currentTimeMillis() < endTime_wait_4_square) && (keepGoing)) {
                            Log.i(TAG, "entered wait2");

                            setGaz((byte) 0);
                            setFlag((byte) 0);
                        }


                        if (System.currentTimeMillis() > endTime_wait_4_square) {
                            Log.i(TAG, "cycle over");

                            if (cycles > 1) {
                                set_path_3(true, cycles-1);
                            } else {
                                enable_path_3 = false;
                                keepGoing = false;
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {//Yann a effacer
                                    notifyCyclesChanged(cycles);
                                }
                            });
                        }

                    }



                    // Making the drone exit the path and stop in case keepGoing is false
                    if (!keepGoing) { // = if keepGoing is false
                        enable_path_1 = false;
                        enable_path_2 = false;
                        enable_path_3 = false;
                        setGaz((byte) 0);
                        setPitch((byte) 0);
                        setRoll((byte) 0);
                        setFlag((byte) 0);
                        keepGoing = true;

                    }
                    ////////////////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////




                    ////////////////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////
                    // Alternative method to take timelapses
                    // If the timelapse mode is activated, then take picture every "timelapse_interval" [s]
                    if (System.currentTimeMillis()>timelapseLaunchTime) {
                        pictureNotTakenYet = true;
                    }
                    if ( (timelapse_enabled == 1) && ((System.currentTimeMillis()>timelapseLaunchTime)) && (pictureNotTakenYet)) {
                        Log.i(TAG,"entering fashion way to make timelapse,   timelapse_interval: "+timelapse_interval+" [s]");
                        // take picture
                        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
                            mDeviceController.getFeatureARDrone3().sendMediaRecordPictureV2();
                        }
                        // Computing the next timelapse launch time
                        timelapseLaunchTime += timelapse_interval*1000;
                        pictureNotTakenYet = false;
                    }
                    ////////////////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////

                }
            }






        }
    };
    private ARDiscoveryDeviceService mDeviceService;
    private ARUtilsManager mFtpListManager;
    private ARUtilsManager mFtpQueueManager;

    public BebopDrone(Context context, @NonNull ARDiscoveryDeviceService deviceService) {
        Log.i(TAG, "entering BebopDrone of class BebopDrone");


        mContext = context;
        mListeners = new ArrayList<>();
        mDeviceService = deviceService;

        // needed because some callbacks will be called on the main thread
        mHandler = new Handler(context.getMainLooper());

        mState = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED;

        // if the product type of the deviceService match with the types supported
        ARDISCOVERY_PRODUCT_ENUM productType = ARDiscoveryService.getProductFromProductID(mDeviceService.getProductID());
        ARDISCOVERY_PRODUCT_FAMILY_ENUM family = ARDiscoveryService.getProductFamily(productType);
        if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE.equals(family)) {

            ARDiscoveryDevice discoveryDevice = createDiscoveryDevice(mDeviceService);
            if (discoveryDevice != null) {
                mDeviceController = createDeviceController(discoveryDevice);
                discoveryDevice.dispose();
            }

            try {
                mFtpListManager = new ARUtilsManager();
                mFtpQueueManager = new ARUtilsManager();

                mFtpListManager.initFtp(mContext, deviceService, ARUTILS_DESTINATION_ENUM.ARUTILS_DESTINATION_DRONE, ARUTILS_FTP_TYPE_ENUM.ARUTILS_FTP_TYPE_GENERIC);
                mFtpQueueManager.initFtp(mContext, deviceService, ARUTILS_DESTINATION_ENUM.ARUTILS_DESTINATION_DRONE, ARUTILS_FTP_TYPE_ENUM.ARUTILS_FTP_TYPE_GENERIC);

                mSDCardModule = new SDCardModule(mFtpListManager, mFtpQueueManager);
                mSDCardModule.addListener(mSDCardModuleListener);
            } catch (ARUtilsException e) {
                Log.e(TAG, "Exception", e);
            }

        } else {
            Log.e(TAG, "DeviceService type is not supported by BebopDrone");
        }
    }

    public void dispose() {
        Log.i(TAG, "entering dispose of class BebopDrone");

        if (mDeviceController != null)
            mDeviceController.dispose();
        if (mFtpListManager != null)
            mFtpListManager.closeFtp(mContext, mDeviceService);
        if (mFtpQueueManager != null)
            mFtpQueueManager.closeFtp(mContext, mDeviceService);
    }
    //endregion Listener

    //region Listener functions
    public void addListener(Listener listener) {
        Log.i(TAG, "entering addListener of class BebopDrone");

        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Connect to the drone
     *
     * @return true if operation was successful.
     * Returning true doesn't mean that device is connected.
     * You can be informed of the actual connection through {@link Listener#onDroneConnectionChanged}
     */
    public boolean connect() {
        Log.i(TAG, "entering connect of class BebopDrone");

        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.start();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Disconnect from the drone
     *
     * @return true if operation was successful.
     * Returning true doesn't mean that device is disconnected.
     * You can be informed of the actual disconnection through {@link Listener#onDroneConnectionChanged}
     */
    public boolean disconnect() {
        Log.i(TAG, "entering disconnect of class BebopDrone");

        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.stop();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Get the current connection state
     *
     * @return the connection state of the drone
     */
    public ARCONTROLLER_DEVICE_STATE_ENUM getConnectionState() {
        Log.i(TAG, "entering getConnectionState of class BebopDrone");

        return mState;
    }

    /**
     * Get the current flying state
     *
     * @return the flying state
     */
    public ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getFlyingState() {
        Log.i(TAG, "entering getFlyingState of class BebopDrone");

        return mFlyingState;
    }

    public void takeOff() {
        Log.i(TAG, "entering takeOff of class BebopDrone");


        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
        }
    }

    public void land() {
        Log.i(TAG, "entering land of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingLanding();
        }
    }

    public void emergency() {
        Log.i(TAG, "entering emergency of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
        }
    }

    public void takePicture() {
        Log.i(TAG, "entering takePicture of class BebopDrone");

        // Setting the format of the pictures to "snapshot"
        // type (enum): The type of photo format
        // - raw: Take raw image
        // - jpeg: Take a 4:3 jpeg photo
        // - snapshot: Take a 16:9 snapshot from camera
        // - jpeg_fisheye: Take jpeg fisheye image only
        //mDeviceController.getFeatureARDrone3().sendPictureSettingsPictureFormatSelection((ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_ENUM)type);
        mDeviceController.getFeatureARDrone3().sendPictureSettingsPictureFormatSelection((ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_SNAPSHOT));


        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendMediaRecordPictureV2();
            Log.i(TAG,"entering takePicture - mDeviceController.getFeatureARDrone3().sendMediaRecordPictureV2()");
        }

    }


    public void takeVideo(int timelapse_interv) {
        Log.i(TAG, "entering takeVideo of class BebopDrone");

        timelapse_interval = timelapse_interv;

        // Configure timelapse mode
        // - enabled (u8): 1 if timelapse is enabled, 0 otherwise
        // - interval (float): interval in seconds for taking pictures
        //
        if (timelapse_interval == 0){ // activating video mode
            timelapse_enabled = 0;
        } else {                      // activating timelapse mode
            timelapse_enabled = 1;
            timelapseLaunchTime = System.currentTimeMillis() + timelapse_interval*1000; // timelapse launchtime in [ms]

        }
        Log.i(TAG,"      timelapse_interval: "+timelapse_interval+"    timelapse_enabled: "+timelapse_enabled);
        mDeviceController.getFeatureARDrone3().sendPictureSettingsTimelapseSelection(timelapse_enabled, timelapse_interval);



        if (takeVideoButton_pressed == true) {
            // Start recording

            // Video (or timelapse if enabled) record
            // record (enum): Command to record video (or timelapse)
            // - stop: Stop the video recording
            // - start: Start the video recording
            //mDeviceController.getFeatureARDrone3().sendMediaRecordVideoV2((ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_ENUM)record);
            mDeviceController.getFeatureARDrone3().sendMediaRecordVideoV2(ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_START);
            Log.i(TAG,"start recording video");

            takeVideoButton_pressed = false;

        } else { // takeVideoButton_pressed == false
            // Stop recording

            mDeviceController.getFeatureARDrone3().sendMediaRecordVideoV2(ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEOV2_RECORD_STOP);
            Log.i(TAG,"stop recording video");

            timelapse_enabled = 0;

            takeVideoButton_pressed = true;

        }

    }


    /**
     * Set the forward/backward angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the pitch value
     *
     * @param pitch value in percentage from -100 to 100
     */
    public void setPitch(byte pitch) {
        Log.i(TAG, "entering setPitch of class BebopDrone");


        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch(pitch);
        }
    }

    /**
     * Set the side angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the roll value
     *
     * @param roll value in percentage from -100 to 100
     */
    public void setRoll(byte roll) {
        Log.i(TAG, "entering setRoll of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll(roll);
        }
    }

    public void setYaw(byte yaw) {
        Log.i(TAG, "entering setYaw of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw(yaw);
        }
    }

    public void setGaz(byte gaz) {
        Log.i(TAG, "entering setGaz of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz(gaz);
        }
    }


    /**
     * Take in account or not the pitch and roll values
     *
     * @param flag 1 if the pitch and roll values should be used, 0 otherwise
     */
    public void setFlag(byte flag) {
        Log.i(TAG, "entering setFlag of class BebopDrone");

        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag(flag);
        }
    }

    /**
     * Download the last flight medias
     * Uses the run id to download all medias related to the last flight
     * If no run id is available, download all medias of the day
     */
    public void getLastFlightMedias() {
        Log.i(TAG, "entering getLastFlightMedias of class BebopDrone");

        String runId = mCurrentRunId;
        if ((runId != null) && !runId.isEmpty()) {
            mSDCardModule.getFlightMedias(runId);
        } else {
            Log.e(TAG, "RunID not available, fallback to the day's medias");
            mSDCardModule.getTodaysFlightMedias();
        }
    }

    public void cancelGetLastFlightMedias() {
        Log.i(TAG, "entering cancelGetLastFlightMedias of class BebopDrone");

        mSDCardModule.cancelGetFlightMedias();
    }

    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service) {
        Log.i(TAG, "entering createDiscoveryDevice of class BebopDrone");


        ARDiscoveryDevice device = null;
        try {
            device = new ARDiscoveryDevice(mContext, service);
        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
            Log.e(TAG, "Error: " + e.getError());
        }

        return device;
    }

    private ARDeviceController createDeviceController(@NonNull ARDiscoveryDevice discoveryDevice) {
        Log.i(TAG, "entering createDeviceController of class BebopDrone");

        ARDeviceController deviceController = null;
        try {
            deviceController = new ARDeviceController(discoveryDevice);

            deviceController.addListener(mDeviceControllerListener);
            deviceController.addStreamListener(mStreamListener);
        } catch (ARControllerException e) {
            Log.e(TAG, "Exception", e);
        }

        return deviceController;
    }

    //region notify listener block
    private void notifyConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        Log.i(TAG, "entering notifyConnectionChanged of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDroneConnectionChanged(state);
        }
    }

    private void notifyCyclesChanged(int cycles) {
        Log.i(TAG, "entering notifyBatteryChanged of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onCyclesChanged(cycles);
        }
    }
    private void notifyBatteryChanged(int battery) {
        Log.i(TAG, "entering notifyBatteryChanged of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onBatteryChargeChanged(battery);
        }
    }

    private void notifyPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        Log.i(TAG, "entering notifyPilotingStateChanged of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onPilotingStateChanged(state);
        }
    }

    private void notifyPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
        Log.i(TAG, "entering notifyPictureTaken of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onPictureTaken(error);
        }
    }

    private void notifyConfigureDecoder(ARControllerCodec codec) {
        Log.i(TAG, "entering notifyConfigureDecoder of class BebopDrone");


        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.configureDecoder(codec);
        }
    }

    private void notifyFrameReceived(ARFrame frame) {
        //Log.i(TAG, "entering notifyFrameReceived of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onFrameReceived(frame);
        }
    }

    private void notifyMatchingMediasFound(int nbMedias) {
        Log.i(TAG, "entering notifyMatchingMediasFound of class BebopDrone");


        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onMatchingMediasFound(nbMedias);
        }
    }
    //endregion notify listener block

    private void notifyDownloadProgressed(String mediaName, int progress) {
        Log.i(TAG, "entering notifyDownloadProgressed of class BebopDrone");

        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDownloadProgressed(mediaName, progress);
        }
    }

    private void notifyDownloadComplete(String mediaName) {
        Log.i(TAG, "entering notifyDownloadComplete of class BebopDrone");


        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDownloadComplete(mediaName);
        }
    }

    public interface Listener {
        /**
         * Called when the connection to the drone changes
         * Called in the main thread
         *
         * @param state the state of the drone
         */
        void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state);

        /**
         * Called when the battery charge changes
         * Called in the main thread
         *
         * @param batteryPercentage the battery remaining (in percent)
         */
        void onBatteryChargeChanged(int batteryPercentage);

        /**
         * Called when the cicles change
         * Called in the main thread
         *
         * @param cyclesNumber are the cycle of the path selected remaining
         */
        void onCyclesChanged(int cyclesNumber);

        /**
         * Called when the piloting state changes
         * Called in the main thread
         *
         * @param state the piloting state of the drone
         */
        void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state);

        /**
         * Called when a picture is taken
         * Called on a separate thread
         *
         * @param error ERROR_OK if picture has been taken, otherwise describe the error
         */
        void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error);

        /**
         * Called when the video decoder should be configured
         * Called on a separate thread
         *
         * @param codec the codec to configure the decoder with
         */
        void configureDecoder(ARControllerCodec codec);

        /**
         * Called when a video frame has been received
         * Called on a separate thread
         *
         * @param frame the video frame
         */
        void onFrameReceived(ARFrame frame);

        /**
         * Called before medias will be downloaded
         * Called in the main thread
         *
         * @param nbMedias the number of medias that will be downloaded
         */
        void onMatchingMediasFound(int nbMedias);

        /**
         * Called each time the progress of a download changes
         * Called in the main thread
         *
         * @param mediaName the name of the media
         * @param progress  the progress of its download (from 0 to 100)
         */
        void onDownloadProgressed(String mediaName, int progress);

        /**
         * Called when a media download has ended
         * Called in the main thread
         *
         * @param mediaName the name of the media
         */
        void onDownloadComplete(String mediaName);
    }


    //Pour la comunication entre la montre et la tablette YANN----------------------------------------------------------------------------------------
    /*
    J'ai rajoute tout ça mais je sais pas si ça focntione encore.
    Pour les intetn plutot de metre this jai mis mContext (c'est ce que j'ai vu sur google comme solution)
     */
    public static final String RECEIVED_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String ALTITUDE = "ALTITUDE";

    public static final String RECEIVED_ACCELERATION = "RECEIVED_ACCELERATION";
    public static final String ACCELERATIONVAR = "ACCELERATIONVAR";
    public static final String MOUVEMENT = "MOUVEMENT";

    private LocationBroadcastReceiver locationBroadcastReceiver;
    private AccelerationBroadcastReceiver accelerationBroadcastReceiver;//NECESSARRY

    //Variable to store the mouvement acceleration
    private double acceleration =0;
    private boolean mouvement =false;
    //For the comunication of the wacht
    public static final String ACTIVTY_SEND = "ACTIVTY_SEND";

    //Senesor Recived Acceleration Necesarry
    private class AccelerationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Show HR in a TextView
            acceleration = intent.getDoubleExtra(ACCELERATIONVAR, -1);//Get the value of the mAccel
            mouvement = intent.getBooleanExtra(MOUVEMENT, false);//Get the value of the mouvement
            Log.i(TAG, (String.format("Recived Acceleration-->Accel: %s Mouve: %s", acceleration,mouvement)));
        }
    }

    //Sensor Recived Location (NECESSARRY)
    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "entering onReceiveIntent of class BebopDrone");
            // Get the variables of the location from the wach.
            double longitude_watch = intent.getDoubleExtra(LONGITUDE, -1);
            double latitude_watch = intent.getDoubleExtra(LATITUDE, -1);
            double altitude_watch = intent.getDoubleExtra(ALTITUDE, -1);
            Log.i(TAG, (String.format("Recived Location BebopClass-->Lat: %s \nLong: %s\nAlt; %s", latitude_watch, longitude_watch,altitude_watch)));
        }
    }

    //TODO implemtents next fonction in AutonumusFlight Activity-->Take reference of Debug AutonumusFlightActivity
    //Fontion to send a string to the wacht via Wear Service and intent
    public void sendMessage(String mensaje) {
        Intent intent_send = new Intent(mContext, WearService.class);
        intent_send.setAction(WearService.ACTION_SEND.EXAMPLE_SEND_STRING.name());//For the Autonomus flight Original (no debug)
        intent_send.putExtra(ACTIVTY_SEND, mensaje);
        mContext.startService(intent_send);
    }

    //Is the fonction called to start the sensor-->It will call the Recording Activity from the wacht
    //TODO call this in the constructor to start recording
    public void startRecordingOnWear(){
        Log.i(TAG, "Launch smartwatch Sensor reading");
        Intent intentStartRec = new Intent(mContext, WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());//Call Command of the Wear Service
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_recordingactivity);//Start the Activity described
        mContext.startService(intentStartRec);
    }
    //TODO Implemnter onResume, onPause, onStop
    public void onRegisterReciver(){//For the onResume
        //Get the location back from the watch
        locationBroadcastReceiver = new LocationBroadcastReceiver();
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(locationBroadcastReceiver, new
                IntentFilter(RECEIVED_LOCATION));

        accelerationBroadcastReceiver = new AccelerationBroadcastReceiver();
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(accelerationBroadcastReceiver, new
                IntentFilter(RECEIVED_ACCELERATION));
    }

    public void onUnRegisterReceiver(){//For the onPause
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(locationBroadcastReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(accelerationBroadcastReceiver);
    }

    //Definition of the focntion called in onStop
    public void stopRecordingOnWear() {//For the onStop
        //It will call the comand STOPACTIVTY declared in the WaerActivity to stop the specified Actiivity
        Intent intentStopRec = new Intent(mContext, WearService.class);
        intentStopRec.setAction(WearService.ACTION_SEND.STOPACTIVITY.name());
        intentStopRec.putExtra(WearService.ACTIVITY_TO_STOP, BuildConfig.W_recordingactivity);
        mContext.startService(intentStopRec);
    }

    //Autogenerated (Empty fontion) I dont know if it necessarry
    public void onLocationChanged(Location location) {

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

}
