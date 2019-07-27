package de.rwth_aachen.phyphox;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class BackgroundTask extends AsyncTask<String, Void, Void> {
    BackgroundTask() { }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            String method = params[0];
            if (method.equals("Insert")) {
                int rowId = Integer.parseInt(params[1]);
                double dataX = Double.parseDouble(params[2]);
                double dataY = Double.parseDouble(params[3]);
                double dataZ = Double.parseDouble(params[4]);
                long timestamp = Long.parseLong(params[5]);
                double dataAbs = Double.parseDouble(params[6]);
                double dataAccuracy = Double.parseDouble(params[7]);
                String SensorName = params[8];
                SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
                ContentValues contentvalue = new ContentValues();
                contentvalue.put("rowId", rowId);
                contentvalue.put("DataX", dataX);
                contentvalue.put("DataY", dataY);
                contentvalue.put("DataZ", dataZ);
                contentvalue.put("DataT", timestamp);
                contentvalue.put("DataAbs", dataAbs);
                contentvalue.put("DataAccuracy", dataAccuracy);
                contentvalue.put("SensorName", SensorName);

                Log.i("SensorData",params[0]+" "+params[1]+" "+params[2]+" "+params[3]+" "+params[4]+" "+params[5]+" "+params[6]+" "+params[7]);
                db.insert("Sensingdata", null, contentvalue);
                //ExperimentList.db.insertInfo(rowId, dataX, dataY, dataZ, timestamp, dataAbs, table_name);
            }
            else if (method.equals("Remove")) {
                SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
                JSONArray dataArray = new JSONArray(params[1]);
                String sql = "Delete from SensingData where dataX=? and dataY=? and dataT=?";
                db.beginTransactionNonExclusive();
                SQLiteStatement statement = db.compileStatement(sql);
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);
                    /*db.delete("Sensingdata",
                             "dataX=? and dataY=? and dataZ=? and dataT=?",
                              new String[]{object.get("dataX").toString(), object.get("dataY").toString(),
                                      object.get("dataZ").toString(), object.get("timestamp").toString()});
                    */
                    statement.bindString(1, object.get("dataX").toString());
                    statement.bindString(2, object.get("dataY").toString());
                    statement.bindString(3, object.get("timestamp").toString());
                    statement.execute();
                    statement.clearBindings();
                    String msg = "Delete uploaded msg timestamp ="+ object.get("timestamp").toString();
                    Log.i("BackgroundTask", msg);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }
            else if (method.equals("Insert GPS")) {
                long rowId = Long.parseLong(params[1]);
                double dataLat = Double.parseDouble(params[2]);
                double dataLon = Double.parseDouble(params[3]);
                double dataZ   = Double.parseDouble(params[4]);
                double dataV   = Double.parseDouble(params[5]);
                long timestamp = Long.parseLong(params[6]);
                double dataAccuracy = Double.parseDouble(params[7]);
                double dataZAccuracy = Double.parseDouble(params[8]);
                double dataSatellites = Double.parseDouble(params[9]);
                SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
                ContentValues contentvalue = new ContentValues();
                contentvalue.put("rowId", rowId);
                contentvalue.put("DataLat", dataLat);
                contentvalue.put("DataLon", dataLon);
                contentvalue.put("DataZ", dataZ);
                contentvalue.put("DataV", dataV);
                contentvalue.put("timestamp", timestamp);
                contentvalue.put("DataAccuracy", dataAccuracy);
                contentvalue.put("DataZAccuracy", dataZAccuracy);
                contentvalue.put("DataSatellites", dataSatellites);

                db.insert("location", null, contentvalue);
                Log.i("GPS", "Insert");
            }
            else if (method.equals("Remove GPS")) {
                SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
                String rowId = params[1];
                int n = db.delete("location", "rowId=?", new String[]{rowId});
                String msg = "Delete uploaded msg rowId ="+rowId + " " + Integer.toString(n);
                Log.i("BackgroundTask", msg);
            }
        } catch (Exception e) {
            Log.e("AsyncTask", e.getMessage());
        }
        return null;
    }

    @Override
    protected  void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected  void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
