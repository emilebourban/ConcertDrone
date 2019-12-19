package ch.epfl.concertdrone.activity;

import android.util.Log;

import com.parrot.arsdk.ardiscovery.UsbAccessoryActivity;

public class UsbAccessoryActivityImpl extends UsbAccessoryActivity {
    private static final String TAG = "UsbAccessoryActivity";
    @Override
    protected Class getBaseActivity() {
        Log.i(TAG, "entering getBaseActivity UsbAccessoryActivity");
        return DeviceListActivity.class;
    }
}
