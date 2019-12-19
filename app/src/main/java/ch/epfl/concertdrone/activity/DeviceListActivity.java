package ch.epfl.concertdrone.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.epfl.concertdrone.R;
import ch.epfl.concertdrone.discovery.DroneDiscoverer;

public class DeviceListActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE";

    private static final String TAG = "DeviceListActivity";

    /**
     * List of runtime permission we need.
     */
    private static final String[] PERMISSIONS_NEEDED = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    /**
     * Code for permission request result handling.
     */
    private static final int REQUEST_CODE_PERMISSIONS_REQUEST = 1;

    // this block loads the native libraries
    // it is mandatory
    static {
        ARSDK.loadSDKLibs();
    }

    private final List<ARDiscoveryDeviceService> mDronesList = new ArrayList<>();
    private final BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            Log.i(TAG, "entering getCount DeviceListActivity");

            return mDronesList.size();
        }

        @Override
        public Object getItem(int position) {
            Log.i(TAG, "entering getItem DeviceListActivity");

            return mDronesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            Log.i(TAG, "entering getItemId DeviceListActivity");

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i(TAG, "entering getView DeviceListActivity");

            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = getLayoutInflater();
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) rowView.findViewById(android.R.id.text1);
                rowView.setTag(viewHolder);
            }

            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) getItem(position);
            holder.text.setText(service.getName() + " on " + service.getNetworkType());

            return rowView;
        }
    };
    private final DroneDiscoverer.Listener mDiscovererListener = new DroneDiscoverer.Listener() {

        @Override
        public void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList) {
            mDronesList.clear();
            mDronesList.addAll(dronesList);

            mAdapter.notifyDataSetChanged();
        }
    };
    public DroneDiscoverer mDroneDiscoverer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        Log.i(TAG, "entering DeviceListActivity");

        final ListView listView = (ListView) findViewById(R.id.list);

        // Assign adapter to ListView
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // launch the activity related to the type of discovery device service
                Intent intent = null;

                ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) mAdapter.getItem(position);
                ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                switch (product) {
//                    case ARDISCOVERY_PRODUCT_ARDRONE:
                    case ARDISCOVERY_PRODUCT_BEBOP_2:
                        intent = new Intent(DeviceListActivity.this, ManualFlightActivity.class);
                        break;

//                    case ARDISCOVERY_PRODUCT_JS:
//                    case ARDISCOVERY_PRODUCT_JS_EVO_LIGHT:
//                    case ARDISCOVERY_PRODUCT_JS_EVO_RACE:
//                        intent = new Intent(DeviceListActivity.this, JSActivity.class);
//                        break;
//
//                    case ARDISCOVERY_PRODUCT_MINIDRONE:
//                    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK:
//                    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT:
//                    case ARDISCOVERY_PRODUCT_MINIDRONE_DELOS3:
//                        intent = new Intent(DeviceListActivity.this, MiniDroneActivity.class);
//                        break;
//                    case ARDISCOVERY_PRODUCT_MINIDRONE_WINGX:
//                        intent = new Intent(DeviceListActivity.this, SwingDroneActivity.class);
//                        break;

                    default:
                        Log.e(TAG, "The type " + product + " is not supported by this sample");
                }

                if (intent != null) {
                    intent.putExtra(EXTRA_DEVICE_SERVICE, service);
                    startActivity(intent);
                }
            }
        });

        mDroneDiscoverer = new DroneDiscoverer(this);

        Set<String> permissionsToRequest = new HashSet<>();
        for (String permission : PERMISSIONS_NEEDED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(this, "Please allow permission " + permission, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                } else {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    REQUEST_CODE_PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "entering onResume DeviceListActivity");

        super.onResume();

        // setup the drone discoverer and register as listener
        mDroneDiscoverer.setup();
        mDroneDiscoverer.addListener(mDiscovererListener);

        // start discovering
        mDroneDiscoverer.startDiscovering();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "entering onPause DeviceListActivity");

        super.onPause();

        // clean the drone discoverer object
        mDroneDiscoverer.stopDiscovering();
        mDroneDiscoverer.cleanup();
        mDroneDiscoverer.removeListener(mDiscovererListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "entering onRequestPermissionsResult DeviceListActivity");


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean denied = false;
        if (permissions.length == 0) {
            // canceled, finish
            denied = true;
        } else {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    denied = true;
                }
            }
        }

        if (denied) {
            Toast.makeText(this, "At least one permission is missing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    static class ViewHolder {
        public TextView text;
    }

}
