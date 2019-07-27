package de.rwth_aachen.phyphox;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.Console;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.locks.Lock;

//The sensorInput class encapsulates a sensor, maps their name from the phyphox-file format to
//  the android identifiers and handles their output, which is written to the dataBuffers
public class sensorInput implements SensorEventListener, Serializable {
    public int type; //Sensor type (Android identifier)
    public boolean calibrated = true;
    public long period; //Sensor aquisition period in nanoseconds (inverse rate), 0 corresponds to as fast as possible
    public long t0 = 0; //the start time of the measurement. This allows for timestamps relative to the beginning of a measurement
    public dataBuffer dataX; //Data-buffer for x
    public dataBuffer dataY; //Data-buffer for y (3D sensors only)
    public dataBuffer dataZ; //Data-buffer for z (3D sensors only)
    public dataBuffer dataT; //Data-buffer for t
    public dataBuffer dataAbs; //Data-buffer for absolute value
    public dataBuffer dataAccuracy; //Data-buffer for absolute value
    transient private SensorManager sensorManager; //Hold the sensor manager

    private long lastReading; //Remember the time of the last reading to fullfill the rate
    private double avgX, avgY, avgZ, avgAccuracy; //Used for averaging
    private boolean average = false; //Avergae over aquisition period?
    private int aquisitions; //Number of aquisitions for this average
    public final String table_name;
    public LinkedList<SensorData> data = new LinkedList(); // sensorData is a tuple
    //private int rowId = 0;
    private Lock dataLock;

    public class SensorException extends Exception {
        public SensorException(String message) {
            super(message);
        }
    }

    public static int resolveSensorString(String type) {
        //Interpret the type string
        switch (type) {
            case "linear_acceleration": return Sensor.TYPE_LINEAR_ACCELERATION;
            case "light": return Sensor.TYPE_LIGHT;
            case "gyroscope": return Sensor.TYPE_GYROSCOPE;
            case "accelerometer": return Sensor.TYPE_ACCELEROMETER;
            case "magnetic_field": return Sensor.TYPE_MAGNETIC_FIELD;
            case "pressure": return Sensor.TYPE_PRESSURE;
            case "temperature": return Sensor.TYPE_AMBIENT_TEMPERATURE;
            case "humidity": return Sensor.TYPE_RELATIVE_HUMIDITY;
            case "proximity": return Sensor.TYPE_PROXIMITY;
            default: return -1;
        }
    }

    //The constructor needs the phyphox identifier of the sensor type, the desired aquisition rate,
    // and the four buffers to receive x, y, z and t. The data buffers may be null to be left unused.
    protected sensorInput(String type, double rate, boolean average, Vector<dataOutput> buffers, Lock lock) throws SensorException {
        this.dataLock = lock;

        if (rate <= 0)
            this.period = 0;
        else
            this.period = (long)((1/rate)*1e9); //Period in ns

        this.average = average;

        //ExperimentList.db.createTable(type);
        //ExperimentList.table_list.add(type);
        //ExperimentList.table_rowId.put(type, new Integer(0));
        this.table_name = type;

        this.type = resolveSensorString(type);
        if (this.type < 0)
            throw  new SensorException("Unknown sensor.");

        //Store the buffer references if any
        if (buffers == null)
            return;

        buffers.setSize(6);
        if (buffers.get(0) != null)
            this.dataX = buffers.get(0).buffer;
        if (buffers.get(1) != null)
            this.dataY = buffers.get(1).buffer;
        if (buffers.get(2) != null)
            this.dataZ = buffers.get(2).buffer;
        if (buffers.get(3) != null)
            this.dataT = buffers.get(3).buffer;
        if (buffers.get(4) != null)
            this.dataAbs = buffers.get(4).buffer;
        if (buffers.get(5) != null)
            this.dataAccuracy = buffers.get(5).buffer;
    }

    public void attachSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    //Check if the sensor is available without trying to use it.
    public boolean isAvailable() {
        return (sensorManager.getDefaultSensor(type) != null);
    }

    //Get the internationalization string for a sensor type
    public static int getDescriptionRes(int type) {
        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return R.string.sensorLinearAcceleration;
            case Sensor.TYPE_LIGHT:
                return R.string.sensorLight;
            case Sensor.TYPE_GYROSCOPE:
                return R.string.sensorGyroscope;
            case Sensor.TYPE_ACCELEROMETER:
                return R.string.sensorAccelerometer;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return R.string.sensorMagneticField;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return R.string.sensorMagneticField;
            case Sensor.TYPE_PRESSURE:
                return R.string.sensorPressure;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return R.string.sensorTemperature;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return R.string.sensorHumidity;
            case Sensor.TYPE_PROXIMITY:
                return R.string.sensorProximity;
        }
        return R.string.unknown;
    }

    public int getDescriptionRes() {
        return sensorInput.getDescriptionRes(type);
    }

    //Start the data aquisition by registering a listener for this sensor.
    public void start() {
        this.t0 = 0; //Reset t0. This will be set by the first sensor event

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && (type == Sensor.TYPE_MAGNETIC_FIELD || type == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)) {
            if (calibrated)
                this.type = Sensor.TYPE_MAGNETIC_FIELD;
            else
                this.type = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
        }

        //Reset averaging
        this.lastReading = 0;
        this.avgX = 0.;
        this.avgY = 0.;
        this.avgZ = 0.;
        this.avgAccuracy = 0.;
        this.aquisitions = 0;

        this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(type), SensorManager.SENSOR_DELAY_FASTEST);
    }

    //Stop the data aquisition by unregistering the listener for this sensor
    public void stop() {
        this.sensorManager.unregisterListener(this);
    }

    //This event listener is mandatory as this class implements SensorEventListener
    //But phyphox does not need it
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //This is called when we receive new data from a sensor. Append it to the right buffer
    public void onSensorChanged(SensorEvent event) {
        if (t0 == 0) {
            t0 = event.timestamp; //Any event sets t0
            if (dataT != null && dataT.getFilledSize() > 0)
                t0 -= dataT.value * 1e9;
        }

        //From here only listen to "this" sensor
        if (event.sensor.getType() == type) {

            Double accuracy = Double.NaN;
            if (type == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
                accuracy = 0.0;
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                switch (event.accuracy) {
                    case SensorManager.SENSOR_STATUS_NO_CONTACT:
                    case SensorManager.SENSOR_STATUS_UNRELIABLE:
                        accuracy = -1.0;
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                        accuracy = 1.0;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                        accuracy = 2.0;
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                        accuracy = 3.0;
                }
            }

            if (average) {
                //We want averages, so sum up all the data and count the aquisitions
                avgX += event.values[0];
                if (event.values.length > 1) {
                    avgY += event.values[1];
                    avgZ += event.values[2];
                }

                avgAccuracy = Math.min(accuracy, avgAccuracy);
                aquisitions++;
            } else {
                //No averaging. Just keep the last result
                avgX = event.values[0];
                if (event.values.length > 1) {
                    avgY = event.values[1];
                    avgZ = event.values[2];
                }
                avgAccuracy = accuracy;
                aquisitions = 1;
            }
            if (lastReading == 0)
                lastReading = event.timestamp;
            if (lastReading + period <= event.timestamp) {
                //Average/waiting period is over
                //Append the data to available buffers
                dataLock.lock();
                try {
                    /*if (dataX != null)
                        dataX.append(avgX / aquisitions);
                    if (dataY != null)
                        dataY.append(avgY / aquisitions);
                    if (dataZ != null)
                        dataZ.append(avgZ / aquisitions);
                    if (dataT != null)
                        dataT.append((event.timestamp - t0) * 1e-9); //We want seconds since t0
                    if (dataAbs != null)
                        dataAbs.append(Math.sqrt(avgX*avgX+avgY*avgY+avgZ*avgZ) / aquisitions);
                    if (dataAccuracy != null)
                        dataAccuracy.append(accuracy);
                        // Write to SQLite
                        Log.i("sensordata", "writing to SQLite");
                        /*BackgroundTask backgroundTask = new BackgroundTask();
                        //rowId = ExperimentList.table_rowId.get(type);
                        backgroundTask.execute("Insert",Integer.toString(ExperimentList.rowId), Double.toString(avgX),
                                Double.toString(avgY), Double.toString(avgZ), Long.toString(event.timestamp),
                                Double.toString(Math.sqrt(avgX*avgX+avgY*avgY+avgZ*avgZ) / aquisitions), Double.toString(accuracy), table_name); // table_name = sensorName
                                */
                        long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
                        SensorData<Double, Double, Double, Long, Double, Double> entry = new SensorData(avgX, avgY, avgZ, timeInMillis, Math.sqrt(avgX*avgX+avgY*avgY+avgZ*avgZ) / aquisitions, accuracy);
                        data.add(entry);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        Calendar cal = Calendar.getInstance(Locale.TAIWAN);
                        cal.setTimeInMillis(event.timestamp * 1000L);
                        Log.i("Timestamp", sdf.format(new Date(timeInMillis)));

                        String sql = "INSERT INTO Sensingdata (rowId, dataX, dataY, dataZ, dataT, dataAbs, dataAccuracy, SensorName) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        if (data.size() % 100 == 0) {
                            int idx = 0;
                            // write into database by transaction
                            SQLiteDatabase db = ExperimentList.db.getWritableDatabase();
                            db.beginTransactionNonExclusive();
                            //SQLiteStatement statement = db.compileStatement(sql);
                            for (SensorData info: data) {
                                /*statement.bindString(1, String.valueOf(ExperimentList.rowId));
                                statement.bindString(2, String.valueOf(info.dataX));
                                statement.bindString(3, String.valueOf(info.dataY));
                                statement.bindString(4, String.valueOf(info.dataZ));
                                statement.bindString(5, String.valueOf(info.timestamp));
                                statement.bindString(6, String.valueOf(info.dataAbs));
                                statement.bindString(7, String.valueOf(info.dataAccuracy));
                                statement.bindString(8, this.table_name);
                                statement.execute();
                                statement.clearBindings();*/
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("rowId", ExperimentList.rowId);
                                contentValues.put("dataX", String.valueOf(info.dataX));
                                contentValues.put("dataY", String.valueOf(info.dataY));
                                contentValues.put("dataZ", String.valueOf(info.dataZ));
                                contentValues.put("dataT", String.valueOf(info.timestamp));
                                contentValues.put("dataAbs", String.valueOf(info.dataAbs));
                                contentValues.put("dataAccuracy", String.valueOf(info.dataAccuracy));
                                contentValues.put("SensorName", this.table_name);
                                db.insert("Sensingdata", null, contentValues);
                                Log.i("sensordata", "writing to SQLite End, index= " + Integer.toString(ExperimentList.rowId));
                                if (ExperimentList.rowId == Integer.MAX_VALUE)
                                    ExperimentList.rowId = 0;
                                else
                                    ExperimentList.rowId ++;
                            }
                            db.setTransactionSuccessful();
                            db.endTransaction();
                            data.clear();
                        }
                        /*if (ExperimentList.rowId == Integer.MAX_VALUE)
                            ExperimentList.rowId = 0;
                        else
                            ExperimentList.rowId ++;*/
                    // Try to add code here!!!
                } catch (SQLiteDatabaseLockedException e) {
                    Log.i("DB", "DB is Locked");
                } catch (SQLiteException e) {}
                finally {
                    dataLock.unlock();
                }
                //Reset averaging
                avgX = 0.;
                avgY = 0.;
                avgZ = 0.;
                avgAccuracy = 0.;
                lastReading = event.timestamp;
                aquisitions = 0;
            }
        }
    }
}