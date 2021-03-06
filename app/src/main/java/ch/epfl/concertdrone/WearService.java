package ch.epfl.concertdrone;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Debug;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.epfl.concertdrone.activity.AutonomousFlightActivity;
import ch.epfl.concertdrone.activity.DebugAutonomousFlightActivity;
import ch.epfl.concertdrone.activity.ManualFlightActivity;
import ch.epfl.concertdrone.drone.BebopDrone;

public class WearService extends WearableListenerService {

    // Tag for Logcat
    private static final String TAG = "WearService";

    //ID's of the diferents isntruction and variables
    public static final String ACTIVITY_TO_START = "ACTIVITY_TO_START";
    public static final String ACTIVITY_TO_STOP = "ACTIVITY_TO_STOP";

    public static final String MESSAGE = "MESSAGE";
    public static final String DATAMAP_INT = "DATAMAP_INT";
    public static final String DATAMAP_INT_ARRAYLIST = "DATAMAP_INT_ARRAYLIST";
    public static final String IMAGE = "IMAGE";
    public static final String PATH = "PATH";

    // Actions defined for the onStartCommand(...)
    public enum ACTION_SEND {
        EXAMPLE_SEND_STRING, EXAMPLE_SEND_STRING_DUBUG,STARTACTIVITY,STOPACTIVITY,
        EXAMPLE_DATAMAP, EXAMPLE_ASSET,MESSAGE
    }

    //Start Comand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // If no action defined, return
        if (intent.getAction() == null) return START_NOT_STICKY;

        // Match against the given action
        ACTION_SEND action = ACTION_SEND.valueOf(intent.getAction());
        PutDataMapRequest putDataMapRequest;
        switch (action) {
            case STARTACTIVITY://Start the Activity defined of the wach
                Log.i(TAG, "WearSeviceClass Start Activity");
                String activity = intent.getStringExtra(ACTIVITY_TO_START);
                sendMessage(activity, BuildConfig.W_path_start_activity);
                break;
            case STOPACTIVITY://Stop the activity defined of the watch
                Log.i(TAG, "WearSeviceClass Stop Activity");
                String activityStop = intent.getStringExtra(ACTIVITY_TO_STOP);
                sendMessage(activityStop, BuildConfig.W_path_stop_activity);
                break;
            case MESSAGE://Send an Message to the wathc (another form used to do it?)
                String message = intent.getStringExtra(MESSAGE);
                if (message == null) message = "No Message";
                sendMessage(message, intent.getStringExtra(PATH));
                break;
            case EXAMPLE_DATAMAP://Not Used
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_datamap);
                putDataMapRequest.getDataMap().putInt(BuildConfig.W_a_key, intent.getIntExtra(DATAMAP_INT, -1));
                putDataMapRequest.getDataMap().putIntegerArrayList(BuildConfig.W_some_other_key, intent.getIntegerArrayListExtra(DATAMAP_INT_ARRAYLIST));
                sendPutDataMapRequest(putDataMapRequest);
                break;
            case EXAMPLE_ASSET://Not used
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_asset);
                putDataMapRequest.getDataMap().putAsset(BuildConfig.W_some_other_key, (Asset) intent.getParcelableExtra(IMAGE));
                sendPutDataMapRequest(putDataMapRequest);
                break;

                //Rajouté
            case EXAMPLE_SEND_STRING: //Send String to the Autonomus Flight (ORiginal)
                // Example action of sending a String received from the MainActivity
                String message_to_send = intent.getStringExtra(AutonomousFlightActivity
                        .EXAMPLE_INTENT_STRING_NAME_ACTIVITY_TO_SERVICE);
                sendMessage(message_to_send, BuildConfig.W_path_example_message);
                Log.i(TAG, "Send String");

                break;

            //Rajouté Pour le DEUBG, il envoi a la classe DebugAutonomus FLight (C'est pour le text debug) //TODO a enlever
            case EXAMPLE_SEND_STRING_DUBUG:
                // Example action of sending a String received from the MainActivity
                String message_to_send_debug = intent.getStringExtra(DebugAutonomousFlightActivity
                        .DEBUG_ACTIVTY_SEND);
                sendMessage(message_to_send_debug, BuildConfig.W_path_example_message);
                Log.i(TAG, "Send String Debug");

                break;

            default:
                Log.w(TAG, "Unknown action");
                break;
        }

        return START_NOT_STICKY;
    }




    //When the value has change it send  the new value recived from the wear service from the watch to the Activty
    //in a intent defined depending of the key used
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {

            // Get the URI of the event
            Uri uri = event.getDataItem().getUri();

            // Test if data has changed or has been removed
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                // Extract the dataMap from the event
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                Log.v(TAG, "DataItem Changed: " + event.getDataItem().toString() + "\n"
                        + "\tPath: " + uri
                        + "\tDatamap: " + dataMapItem.getDataMap() + "\n");

                Intent intent;
                Intent intentDebug;//Another Intent for debuging

                assert uri.getPath() != null;
                switch (uri.getPath()) {
                    case BuildConfig.W_example_path_asset:
                        // Extract the data behind the key you know contains data
                        Asset asset = dataMapItem.getDataMap().getAsset(BuildConfig.W_some_other_key);
                        intent = new Intent("REPLACE_THIS_WITH_A_STRING_OF_ACTION_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY");
                        bitmapFromAsset(asset, intent, "REPLACE_THIS_WITH_A_STRING_OF_IMAGE_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY");
                        break;
                    case BuildConfig.W_example_path_datamap:
                        // Extract the data behind the key you know contains data
                        int integer = dataMapItem.getDataMap().getInt(BuildConfig.W_a_key);
                        ArrayList<Integer> arraylist = dataMapItem.getDataMap().getIntegerArrayList(BuildConfig.W_some_other_key);
                        for (Integer i : arraylist)
                            Log.i(TAG, "Got integer " + i + " from array list");
                        intent = new Intent("REPLACE_THIS_WITH_A_STRING_OF_ANOTHER_ACTION_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY");
                        intent.putExtra("REPLACE_THIS_WITH_A_STRING_OF_INTEGER_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY", integer);
                        intent.putExtra("REPLACE_THIS_WITH_A_STRING_OF_ARRAYLIST_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY", arraylist);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        break;

                        //ajouté et utilisé
                    case BuildConfig.W_heart_rate_path:
                        int heartRate = dataMapItem.getDataMap()
                                .getInt(BuildConfig.W_heart_rate_key);

                        //TODO modification pour l'intent
                        //Pour la AutonomousFlightActivity (original)
                        /*
                        intent = new Intent(AutonomousFlightActivity.RECEIVE_HEART_RATE);
                        intent.putExtra(AutonomousFlightActivity.HEART_RATE, heartRate);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        */

                        //pour le mode Debug
                        ///*
                        intentDebug = new Intent(DebugAutonomousFlightActivity.RECEIVE_HEART_RATE);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.HEART_RATE, heartRate);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intentDebug);
                        //*/

                        //La classe Bebop ne devrait rien faire avec ça-->(C'est optionel)

                        break;

                        //Pour le GPS
                    case BuildConfig.W_location_path:
                        double longitude = dataMapItem.getDataMap().getDouble(BuildConfig
                                .W_longitude_key);
                        double latitude = dataMapItem.getDataMap().getDouble(BuildConfig
                            .W_latitude_key);
                        double altitude = dataMapItem.getDataMap().getDouble(BuildConfig
                                .W_altitude_key);

                        //TODO modification ou les info vont

                        //Pour la AutonomousFlightActivity
                        /*
                        intent = new Intent(AutonomousFlightActivity.RECEIVED_LOCATION);
                        intent.putExtra(AutonomousFlightActivity.LONGITUDE, longitude);
                        intent.putExtra(AutonomousFlightActivity.LATITUDE, latitude);
                        intent.putExtra(AutonomousFlightActivity.ALTITUDE, altitude);
                         */

                        //Pour DeBug DebugAutonomousFlightActivity (ça marche)
                        ///*
                        intentDebug = new Intent(DebugAutonomousFlightActivity.RECEIVED_LOCATION);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.LONGITUDE, longitude);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.LATITUDE, latitude);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.ALTITUDE, altitude);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intentDebug);//For debug
                        //*/

                        //Pour la classe BebopDrone (a la demande de Anthony)

                        intent = new Intent(BebopDrone.RECEIVED_LOCATION);
                        intent.putExtra(BebopDrone.LONGITUDE, longitude);
                        intent.putExtra(BebopDrone.LATITUDE, latitude);
                        intent.putExtra(BebopDrone.ALTITUDE, altitude);


                        //Pour la Activite ManualFlightActivity
                        intent = new Intent(ManualFlightActivity.RECEIVED_LOCATION);
                        intent.putExtra(ManualFlightActivity.LONGITUDE, longitude);
                        intent.putExtra(ManualFlightActivity.LATITUDE, latitude);
                        intent.putExtra(ManualFlightActivity.ALTITUDE, altitude);

                        //A modifier pour Autonomus Activity OU BebopDrone class OU ManualFlightActivity
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        Log.i(TAG, "GPS send to DebugAutonomousFlight Activity and BebopDrone");
                        break;

                    case BuildConfig.W_acceleration_path:
                        double acceleration = dataMapItem.getDataMap().getDouble(BuildConfig
                                .W_acceleration_key);
                        boolean mouvement = dataMapItem.getDataMap().getBoolean(BuildConfig
                                .W_mouvement_key);
                        Log.i(TAG, (String.format("Wear App Service Accel: %s Mouve: %s", acceleration,mouvement)));
                        //For debug
                        intentDebug = new Intent(DebugAutonomousFlightActivity.RECEIVED_ACCELERATION);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.ACCELERATIONVAR, acceleration);
                        intentDebug.putExtra(DebugAutonomousFlightActivity.MOUVEMENT, mouvement);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intentDebug);//For debug

                        //Send to bebopDron class
                        /*
                        intent = new Intent(BebopDrone.RECEIVED_ACCELERATION);
                        intent.putExtra(BebopDrone.ACCELERATIONVAR, acceleration);
                        intent.putExtra(BebopDrone.MOUVEMENT, mouvement);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        */

                        //Send to ManualFlightActivity
                        intent = new Intent(ManualFlightActivity.RECEIVED_ACCELERATION);
                        intent.putExtra(ManualFlightActivity.ACCELERATIONVAR, acceleration);
                        intent.putExtra(ManualFlightActivity.MOUVEMENT, mouvement);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                        break;
                    default:
                        Log.v(TAG, "Data changed for unhandled path: " + uri);
                        break;
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.w(TAG, "DataItem deleted: " + event.getDataItem().toString());
            }

            // For demo, send a acknowledgement message back to the node that created the data item
            sendMessage("Received data OK!", BuildConfig.W_path_acknowledge, uri.getHost());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // A message has been received from the Wear API
        // Get the URI of the event
        String path = messageEvent.getPath();
        String data = new String(messageEvent.getData());
        Log.v(TAG, "Received a message for path " + path
                + " : \"" + data
                + "\", from node " + messageEvent.getSourceNodeId());

        if (path.equals(BuildConfig.W_path_start_activity)
                && data.equals(BuildConfig.W_mainactivity)) {
            startActivity(new Intent(this, AutonomousFlightActivity.class));//Je sais pas s'il faut modifier
        }

        switch (path) {
            case BuildConfig.W_path_start_activity:
                Log.v(TAG, "Message asked to open Activity");
                Intent startIntent = null;
                switch (data) {
                    case BuildConfig.W_mainactivity:
                        startIntent = new Intent(this, AutonomousFlightActivity.class);//Je sais pas bis
                        break;
                }

                if (startIntent == null) {
                    Log.w(TAG, "Asked to start unhandled activity: " + data);
                    return;
                }
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
                break;
            case BuildConfig.W_path_acknowledge:
                Log.v(TAG, "Received acknowledgment");
                break;
            case BuildConfig.W_example_path_text:
                Log.v(TAG, "Message contained text. Return a datamap for demo purpose");
                ArrayList<Integer> arrayList = new ArrayList<>();
                Collections.addAll(arrayList, 5, 7, 9, 10);

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_datamap);
                putDataMapRequest.getDataMap().putInt(BuildConfig.W_a_key, 42);
                putDataMapRequest.getDataMap().putIntegerArrayList(BuildConfig.W_some_other_key, arrayList);
                sendPutDataMapRequest(putDataMapRequest);
                break;
            default:
                Log.w(TAG, "Received a message for unknown path " + path + " : " + new String(messageEvent.getData()));
        }
    }
//---------------------------------------------------------NOT MODIFIY AFTER THIS----------------------------------------------------------------//
    private void sendMessage(String message, String path, final String nodeId) {
        // Sends a message through the Wear API
        Wearable.getMessageClient(this)
                .sendMessage(nodeId, path, message.getBytes())
                .addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.v(TAG, "Sent message to " + nodeId + ". Result = " + integer);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Message not sent. " + e.getMessage());
                    }
                });
    }

    private void sendMessage(String message, String path) {
        // Send message to ALL connected nodes
        sendMessageToNodes(message, path);
    }

    void sendMessageToNodes(final String message, final String path) {
        Log.v(TAG, "Sending message " + message);
        // Lists all the nodes (devices) connected to the Wear API
        Wearable.getNodeClient(this).getConnectedNodes().addOnCompleteListener(new OnCompleteListener<List<Node>>() {
            @Override
            public void onComplete(@NonNull Task<List<Node>> listTask) {
                List<Node> nodes = listTask.getResult();
                for (Node node : nodes) {
                    Log.v(TAG, "Try to send message to a specific node");
                    WearService.this.sendMessage(message, path, node.getId());
                }
            }
        });
    }

    //FOR DATA MAP (not used)
    void sendPutDataMapRequest(PutDataMapRequest putDataMapRequest) {
        putDataMapRequest.getDataMap().putLong("time", System.nanoTime());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        Wearable.getDataClient(this)
                .putDataItem(request)
                .addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.v(TAG, "Sent datamap.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Datamap not sent. " + e.getMessage());
                    }
                });
    }

    private void bitmapFromAsset(Asset asset, final Intent intent, final String extraName) {
        // Reads an asset from the Wear API and parse it as an image
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        // Convert asset and convert it back to an image
        Wearable.getDataClient(this).getFdForAsset(asset)
                .addOnCompleteListener(new OnCompleteListener<DataClient.GetFdForAssetResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataClient.GetFdForAssetResponse> runnable) {
                        Log.v(TAG, "Got bitmap from asset");
                        InputStream assetInputStream = runnable.getResult().getInputStream();
                        Bitmap bmp = BitmapFactory.decodeStream(assetInputStream);

                        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                        byte[] bytes = byteStream.toByteArray();
                        intent.putExtra(extraName, bytes);
                        LocalBroadcastManager.getInstance(WearService.this).sendBroadcast(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception runnable) {
                        Log.e(TAG, "Failed to get bitmap from asset");
                    }
                });
    }
    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        bitmap = resizeImage(bitmap, 390);

        if (bitmap != null) {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private static Bitmap resizeImage(Bitmap bitmap, int newSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Image smaller, return it as is!
        if (width <= newSize && height <= newSize) return bitmap;

        int newWidth;
        int newHeight;

        if (width > height) {
            newWidth = newSize;
            newHeight = (newSize * height) / width;
        } else if (width < height) {
            newHeight = newSize;
            newWidth = (newSize * width) / height;
        } else {
            newHeight = newSize;
            newWidth = newSize;
        }

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bitmap, 0, 0,
                width, height, matrix, true);
    }
}
