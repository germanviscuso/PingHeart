package com.kii.demo.wearable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.KiiUser;
import com.kii.demo.wearable.R;
import com.mariux.teleport.lib.TeleportClient;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    // UI references.
    private TextView mHRView;

    // Teleport references
    private static final String STARTACTIVITY = "startActivity";
    TeleportClient mTeleportClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the message from the intent
        Intent intent = getIntent();
        //String message = intent.getStringExtra(AuthActivity.EXTRA_MESSAGE);
        setContentView(R.layout.activity_main);

        mTeleportClient = new TeleportClient(this);

        mHRView = (TextView) findViewById(R.id.heartRate);
        Button mReadHRButton = (Button) findViewById(R.id.hr_button);

        mReadHRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestHeartRate();
            }
        });
    }

    private void requestHeartRate() {
        sendMessage(STARTACTIVITY);
    }

    /**
     * Send message to Wear device via Teleport
     */
    public void sendMessage(String msg) {
        mTeleportClient.setOnGetMessageTask(new ShowHRFromOnGetMessageTask());
        mTeleportClient.sendMessage(msg, null);
    }

    public class ShowHRFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {
        @Override
        protected void onPostExecute(String path) {
            //Toast.makeText(getApplicationContext(), "Message - " + path, Toast.LENGTH_SHORT).show();
            try {
                float value = Float.valueOf(path);
                mHRView.setText(path);
            }
            catch(Exception e){
                //not a heart rate value, disregard
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
            //TODO
            return true;
        }
        if (id == R.id.action_logout) {
            Installation.deleteLoginToken(this);
            KiiUser.logOut();
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
