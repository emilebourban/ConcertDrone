package ch.epfl.concertdronewear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends WearableActivity {

    public static final String
            EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_MESSAGE_STRING_RECEIVED =
            "EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_MESSAGE_STRING_RECEIVED";
    public static final String
            EXAMPLE_INTENT_STRING_NAME_WHEN_BROADCAST =
            "EXAMPLE_INTENT_STRING_NAME_WHEN_BROADCAST";
    public static final String
            EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_IMAGE_DATAMAP_RECEIVED =
            "EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_IMAGE_DATAMAP_RECEIVED";
    public static final String
            EXAMPLE_INTENT_IMAGE_NAME_WHEN_BROADCAST =
            "EXAMPLE_INTENT_IMAGE_NAME_WHEN_BROADCAST";

    private TextView mTextView;
    private TextView TextTry; //Text d'essay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        mTextView = findViewById(R.id.text);
        TextTry =findViewById(R.id.textTry);

        // Enables Always-on
        setAmbientEnabled();
    }

    //Fontion qui ressoi le string envoye par la tablette
    private BroadcastReceiver mBroadcastReveiverString = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextTry.setText(
                    intent.getStringExtra(EXAMPLE_INTENT_STRING_NAME_WHEN_BROADCAST));
        }
    };

    protected void onResume() {
        super.onResume();

        // Register broadcasts from WearService
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(mBroadcastReveiverString, new IntentFilter(
                        EXAMPLE_BROADCAST_NAME_FOR_NOTIFICATION_MESSAGE_STRING_RECEIVED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Un-register broadcasts from WearService
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(mBroadcastReveiverString);
    }

    //Esto es para el modo ambiente si hace falta
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

}
