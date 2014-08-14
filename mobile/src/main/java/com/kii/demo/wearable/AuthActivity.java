package com.kii.demo.wearable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.exception.app.AppException;
import com.mariux.teleport.lib.TeleportClient;

import java.io.IOException;


/**
 * A login screen that offers login via 4 digit pin.

 */
public class AuthActivity extends Activity {

    private static final String TAG = AuthActivity.class.getName();

    private static final String KII_APP_ID = "073d2186";
    private static final String KII_APP_KEY = "27e4f6457e1daaa16d1bc7125073ce74";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserSignInTask mSignInTask = null;
    private UserRegistrationTask mRegisterTask = null;

    private static final String STARTACTIVITY = "startActivity";
    TeleportClient mTeleportClient;

    // UI references.
    private EditText mPasswordView;
    private EditText mPasswordVerifyView;
    private View mProgressView;
    private View mAuthFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mTeleportClient = new TeleportClient(this);
        Kii.initialize(KII_APP_ID, KII_APP_KEY, Kii.Site.US);

        // Set up the auth form.
        mPasswordView = (EditText) findViewById(R.id.passw);
        mPasswordVerifyView = (EditText) findViewById(R.id.passwVerify);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        Button mRegisterButton = (Button) findViewById(R.id.register_button);

        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuth(false);
            }
        });

        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuth(true);
            }
        });

        mAuthFormView = findViewById(R.id.auth_form);
        mProgressView = findViewById(R.id.progressBar);
    }


    /**
     * Send message to Wear device via Teleport
     */
    public void sendMessage(String msg) {
        mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());
        mTeleportClient.sendMessage(msg, null);
    }

    public class ShowToastFromOnGetMessageTask extends TeleportClient.OnGetMessageTask {
        @Override
        protected void onPostExecute(String path) {
            Toast.makeText(getApplicationContext(), "Message - " + path, Toast.LENGTH_SHORT).show();
            //let's reset the task (otherwise it will be executed only once)
            mTeleportClient.setOnGetMessageTask(new ShowToastFromOnGetMessageTask());
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

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptAuth(boolean isRegistration) {
        if(!isRegistration) {
            if (mSignInTask != null) {
                return;
            }
        } else {
            if (mRegisterTask != null) {
                return;
            }
        }

        // Reset errors.
        mPasswordView.setError(null);
        mPasswordVerifyView.setError(null);

        // Store values at the time of the auth attempt.
        String password = mPasswordView.getText().toString();
        String passwordVerify = mPasswordVerifyView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || password.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else
        if (password.compareTo(passwordVerify) != 0) {
            mPasswordView.setError(getString(R.string.error_mismatch_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            if(!isRegistration) {
                mSignInTask = new UserSignInTask(this, password);
                mSignInTask.execute((Void) null);
            }
            else {
                mRegisterTask = new UserRegistrationTask(this, password);
                mRegisterTask.execute((Void) null);
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mAuthFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPassword;

        UserSignInTask(Context context, String password) {
            mContext = context;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt sign in against Kii Cloud
            try {
                String id = Installation.id(mContext);
                Log.d(TAG, "Attempting sign in with id: " + id);
                KiiUser.logIn(id, mPassword);
            } catch (IOException e) {
                return false;
            } catch (AppException e) {
                return false;
            }
            Log.d(TAG, "Sign in successful");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSignInTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mSignInTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserRegistrationTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPassword;

        UserRegistrationTask(Context context, String password) {
            mContext = context;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt registration against Kii Cloud
            try {
                String id = Installation.id(mContext);
                Log.d(TAG, "Attempting registration with id: " + id);
                KiiUser.Builder builder = KiiUser.builderWithName(id);
                KiiUser user = builder.build();
                user.register(mPassword);
            }
              catch (IllegalArgumentException e) {
                return false; // wrong format
            } catch (AppException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
            Log.d(TAG, "Registration successful");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}
