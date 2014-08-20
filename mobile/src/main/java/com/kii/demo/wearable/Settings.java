package com.kii.demo.wearable;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class Settings {
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";
    private static String loginToken = null;
    private static final String ACCESSTOKEN = "LOGINTOKEN";
    private static final String PREF_NAME = "SETTINGS";


    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeUUIDFile(installation);
                sID = readFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    public synchronized static String loadAccessToken(Context context) {
        if (loginToken == null) {
            File tokenFile = new File(context.getFilesDir(), ACCESSTOKEN);
            try {
                if (tokenFile.exists())
                    loginToken = readFile(tokenFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return loginToken;
    }

    public synchronized static String saveAccessToken(Context context, String token) {
        File tokenFile = new File(context.getFilesDir(), ACCESSTOKEN);
        try {
            writeFile(tokenFile, token);
            loginToken = token;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return loginToken;
    }

    public synchronized static boolean deleteLoginToken(Context context){
        boolean success = deleteLocalFile(context, ACCESSTOKEN);
        if(success)
            loginToken = null;
        return success;
    }

    public synchronized static boolean deleteInstallation(Context context){
        boolean success = deleteLocalFile(context, INSTALLATION);
        if(success)
            sID = null;
        return success;
    }

    private synchronized static boolean deleteLocalFile(Context context, String filename){
        File file = new File(context.getFilesDir(), filename);
        boolean success = false;
        try {
            if (file.exists())
                success = deleteFile(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    private static String readFile(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeUUIDFile(File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file, false);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    private static void writeFile(File file, String data) throws IOException {
        FileOutputStream out = new FileOutputStream(file, false);
        out.write(data.getBytes());
        out.close();
    }

    private static boolean deleteFile(File file) throws IOException {
        return file.delete();
    }
}
