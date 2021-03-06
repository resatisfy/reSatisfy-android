package com.resatisfy.android_lib.controllers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import com.resatisfy.android_lib.RSPush;
import com.resatisfy.android_lib.utilities.RSConfig;
import com.resatisfy.android_lib.utilities.RSSettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;


import javax.net.ssl.HttpsURLConnection;



public class RSExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Context context;
    private Thread.UncaughtExceptionHandler defaultUEH;
    RSConfig getConfig = null;


    public RSExceptionHandler(Context ctx) {
        getConfig = RSConfig.defaultConfig(ctx);

        context = ctx;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

    }

    public void uncaughtException(final Thread tread, final Throwable error) {

        StringBuilder stackStrace = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            stackStrace.append(element.toString());
            stackStrace.append("==//==");
        }

//        String fileName = ""; String lineNumber = "";
//        try{
//            String fstEle = error.getStackTrace()[0].toString();
//            String[] fstEleExplode = fstEle.split("()");
//
//            System.out.println("--------------------------2");
//            System.out.println();
//        }catch (Exception e){  }


        Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter("appKey", getConfig.getAppKey());
        builder.appendQueryParameter("appSecret", getConfig.getAppSecret());
        builder.appendQueryParameter("deviceType", "android");

        try{
            builder.appendQueryParameter("message", error.getMessage());
        }catch (Exception e){  }

        try{
            if(error.getCause() != null){
                builder.appendQueryParameter("cause", error.getCause().toString());
            }else{
                builder.appendQueryParameter("cause", error.getMessage());
            }
        }catch (Exception e){  }


        builder.appendQueryParameter("stackTrace", stackStrace.toString());
        this.addExtraData(builder);

        final String postedQuery = builder.build().getEncodedQuery();


        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                //System.out.println("----------------//3-----------------------");
                try {
                    URL url = new URL(RSSettings.getApiUrl() + "post-report-android");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(postedQuery);
                    writer.flush();
                    writer.close();
                    os.close();


                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject responseobject = new JSONObject(bufferedReader.readLine());
                    conn.disconnect();
                    RSHttpsCompletion(responseobject);

                    /////defaultUEH.uncaughtException(tread, error);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    ////System.exit(1);

                } catch (Exception e) {
                    Log.e("RSCrashReporting : ", e.toString());
                }


                Looper.loop();
            }
        }.start();

        try { Thread.sleep(3000); }catch (Exception e){}

    }

    public void RSHttpsCompletion(JSONObject json) {
        try{
            String getStatus = json.getString("status");
            if(getStatus.equals("success")) {

//                    System.out.println("-----------------------//--------------------");
//                    System.out.println(json);

            } else if (!json.getString("msg").isEmpty()){
                Log.e("RSCrashReporting : ",json.getString("msg") );
            }
        }catch (Exception e){}
    }


    void addExtraData(Uri.Builder builder){

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            builder.appendQueryParameter("package",pi.packageName);
            builder.appendQueryParameter("version",pi.versionName);
        } catch (Exception e) {  }

        builder.appendQueryParameter("channelId",RSPush.channelId(context));


        builder.appendQueryParameter("android_version", android.os.Build.VERSION.RELEASE);
        builder.appendQueryParameter("board",android.os.Build.BOARD);
        builder.appendQueryParameter("brand",android.os.Build.BRAND);
        builder.appendQueryParameter("device",android.os.Build.DEVICE);
        builder.appendQueryParameter("host",android.os.Build.HOST);
        builder.appendQueryParameter("build_id",android.os.Build.ID);
        builder.appendQueryParameter("model",android.os.Build.MODEL);
        builder.appendQueryParameter("product",android.os.Build.PRODUCT);
        builder.appendQueryParameter("type",android.os.Build.TYPE);

        try {
            StatFs stat = getStatFs();
            builder.appendQueryParameter("total_internal_memory",getTotalInternalMemorySize(stat));
            builder.appendQueryParameter("available_internal_memory",getAvailableInternalMemorySize(stat));
        } catch (Exception e) {  }

    }


    private StatFs getStatFs() {
        File path = Environment.getDataDirectory();
        return new StatFs(path.getPath());
    }

    private String getAvailableInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long myLong = availableBlocks * blockSize;
        return Long.toString(myLong);
    }

    private String getTotalInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long myLong =  totalBlocks * blockSize;

        return Long.toString(myLong);
    }






}













