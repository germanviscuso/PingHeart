package com.kii.demo.wearable;

import android.content.Intent;

import android.util.Log;

import com.mariux.teleport.lib.TeleportService;

public class WearService extends TeleportService {

    private static final String TAG = WearService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        setOnGetMessageTask(new ActivityManagementTask());
        //setOnGetMessageTask(new NotifyFromOnGetMessageTask());
    }

    //Task that shows the path of a received message
    public class ActivityManagementTask extends TeleportService.OnGetMessageTask {
        @Override
        protected void onPostExecute(String  path) {
            if (path.equals("startActivity")){
                Intent startIntent = new Intent(getBaseContext(), WearActivity.class);
                startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startIntent.putExtra("keep", true);
                startActivity(startIntent);
            } else if (path.equals("stopActivity")) {
                Intent stopIntent = new Intent(getBaseContext(), WearActivity.class);
                stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                stopIntent.putExtra("keep", false);
                startActivity(stopIntent);
            } else {
                Log.i(TAG, "Got a message with path: " + path);
            }
            //let's reset the task (otherwise it will be executed only once)
            setOnGetMessageTask(new ActivityManagementTask());
        }
    }

    //Task that shows the path of a received message
    public class NotifyFromOnGetMessageTask extends TeleportService.OnGetMessageTask {

        @Override
        protected void onPostExecute(String  path) {
            Log.i(TAG, "Got a message with path: " + path);
            //let's reset the task (otherwise it will be executed only once)
            setOnGetMessageTask(new NotifyFromOnGetMessageTask());
        }
    }

}