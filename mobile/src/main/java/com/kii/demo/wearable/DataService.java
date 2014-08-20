package com.kii.demo.wearable;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.mariux.teleport.lib.TeleportClient;

import java.util.Timer;
import java.util.TimerTask;

public class DataService extends Service {

    private static final String TAG = DataService.class.getSimpleName();

    private UserTokenSignInTask mTokenSignInTask = null;
    TeleportClient mTeleportClient;
    private static double lastHRValue = 0.0;
    public static int period = 3000; // in seconds
    public static int duration = period + 60; // in seconds, always less than period
    private Timer startTimer;
    private Timer stopTimer;

    private TimerTask startTask = new TimerTask() {
        @Override
        public void run() {
            Log.i(TAG, "Start timer called");
            if (mTeleportClient != null)
                mTeleportClient.sendMessage(AppConfig.START_ACTIVITY, null);
        }
    };

    private TimerTask stopTask = new TimerTask() {
        @Override
        public void run() {
            Log.i(TAG, "Stop timer called");
            if (mTeleportClient != null)
                mTeleportClient.sendMessage(AppConfig.STOP_ACTIVITY, null);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Creating service");
        Kii.initialize(AppConfig.KII_APP_ID, AppConfig.KII_APP_KEY, AppConfig.KII_SITE);
        mTeleportClient = new TeleportClient(this);
        //let's set the two task to be executed when a message is received
        mTeleportClient.setOnGetMessageTask(new SaveHRFromOnGetMessageTask());
        mTeleportClient.connect();
        loginWithToken();
        startTimer = new Timer("StartTimer");
        if(AppConfig.AUTONOMOUS_OPERATION)
            startTimer.schedule(startTask, 1000L, period * 1000L);
        stopTimer = new Timer("StopTimer");
        if(AppConfig.AUTONOMOUS_OPERATION)
            stopTimer.schedule(stopTask, 1000L, duration * 1000L);
    }

    private void loginWithToken() {
        if(KiiUser.getCurrentUser() == null) {
            Log.d(TAG, "Not logged in. Retrieving access token...");
            String token = Settings.loadAccessToken(this);
            if(token != null){
                Log.d(TAG, "Token: " + token);
                mTokenSignInTask = new UserTokenSignInTask(this, token);
                mTokenSignInTask.execute((Void) null);
            }
        }
    }

    public class SaveHRFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {

        @Override
        protected void onPostExecute(String path) {
            if(KiiUser.getCurrentUser() != null) {
                try {
                    double value = Double.valueOf(path);
                    if (value != lastHRValue) {
                        KiiBucket bucket = Kii.user().bucket(AppConfig.USER_BUCKET);
                        KiiObject object = bucket.object();
                        object.set("value", value);
                        object.save(new KiiObjectCallBack() {
                            @Override
                            public void onSaveCompleted(int token, KiiObject object, Exception exception) {
                                Log.d(TAG, "Heart rate data sent to cloud");
                            }
                        });
                        lastHRValue = value;
                    }
                } catch (Exception e) {
                    //not a heart rate value, discard
                }
            } else {
                Log.d(TAG, "User has not signed in");
            }
            //let's reset the task (otherwise it will be executed only once)
            mTeleportClient.setOnGetMessageTask(new SaveHRFromOnGetMessageTask());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying service");
        //if (mTeleportClient != null)
            //mTeleportClient.sendMessage(AppConfig.STOP_ACTIVITY, null);
        mTeleportClient.disconnect();
        startTimer.cancel();
        startTimer = null;
        stopTimer.cancel();
        stopTimer = null;
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user via an access token.
     */
    public class UserTokenSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mToken;

        UserTokenSignInTask(Context context, String token) {
            mContext = context;
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt sign in against Kii Cloud using an access token
            try {
                Log.d(TAG, "Attempting sign in with access token");
                KiiUser.loginWithToken(mToken);
            } catch (Exception e) {
                return false;
            }
            Log.d(TAG, "Sign in successful. User id: " + KiiUser.getCurrentUser().getUsername());
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mTokenSignInTask = null;
            if (!success) {
                Log.e(TAG, "Error signing in with token");
            }
        }

        @Override
        protected void onCancelled() {
            mTokenSignInTask = null;
        }
    }
}
