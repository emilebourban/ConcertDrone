package ch.epfl.concertdronewear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
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
    private TextView TextCom; //Text d'essay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled(); //Set the save energy mode-->Enables Always-on

        //For Debug
        mTextView = findViewById(R.id.text);
        TextCom =findViewById(R.id.textCom);
        TextCom.setText("En attente de la Tablette");
    }

    //Fonction for DEBUG. It recive a String from the tablete to see if is well conected to the Wacth
    private BroadcastReceiver mBroadcastReveiverString = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextCom.setText(
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

        // Un-register broadcasts from WearService (For save energy)
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(mBroadcastReveiverString);
    }

    //Ambietn Mode Fontions
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

}
