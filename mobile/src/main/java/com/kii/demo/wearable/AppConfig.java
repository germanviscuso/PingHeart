package com.kii.demo.wearable;

import com.kii.cloud.storage.Kii;

public class AppConfig {
    // Kii config. Create an app at developer.kii.com and replace with your Kii app settings here
    public static final String KII_APP_ID = "073d2186";
    public static final String KII_APP_KEY = "27e4f6457e1daaa16d1bc7125073ce74";
    public static final Kii.Site KII_SITE = Kii.Site.US;
    public static final String USER_BUCKET = "heartrate";
    // Teleport config
    public static final String START_ACTIVITY = "startActivity";
    public static final String STOP_ACTIVITY = "stopActivity";
}
