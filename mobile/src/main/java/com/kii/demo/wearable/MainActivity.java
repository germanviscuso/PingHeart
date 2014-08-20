package com.kii.demo.wearable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.mariux.teleport.lib.TeleportClient;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    // UI references.
    private TextView mHRView;

    TeleportClient mTeleportClient;

    private static double lastHRValue = 0.0;

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

        Button mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(DataService.class.getName()));
                //mTeleportClient.sendMessage(AppConfig.START_ACTIVITY, null);
            }
        });

        Button mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(DataService.class.getName()));
                //mTeleportClient.sendMessage(AppConfig.STOP_ACTIVITY, null);
            }
        });
    }

    public class ShowHRFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {

        @Override
        protected void onPostExecute(String path) {
            //Toast.makeText(getApplicationContext(), "Message - " + path, Toast.LENGTH_SHORT).show();
            try {
                double value = Double.valueOf(path);
                mHRView.setText(path);
                /*if(value != lastHRValue) {
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
                }*/
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
