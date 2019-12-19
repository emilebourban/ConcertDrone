package ch.epfl.concertdrone.activity;

import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

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
        Intent intentStartPlayActivity = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intentStartPlayActivity, START_DEVICE_LIST);
    }




    public void OpenGallery(View view) {
        // TODO: implement a function leading to the footages (pictures + videos) taken by the drone
        // --> cf.: https://stackoverflow.com/questions/16928727/open-gallery-app-from-android-intent/23821227
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent , RESULT_GALLERY );
    }
}
