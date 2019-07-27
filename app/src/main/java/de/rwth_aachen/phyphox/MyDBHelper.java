package de.rwth_aachen.phyphox;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

// The class provides features for writing data into DB,
public class MyDBHelper extends SQLiteOpenHelper {
    static int count = 0; // # of entries that have been updated
    public MyDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, getMyDatabaseName(name), factory, version);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS GYROSCOPE (DataX DOUBLE,"
        //       + " DataY DOUBLE, DataZ DOUBLE, DataT LONG, DataAbs DOUBLE);");
    }

    // from https://blog.csdn.net/u013766436/article/details/51044566
    private static String getMyDatabaseName(String name){
        String databasename = name;
        String sdcardPath = "";
        File fileList[] = new File("/storage/").listFiles();
        for (File file: fileList) {
            if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
                sdcardPath = file.getAbsolutePath();
        }
        if (sdcardPath != "")
            Log.i("DBTest", sdcardPath);
        else
            Log.i("DBTest", "no sd card");
        boolean isSdcardEnable = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if ( Environment.isExternalStorageRemovable(Environment.getExternalStorageDirectory())) {
                isSdcardEnable = true;
            }
        }
        String dbPath = "";
        Log.i("DBTest", Boolean.toString(isSdcardEnable));
        if(isSdcardEnable){
            dbPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hscc/database/";
            databasename = dbPath + databasename;
            Log.i("DBTest", "build db in sd-card");
        }else{//未插入SDCard，建在内存中
            Log.i("DBTest", "build db in local ROM");
        }
        File dbp = new File(dbPath);
        if(!dbp.exists()){
            dbp.mkdirs();
        }
        return databasename;
    }


    public void insertInfo(int rowId, double dataX, double dataY, double dataZ, long dataT, double dataAbs, String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentvalue = new ContentValues();
        contentvalue.put("rowId", rowId);
        contentvalue.put("DataX", dataX);
        contentvalue.put("DataY", dataY);
        contentvalue.put("DataZ", dataZ);
        contentvalue.put("DataT", dataT);
        contentvalue.put("DataAbs", dataAbs);

        db.insert(table_name, null, contentvalue);
        //db.close();
    }

    public void createTable(String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + table_name + " (rowId LONG, DataX DOUBLE,"
                + " DataY DOUBLE, DataZ DOUBLE, DataT LONG, DataAbs DOUBLE, DataAccuracy DOUBLE, SensorName TEXT);");
    }

    public Cursor getEntry(String table_name, String idx) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = null;
        if (table_name == "Sensingdata") {
            result = db.query(table_name, new String[]{"rowId", "DataX", "DataY", "DataZ", "DataT", "DataAbs", "DataAccuracy", "SensorName"},
                    "rowId" + "=?", new String[]{idx}, null, null, null, null);
        }
        else if (table_name == "location") {
            result = db.query(table_name, new String[]{"rowId", "dataLat", "dataLon", "dataZ", "dataV", "timestamp", "dataZAccuracy", "dataZAccuracy", "dataSatellites"},
                    "rowId" + "=?", new String[]{idx}, null, null, null, null);
        }

        return result;
    }

    public int getCount(String table_name) {
        String countQuery = "SELECT  * FROM " + table_name;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int TotalCount = cursor.getCount();
        cursor.close();
        return TotalCount;
    }

    public Cursor getCursor(String table_name) {
        String countQuery = "SELECT  * FROM " + table_name;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        return cursor;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertUserInfo(String name, String age, String gender, String height, String weight, String nationality, String job) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentvalue = new ContentValues();
        contentvalue.put("Name", name);
        contentvalue.put("Age", age);
        contentvalue.put("Gender", gender);
        contentvalue.put("Height", height);
        contentvalue.put("Weight", weight);
        contentvalue.put("Nationality", nationality);
        contentvalue.put("Job", job);

        String msg = name+" "+" "+age+" "+gender+" "+height+" "+weight+" "+nationality+" "+job;
        Log.i("UserInfo_DB", msg);
        db.insert("User", null, contentvalue);
        Log.i("UserInfo_DB", "completed");
    }

    public void createUserTable(String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + table_name + " (Name TEXT, Age TEXT,"
                + " Gender TEXT, Height TEXT, Weight TEXT, Nationality TEXT, Job TEXT);");
    }

    public void createGpsTable(String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + table_name + " (rowId LONG, DataLat DOUBLE,"
                + " DataLon DOUBLE, DataZ DOUBLE, DataV, timestamp LONG, DataAccuracy DOUBLE, DataZAccuracy DOUBLE,"
                + " DataSatellites DOUBLE);");
    }

    public int getMinRowId(String table_name) {
        int min_rowId = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT min(rowId) FROM " + table_name;
        Cursor result = db.rawQuery(query, null);

        while(result.moveToNext()) {
            min_rowId = result.getInt(0);
        }
        return min_rowId;
    }

    // get all data
    public Cursor getInfo(String table_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = null;
        if (table_name == "Sensingdata") {
            result = db.query(table_name, new String[]{"rowId", "DataX", "DataY", "DataZ", "DataT", "DataAbs", "DataAccuracy", "SensorName"},
                    null, null, null, null, null);
        }
        else if (table_name == "location") {
            result = db.query(table_name, new String[]{"rowId", "dataLat", "dataLon", "dataZ", "dataV", "timestamp", "dataAccuracy", "dataZAccuracy", "dataSatellites"},
                    null, null, null, null, null, null);
        }

        return result;
    }

    public boolean IsTableExist(String tableName){
        boolean result = false;
        SQLiteDatabase db = this.getReadableDatabase();
        if(tableName == null){
            return false;
        }
        Cursor cursor = null;
        try {
            String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='"+tableName+"' ";
            cursor = db.rawQuery(sql, null);
            if(cursor.moveToNext()){
                int count = cursor.getInt(0);
                if(count>0){
                    result = true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }
}
