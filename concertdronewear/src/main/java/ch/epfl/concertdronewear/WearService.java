package ch.epfl.concertdronewear;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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

import static android.app.Service.START_NOT_STICKY;

public class WearService extends WearableListenerService {

    // Tag for Logcat
    private static final String TAG = "WearService";

    //String code for the variable transmited
    public static final String HEART_RATE = "HEART_RATE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String ALTITUDE = "ALTITUDE";

    //String code for the COmand Transmited
    public static final String ACTIVITY_TO_START = "ACTIVITY_TO_START";
    public static final String MESSAGE = "MESSAGE";
    public static final String DATAMAP_INT = "DATAMAP_INT";
    public static final String DATAMAP_INT_ARRAYLIST = "DATAMAP_INT_ARRAYLIST";
    public static final String IMAGE = "IMAGE";
    public static final String PATH = "PATH";
    // Constants
    public enum ACTION_SEND {
        STARTACTIVITY, MESSAGE, EXAMPLE_DATAMAP, EXAMPLE_ASSET,EXAMPLE_SEND_STRING,
        HEART_RATE, LOCATION
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // If no action defined, return
        if (intent.getAction() == null) return START_NOT_STICKY;

        // Match against the given action
        ACTION_SEND action = ACTION_SEND.valueOf(intent.getAction());
        PutDataMapRequest putDataMapRequest;
        switch (action) {
            case STARTACTIVITY://Necesarry
                //Sart an Activty of the tablette
                String activity = intent.getStringExtra(ACTIVITY_TO_START);
                sendMessage(activity, BuildConfig.W_path_start_activity);
                break;
            case MESSAGE://Usefull
                String message = intent.getStringExtra(MESSAGE);
                if (message == null) message = "error";
                sendMessage(message, intent.getStringExtra(PATH));
                break;
            case EXAMPLE_DATAMAP://Not used
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_datamap);
                putDataMapRequest.getDataMap().putInt(BuildConfig.W_a_key, intent.getIntExtra(DATAMAP_INT, -1));
                putDataMapRequest.getDataMap().putIntegerArrayList(BuildConfig.W_some_other_key, intent.getIntegerArrayListExtra(DATAMAP_INT_ARRAYLIST));
                sendPutDataMapRequest(putDataMapRequest);
                break;
            case EXAMPLE_ASSET: //Not used
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_asset);
                putDataMapRequest.getDataMap().putAsset(BuildConfig.W_some_other_key, (Asset) intent.getParcelableExtra(IMAGE));
                sendPutDataMapRequest(putDataMapRequest);
                break;
                //Ajouté pour test
            case HEART_RATE://OPTIONAL
                putDataMapRequest = PutDataMapRequest
                        .create(BuildConfig.W_heart_rate_path);
                putDataMapRequest.getDataMap()
                        .putInt(BuildConfig.W_heart_rate_key,
                                intent.getIntExtra(HEART_RATE, -1));
                sendPutDataMapRequest(putDataMapRequest);
                break;
            case LOCATION://NECESARRY
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_location_path);
                putDataMapRequest.getDataMap().putDouble(BuildConfig.W_latitude_key, intent
                        .getDoubleExtra(LATITUDE, -1));
                putDataMapRequest.getDataMap().putDouble(BuildConfig.W_longitude_key, intent
                        .getDoubleExtra(LONGITUDE, -1));
                putDataMapRequest.getDataMap().putDouble(BuildConfig.W_altitude_key, intent
                        .getDoubleExtra(ALTITUDE, -1));
                sendPutDataMapRequest(putDataMapRequest);
                break;
            //Rajouté
            /*case EXAMPLE_SEND_STRING:
                // Example action of sending a String received from the MainActivity
                String message_to_send = intent.getStringExtra(MainActivity
                        .EXAMPLE_INTENT_STRING_NAME_ACTIVITY_TO_SERVICE);
                sendMessage(message_to_send, BuildConfig.W_path_example_message);
                Log.i(TAG, "Send String");
              //Il faudra rajouter ceci si on veut cominique de la montre a la tablette
                  public static final String EXAMPLE_INTENT_STRING_NAME_ACTIVITY_TO_SERVICE =
            "EXAMPLE_INTENT_STRING_NAME_ACTIVITY_TO_SERVICE"; (DANS LE MAIN ACTIVITY DE LA MONTRE)
             */
            default:
                Log.w(TAG, "Unknown action");
                break;
        }

        return START_NOT_STICKY;
    }



    //Will link the variables of the watch and the tablette as the same key W_....
    //when change the value. Not Used in this Wear service
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

                assert uri.getPath() != null;
                switch (uri.getPath()) {
                    case BuildConfig.W_example_path_asset://not used
                        // Extract the data behind the key you know contains data
                        Asset asset = dataMapItem.getDataMap().getAsset(BuildConfig.W_some_other_key);
                        intent = new Intent("REPLACE_THIS_WITH_A_STRING_OF_ACTION_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY");
                        bitmapFromAsset(asset, intent, "REPLACE_THIS_WITH_A_STRING_OF_IMAGE_PREFERABLY_DEFINED_AS_A_CONSTANT_IN_TARGET_ACTIVITY");
                        break;
                    case BuildConfig.W_example_path_datamap://For data map Not used
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

    //The information from the Tablette
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
            startActivity(new Intent(this, MainActivity.class));
        }

        switch (path) {
            case BuildConfig.W_path_start_activity://Ask to open a selected activity  of the wach
                Log.v(TAG, "Message asked to open Activity");
                Intent startIntent = null;
                switch (data) {
                    case BuildConfig.W_mainactivity://not used,
                        startIntent = new Intent(this, MainActivity.class);
                        break;
                        //Ajouté
                    case BuildConfig.W_recordingactivity://It will open recording activity
                        Log.d(TAG, "Start recording message received");
                        startIntent = new Intent(this, RecordingActivity.class);
                        break;
                }

                if (startIntent == null) {
                    Log.w(TAG, "Asked to start unhandled activity: " + data);
                    return;
                }
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
                break;
            case BuildConfig.W_path_stop_activity://Ask to stop an activity of the wach
                switch (data) {
                    case BuildConfig.W_recordingactivity://Stop the recording activity
                        Intent intentStop = new Intent();
                        intentStop.setAction(RecordingActivity.STOP_ACTIVITY);
                        LocalBroadcastManager.getInstance(WearService.this)
                                .sendBroadcast(intentStop);
                        break;
                }
                break;
            case BuildConfig.W_path_acknowledge://Debug, use to konw the path and show in Logcat
                Log.v(TAG, "Received acknowledgment");
                break;
            case BuildConfig.W_example_path_text://I dont know (Test??)(it was by Yann)
                Log.v(TAG, "Message contained text. Return a datamap for demo purpose");
                ArrayList<Integer> arrayList = new ArrayList<>();
                Collections.addAll(arrayList, 5, 7, 9, 10);

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_example_path_datamap);
                putDataMapRequest.getDataMap().putInt(BuildConfig.W_a_key, 42);
                putDataMapRequest.getDataMap().putIntegerArrayList(BuildConfig.W_some_other_key, arrayList);
                sendPutDataMapRequest(putDataMapRequest);
                break;

                //Rajouté
            case BuildConfig.W_path_example_message://recivei an mesage
                // The message received is already extracted in the `data` variable
                Intent intent = new Intent(MainActivity
                        .EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_MESSAGE_STRING_RECEIVED);
                intent.putExtra(MainActivity
                        .EXAMPLE_INTENT_STRING_NAME_WHEN_BROADCAST, data);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            default:
                Log.w(TAG, "Received a message for unknown path " + path + " : " + new String(messageEvent.getData()));
        }
    }

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

    //For Text Mesage (Used??)
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

    //For DATAMAP not used
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
    //FOR BITMAP-->Not Used
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
