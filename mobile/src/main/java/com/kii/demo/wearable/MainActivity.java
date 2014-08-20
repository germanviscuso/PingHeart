package com.kii.demo.wearable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.kii.cloud.storage.KiiUser;
import com.mariux.teleport.lib.TeleportClient;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    // UI references.
    private TextView mHRView;

    TeleportClient mTeleportClient;

    private static boolean startedWatch = false;
    private static boolean startedService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the message from the intent
        Intent intent = getIntent();
        //String message = intent.getStringExtra(AuthActivity.EXTRA_MESSAGE);
        setContentView(R.layout.activity_main);


        mTeleportClient = new TeleportClient(this);
        //let's set the two task to be executed when a message is received
        mTeleportClient.setOnGetMessageTask(new ShowHRFromOnGetMessageTask());

        mHRView = (TextView) findViewById(R.id.heartRate);

        if(AppConfig.AUTONOMOUS_OPERATION){
            startService(new Intent(DataService.class.getName()));
            startedService = true;
        }
    }

    public class ShowHRFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {

        @Override
        protected void onPostExecute(String path) {
            //Toast.makeText(getApplicationContext(), "Message - " + path, Toast.LENGTH_SHORT).show();
            try {
                double value = Double.valueOf(path);
                mHRView.setText(path);
            }
            catch(Exception e){
                //not a heart rate value, discard
            }
            //let's reset the task (otherwise it will be executed only once)
            mTeleportClient.setOnGetMessageTask(new ShowHRFromOnGetMessageTask());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTeleportClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTeleportClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            //TODO add timer period setting
            return true;
        }
        if (id == R.id.action_startstop_watch) {
            if (mTeleportClient != null) {
                if (startedWatch){
                    mTeleportClient.sendMessage(AppConfig.STOP_ACTIVITY, null);
                    startedWatch = false;
                }
                else {
                    mTeleportClient.sendMessage(AppConfig.START_ACTIVITY, null);
                    startedWatch = true;
                }
            }
            return true;
        }
        if (id == R.id.action_startstop_service) {
            if(startedService) {
                stopService(new Intent(DataService.class.getName()));
                startedService = false;
            }
            else {
                startService(new Intent(DataService.class.getName()));
                startedService = true;
            }
            return true;
        }
        if (id == R.id.action_logout) {
            Settings.deleteLoginToken(this);
            KiiUser.logOut();
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
