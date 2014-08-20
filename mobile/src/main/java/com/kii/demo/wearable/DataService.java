package com.kii.demo.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class DataService extends Service {

    private static final String TAG = DataService.class.getSimpleName();
    public static int timeGap = 60; // in seconds
    private Timer timer;

    private TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            Log.i(TAG, "Timer task doing work");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Creating data service");

        timer = new Timer("DataTimer");
        timer.schedule(updateTask, 1000L, timeGap * 1000L);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying service");

        timer.cancel();
        timer = null;
    }
}
