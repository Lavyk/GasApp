package com.lavyk.gas;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.FOREGROUND_SERVICE;

public class GpsActivity extends AppCompatActivity implements View.OnClickListener {
    private final int PERMISSION_REQUEST_CODE = 200;

    Switch swtGps;
    Boolean switchGpsState = false;

    public BackgroundLocationService gpsService;
    public boolean mTracking = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        setWidgetIds();

        //prepare service
      //final Intent intent = new Intent(this, BackgroundLocationService.class);

        //startService(intent);
        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


/*        if(isMyServiceRunning(BackgroundLocationService.class)) {
            Toast.makeText(this, "Background Service Running ", Toast.LENGTH_SHORT).show();
            mTracking = true;
            swtGps.setChecked(true);
            switchGpsState = true;
            startTracking();
        } else {
            Toast.makeText(this, "Background Service not Running ", Toast.LENGTH_SHORT).show();
            mTracking = true;
            swtGps.setChecked(true);
            switchGpsState = true;
        }*/

        if(isMyServiceRunning(BackgroundLocationService.class)) {
            Toast.makeText(getApplicationContext(), "gpsService Online", Toast.LENGTH_SHORT).show();
            mTracking = true;
            swtGps.setChecked(true);
            switchGpsState = true;
            Intent intent = new Intent(this, BackgroundLocationService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            Toast.makeText(getApplicationContext(), "gpsService Offline", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void setWidgetIds() {
        swtGps = (Switch) findViewById(R.id.switch1);
        swtGps.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch1:
                if (swtGps.isChecked()) {
                    startTracking();
                } else {
                    stopTracking();
                }
                switchGpsState = isMyServiceRunning(BackgroundLocationService.class);
                //Toast.makeText(getApplicationContext(), "Switch1 :" + switchGpsState, Toast.LENGTH_LONG).show(); // display the current state for switch's

                break;
        }
    }

    public void startTracking() {
        //check for permission
        if (ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED) {

            Intent intent = new Intent(this, BackgroundLocationService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            if(gpsService != null) {
                                Toast.makeText(getApplicationContext(), "gpsService Online", Toast.LENGTH_SHORT).show();
                                gpsService.startTracking();
                                mTracking = true;
                            } else {
                                Toast.makeText(getApplicationContext(), "gpsService Offline", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 1000);


        } else {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, FOREGROUND_SERVICE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            }
        }
    }

    public void stopTracking() {
        Intent intent = new Intent(this, BackgroundLocationService.class);
        stopService(intent);
        mTracking = false;
        gpsService.stopTracking();
    }

    private void openSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundLocationService")) {
                gpsService = ((BackgroundLocationService.LocationServiceBinder) service).getService();
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundLocationService")) {
                gpsService = null;
            }
        }
    };
}
