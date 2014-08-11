package com.kii.demo.wearable;

import android.content.Intent;

import android.util.Log;

import com.mariux.teleport.lib.TeleportService;

public class WearService extends TeleportService {

    private static final String TAG = WearService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        setOnGetMessageTask(new StartActivityTask());
        //setOnGetMessageTask(new NotifyFromOnGetMessageTask());
    }

    //Task that shows the path of a received message
    public class StartActivityTask extends TeleportService.OnGetMessageTask {
        @Override
        protected void onPostExecute(String  path) {
            if (path.equals("startActivity")){

                Intent startIntent = new Intent(getBaseContext(), WearActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            } else {
                Log.i(TAG, "Got a message with path: " + path);
            }
            //let's reset the task (otherwise it will be executed only once)
            setOnGetMessageTask(new StartActivityTask());
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