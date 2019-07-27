package de.rwth_aachen.phyphox;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UploadIntentService extends IntentService {
    public static int count = 0; // shared
    public static boolean ProfileUpload = false;
    public static long GpsCount = 0;
    public static JSONArray dataArray = new JSONArray();
    //private SharedPreferences preferences = this.getSharedPreferences("ProfileUpload",MODE_PRIVATE);
    //boolean isFirst = preferences.getBoolean("ProfileUpload", true);
    public List<String> timestamps = new ArrayList<String>();
    public MyDBHelper db = new MyDBHelper(this, "SensorData", null, 1);
    private PowerManager mgr;
    private PowerManager.WakeLock wakeLock;

    public UploadIntentService(String name) {
        super(name);
    }

    public UploadIntentService() {
        super("UploadIntentService");
    }

    @Override
    public void setIntentRedelivery(boolean enabled) {
        Log.d("TestService", "setIntentRedelivery()");
        super.setIntentRedelivery(enabled);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i("TestService", "onStartCommand");
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final Handler mHandler = new Handler();
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.i("TestService", "Service Start!");
        while(true) {
            NetworkInfo info = connManager.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) { }
            else {
                if (!info.isAvailable()) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                        }
                    }, 1000);
                } else {
                    upload2DB();
                    uploadGPS();
                    if (!this.ProfileUpload) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i("TestService", "Start sending UserProfile");
                                uploadUserProfile();
                            }
                        });
                        t.start();
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "phyphox:SubWakeLock");
        wakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    public void upload2DB() {
        if (!dataArray.isNull(0)) {
            Log.i("TestUpload", "continue last upload");
            writeEntry(dataArray.toString());
            return;
        }
        Cursor result = db.getInfo("Sensingdata");
        if(result != null) {
            result.moveToFirst();
            if (result.getCount() > 10000)
                count = 10000;
            else
                count = result.getCount();
            for(int i = 0 ; i < count ; i++) {
                final String UID = Build.SERIAL;//ExperimentList.serial;
                final String RowId = Integer.toString(result.getInt(0));
                final String dataX = Double.toString(result.getDouble(1));
                final String dataY = Double.toString(result.getDouble(2));
                final String dataZ = Double.toString(result.getDouble(3));
                final String timestamp = Long.toString(result.getLong(4));
                final String dataAbs = Double.toString(result.getDouble(5));
                final String dataAccuracy = Double.toString(result.getDouble(6));
                final String SensorName = result.getString(7);
                String msg = UID+" "+RowId+" " + dataX+" "+dataY+" "+dataZ+" "+timestamp+" "
                        +dataAbs+" "+ dataAccuracy + " " + SensorName;
                Log.i("TestUpload", msg);
                JSONObject data = new JSONObject();
                try {
                    data.put("UID", UID);
                    data.put("Sensor", SensorName);
                    data.put("dataX", dataX);
                    data.put("dataY", dataY);
                    data.put("dataZ", dataZ);
                    data.put("timestamp", timestamp);
                    data.put("dataAbs", dataAbs);
                    data.put("dataAccuracy", dataAccuracy);
                } catch (JSONException e) { }
                dataArray.put(data);
                timestamps.add(timestamp);
                result.moveToNext();
            }
            String dataToSend = dataArray.toString();
            Log.i("TestUpload1", "Start Sending " + dataArray.length() + " records");
            writeEntry(dataToSend);
        }
    }

    public void writeEntry(String message) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        boolean retry = true;
        try {
            URL url = new URL("http://140.114.89.66/smart_insole_sensor.php"); // 10.0.2.2 for emulator
            //URL url = new URL("http://192.168.137.1/smart_insole.php"); // for local test
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(message.getBytes().length);

            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.connect();

            output = new BufferedOutputStream(connection.getOutputStream());
            output.write(message.getBytes());
            output.flush();
            Log.i("TestUpload", "Upload!");
            input = connection.getInputStream();
            //count++;
            dataArray = new JSONArray(); // clear
        }catch(ClientProtocolException e) {
            Log.e("Log_tag", "ClientProtocol");
            e.printStackTrace();
        }catch(IOException e) {
            Log.e("Log_tag", "IOException");
            e.printStackTrace();
        } finally {
            /*try {
                output.close();
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            connection.disconnect();
            SQLiteDatabase db = this.db.getWritableDatabase();
            String args = TextUtils.join(",", timestamps);
            Log.i("TestUpload1", "BEFORE Sensor Table Count= " + Integer.toString(this.db.getCount("Sensingdata")));
            db.execSQL("delete from Sensingdata where DataT IN (" + args + ")");
            //db.delete("Sensingdata", "DataT IN (" + args+ ")", null);
            Log.i("TestUpload1", "AFTER Sensor Table Count= " + Integer.toString(this.db.getCount("Sensingdata")));
            timestamps.clear();
        }
    }

    public void uploadUserProfile() {
        SQLiteDatabase db = this.db.getWritableDatabase();
        Cursor result = db.query("User", new String[] {"Name", "Age", "Gender", "Height", "Weight", "Nationality", "Job"},
                null, null, null, null, null);
        Log.i("Upload_UserInfo","access success");
        while (result.moveToNext()) {
            final String UID = Build.SERIAL;//ExperimentList.serial;
            final String Name = result.getString(0);
            final String Age = result.getString(1);
            final String Gender = result.getString(2);
            final String Height = result.getString(3);
            final String Weight = result.getString(4);
            final String Nationality = result.getString(5);
            final String Job = result.getString(6);

            InputStream input = null;
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("UID", UID));
            nameValuePairs.add(new BasicNameValuePair("Sensor", "User"));
            nameValuePairs.add(new BasicNameValuePair("Name", Name));
            nameValuePairs.add(new BasicNameValuePair("Age", Age));
            nameValuePairs.add(new BasicNameValuePair("Gender", Gender));
            nameValuePairs.add(new BasicNameValuePair("Height", Height));
            nameValuePairs.add(new BasicNameValuePair("Weight", Weight));
            nameValuePairs.add(new BasicNameValuePair("Nationality", Nationality));
            nameValuePairs.add(new BasicNameValuePair("Job", Job));

            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://140.114.89.66/smart_insole.php"); // 10.0.2.2 for emulator, 192.168.137.1 for LAN

                // Pass the nameValuePairs to the httpPost
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpClient.execute(httpPost);
                String message = UID + " " + Name + " " + Age;
                Log.i("Upload_UserInfo", "Send User Profile to DB:" + message);
                HttpEntity entity = response.getEntity();
                input = entity.getContent();
                this.ProfileUpload = true;
                //SharedPreferences.Editor editor = preferences.edit();
                //editor.putBoolean("isFirstUse", false);
                //editor.commit();

            }catch(ClientProtocolException e) {
                Log.e("Log_tag", "ClientProtocol");
                e.printStackTrace();
            }catch(IOException e) {
                Log.e("Log_tag", "IOException");
                e.printStackTrace();
            }
        }
    }

    public void uploadGPS() {
        //int totalCount = ExperimentList.db.getCount("location");
        Cursor result = this.db.getInfo("location");
        //Log.i("TestService", "GPS CurrentCount = "+Long.toString(GpsCount));
        //Log.i("TestService", "GPS TotalCount = "+Integer.toString(totalCount));
        //if (totalCount == 0)
        //    return;
        //long i = GpsCount;
        //Cursor result = ExperimentList.db.getEntry("location", Long.toString(i));
        if (result != null) {
            result.moveToNext();
            for (int i = 0; i < result.getCount(); i++) {
                final String UID = Build.SERIAL;//ExperimentList.serial;
                final String RowId = Long.toString(result.getLong(0));
                final String dataLat = Double.toString(result.getDouble(1));
                final String dataLon = Double.toString(result.getDouble(2));
                final String dataZ = Double.toString(result.getDouble(3));
                final String velocity = Double.toString(result.getDouble(4));
                final String timestamp = Long.toString(result.getLong(5));
                final String dataAccuracy = Double.toString(result.getDouble(6));
                final String dataZAccuracy = Double.toString(result.getDouble(7));
                final String dataSatellites = Double.toString(result.getDouble(8));
                Log.i("GPS", "Ready to upload");
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        writeGpsEntry(UID, dataLat, dataLon, dataZ, velocity, timestamp, dataAccuracy,
                                dataZAccuracy, dataSatellites, RowId);
                    }
                });
                t.start();
            }
        }
    }

    public void writeGpsEntry(String UID, String dataLat, String dataLon, String dataZ, String velocity,
                              String timestamp, String dataAccuracy, String dataZAccuracy, String dataSatellites,
                              String rowId)
    {
        InputStream input = null;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("UID", UID));
        nameValuePairs.add(new BasicNameValuePair("Sensor", "location"));
        nameValuePairs.add(new BasicNameValuePair("dataLat", dataLat));
        nameValuePairs.add(new BasicNameValuePair("dataLon", dataLon));
        nameValuePairs.add(new BasicNameValuePair("dataZ", dataZ));
        nameValuePairs.add(new BasicNameValuePair("dataV", velocity));
        nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));
        nameValuePairs.add(new BasicNameValuePair("dataAccuracy", dataAccuracy));
        nameValuePairs.add(new BasicNameValuePair("dataZAccuracy", dataZAccuracy));
        nameValuePairs.add(new BasicNameValuePair("dataSatellites", dataSatellites));

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://140.114.89.66/smart_insole.php"); // 10.0.2.2 for emulator
            //HttpPost httpPost = new HttpPost("http://192.168.137.1/hscc.php"); // for local test
            // Pass the nameValuePairs to the httpPost
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpClient.execute(httpPost);
            Log.i("GPS", "Send to DB");
            HttpEntity entity = response.getEntity();
            input = entity.getContent();
            count++;

        }catch(ClientProtocolException e) {
            Log.e("Log_tag", "ClientProtocol");
            e.printStackTrace();
        }catch(IOException e) {
            Log.e("Log_tag", "IOException");
            e.printStackTrace();
        } finally {
            BackgroundTask backgroundTask = new BackgroundTask();
            backgroundTask.execute("Remove GPS", rowId);
        }
    }
}
