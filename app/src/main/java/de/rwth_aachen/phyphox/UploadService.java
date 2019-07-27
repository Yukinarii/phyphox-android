package de.rwth_aachen.phyphox;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

// for Google Sheets API
// for example code
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UploadService extends Service {
    public Runnable mRunnable = null;
    public static int count = 0; // shared
    public static long GpsCount = 0;
    private SharedPreferences preferences;
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Handler mHandler = new Handler();
        Log.i("TestService", "Service Start!");
        mRunnable = new Runnable() {
            @Override
            public void run() {
                // DO SOMETHING HERE
                ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connManager.getActiveNetworkInfo();
                if (info == null || !info.isConnected()){

                }
                else {
                    if (!info.isAvailable()) {

                    } else {
                        upload2DB();
                        uploadGPS();
                        if (ExperimentList.ProfileUpload == false) {
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    uploadUserProfile();
                                }
                            });
                            t.start();
                        }
                        Log.i("TestService", "Upload Completed");
                    }
                }
                mHandler.postDelayed(mRunnable, 1000);
            }
        };
        mHandler.postDelayed(mRunnable, 1000);
        return super.onStartCommand(intent, flags, startId);
    }

    public void upload2DB() {
            int totalCount = ExperimentList.db.getCount("Sensingdata");
            Log.i("TestService", "CurrentCount = "+Integer.toString(count));
            Log.i("TestService", "TotalCount = "+Integer.toString(totalCount));
            if (totalCount == 0)
                return;
            int i = count;
            Cursor result = ExperimentList.db.getEntry("Sensingdata", Integer.toString(i));
            while (result.moveToNext()) {
                final String UID = ExperimentList.serial;
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
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        writeEntry(UID, dataX, dataY, dataZ,timestamp, dataAbs, dataAccuracy, SensorName, RowId);
                    }
                });
                t.start();
            }
    }
    public int LogintoDB() throws IOException {
        int return_result = 0;
        String user_id = "";
        String pwd = "";
        String params = "uid="+user_id+'&'+"pwd="+pwd;
        String db_url = "http://192.168.137.1:80/login.php"; // 10.0.2.2 for emulator, 192.168.137.1 for LAN
        try {
            URL url = new URL(db_url);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setDoOutput(true);
            http.setRequestMethod("POST");
            OutputStream out = http.getOutputStream();
            out.write(params.getBytes());
            Log.i("Testservice", "Connect to Server!");
            out.flush();
            out.close();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((http.getInputStream())));
            String line = "";
            StringBuilder builder = new StringBuilder();

            while (null!=(line=bufferedReader.readLine())) {
                builder.append(line);
            }
            String result = builder.toString();
            JSONObject object = new JSONObject(result);
            return_result = object.getInt("status");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return return_result;
    }

    public void writeEntry(String UID, String dataX, String dataY, String dataZ, String timestamp, String dataAbs, String dataAccuracy, String table_name, String rowId) {
        InputStream input = null;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("UID", UID));
        nameValuePairs.add(new BasicNameValuePair("Sensor", table_name));
        nameValuePairs.add(new BasicNameValuePair("dataX", dataX));
        nameValuePairs.add(new BasicNameValuePair("dataY", dataY));
        nameValuePairs.add(new BasicNameValuePair("dataZ", dataZ));
        nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));
        nameValuePairs.add(new BasicNameValuePair("dataAbs", dataAbs));
        nameValuePairs.add(new BasicNameValuePair("dataAccuracy", dataAccuracy));

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://140.114.89.66/smart_insole.php"); // 10.0.2.2 for emulator
            //HttpPost httpPost = new HttpPost("http://192.168.137.1/hscc.php"); // for local test
            // Pass the nameValuePairs to the httpPost
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpClient.execute(httpPost);
            Log.i("TestService", "Send to DB");
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
            backgroundTask.execute("Remove", rowId);
        }
    }

    public void uploadUserProfile() {
        SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
        Cursor result = db.query("User", new String[] {"Name", "Age", "Gender", "Height", "Weight", "Nationality", "Job"},
                null, null, null, null, null);
        Log.i("Upload_UserInfo","access success");
        while (result.moveToNext()) {
            final String UID = ExperimentList.serial;
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
                Log.i("TestService", "Send User Profile to DB");
                HttpEntity entity = response.getEntity();
                input = entity.getContent();
                ExperimentList.ProfileUpload = true;

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
        int totalCount = ExperimentList.db.getCount("location");
        Log.i("TestService", "GPS CurrentCount = "+Long.toString(GpsCount));
        Log.i("TestService", "GPS TotalCount = "+Integer.toString(totalCount));
        if (totalCount == 0)
            return;
        long i = GpsCount;
        Cursor result = ExperimentList.db.getEntry("location", Long.toString(i));
        while (result.moveToNext()) {
            final String UID = ExperimentList.serial;
            final String RowId = Long.toString(result.getLong(0));
            final String dataLat = Double.toString(result.getDouble(1));
            final String dataLon = Double.toString(result.getDouble(2));
            final String dataZ = Double.toString(result.getDouble(3));
            final String velocity = Double.toString(result.getDouble(4));
            final String timestamp = Long.toString(result.getLong(5));
            final String dataAccuracy = Double.toString(result.getDouble(6));
            final String dataZAccuracy = Double.toString(result.getDouble(7));
            final String dataSatellites = Double.toString(result.getDouble(8));

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
            Log.i("TestService", "Send to DB");
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

/*
This two should be in the MainActivity
MyDBHelper myDBHelper = new MyDBHelper(getApplicationContext());
startService(new Intent(this, MyService.class));
*/