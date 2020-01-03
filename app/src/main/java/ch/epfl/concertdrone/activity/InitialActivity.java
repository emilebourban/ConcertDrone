package ch.epfl.concertdrone.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ch.epfl.concertdrone.R;

public class InitialActivity extends AppCompatActivity {

    private static final String TAG = "ManualFlightActivity";

    private static final int START_DEVICE_LIST = 1;

    public static final int RESULT_GALLERY = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "entering onCreate InitialActivity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
    }


    // XML callback of the button of MainActivity
    // (leading from MainActivity to PlayActivity)
    public void StartDeviceListActivity(View view) {
        Intent intentToDeviceListActivity = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intentToDeviceListActivity, START_DEVICE_LIST);
    }


    public void OpenGallery(View view) {
        // Leads to the footages (pictures + videos) taken by the drone
        // --> cf.: https://stackoverflow.com/questions/16928727/open-gallery-app-from-android-intent/23821227
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent , RESULT_GALLERY );
    }



    public void OpenWifiSettings(View view) {
        // Leads to wifi settings in order to eventually connect to the drone if it doesn't do it automatically
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent);
    }



    //For debugging, acces to AutonomousFlightActivity without pass to Device ListaActivty
    public void OnClicktoAutonomus(View view) {
        Intent intentToAutonomusFlight = new Intent(this, DebugAutonomousFlightActivity.class);
        startActivity(intentToAutonomusFlight);
        Toast.makeText(getApplicationContext(), "Debug AutonomusFlight", Toast.LENGTH_SHORT).show();
    }


}
