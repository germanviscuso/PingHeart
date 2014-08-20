package com.kii.demo.wearable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;


/**
 * A login screen that offers login via 4 digit pin.

 */
public class AuthActivity extends Activity {

    private static final String TAG = AuthActivity.class.getName();


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserSignInTask mSignInTask = null;
    private UserRegistrationTask mRegisterTask = null;
    private UserTokenSignInTask mTokenSignInTask = null;

    // UI references.
    private EditText mPasswordView;
    private EditText mPasswordVerifyView;
    private CheckBox mRememberCheckbox;
    private boolean rememberMe = false;
    private View mProgressView;
    private View mAuthFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        Kii.initialize(AppConfig.KII_APP_ID, AppConfig.KII_APP_KEY, AppConfig.KII_SITE);

        // Set up the auth form.
        mPasswordView = (EditText) findViewById(R.id.passw);
        mPasswordVerifyView = (EditText) findViewById(R.id.passwVerify);
        mRememberCheckbox = (CheckBox) findViewById(R.id.rememberBox);

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
        mRememberCheckbox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rememberMe = ((CheckBox) v).isChecked();
            }
        });

        mAuthFormView = findViewById(R.id.auth_form);
        mProgressView = findViewById(R.id.progressBar);

        // Now that the UI is set up we try to login via previous user access token
        tryLoginWithToken();
    }

    private void tryLoginWithToken() {
        String token = Settings.loadAccessToken(this);
        if(token != null){
            Log.d(TAG, "Token: " + token);
            showProgress(true);
            mTokenSignInTask = new UserTokenSignInTask(this, token);
            mTokenSignInTask.execute((Void) null);
        }
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
                mSignInTask = new UserSignInTask(this, password, rememberMe);
                mSignInTask.execute((Void) null);
            }
            else {
                mRegisterTask = new UserRegistrationTask(this, password, rememberMe);
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
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPassword;
        private final boolean mRememberMe;

        UserSignInTask(Context context, String password, boolean rememberMe) {
            mContext = context;
            mPassword = password;
            mRememberMe = rememberMe;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt sign in against Kii Cloud
            try {
                String id = Settings.id(mContext);
                Log.d(TAG, "Attempting sign in with id: " + id);
                KiiUser.logIn(id, mPassword);
                if(mRememberMe) {
                    Log.d(TAG, "Storing access token...");
                    String accessToken = KiiUser.getCurrentUser().getAccessToken();
                    // Now we store the token in a local file
                    Settings.saveAccessToken(mContext, accessToken);
                }
            } catch (Exception e) {
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
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
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
                Log.e(TAG, e.toString());
                return false;
            }
            Log.d(TAG, "Sign in successful. User id: " + KiiUser.getCurrentUser().getUsername());
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mTokenSignInTask = null;
            showProgress(false);
            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.e(TAG, "Error signing in with token");
            }
        }

        @Override
        protected void onCancelled() {
            mTokenSignInTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous registration task used to authenticate
     * the user.
     */
    public class UserRegistrationTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPassword;
        private final boolean mRememberMe;

        UserRegistrationTask(Context context, String password, boolean rememberMe) {
            mContext = context;
            mPassword = password;
            mRememberMe = rememberMe;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt registration against Kii Cloud
            try {
                String id = Settings.id(mContext);
                Log.d(TAG, "Attempting registration with id: " + id);
                KiiUser.Builder builder = KiiUser.builderWithName(id);
                KiiUser user = builder.build();
                user.register(mPassword);
                if(mRememberMe) {
                    Log.d(TAG, "Storing access token...");
                    String accessToken = KiiUser.getCurrentUser().getAccessToken();
                    // Now we store the token in a local file
                    Settings.saveAccessToken(mContext, accessToken);
                }
            }
              catch (Exception e) {
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
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
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
