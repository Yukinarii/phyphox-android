package de.rwth_aachen.phyphox;

public class SensorData<A, B, C, D, E, F> {
    public final A dataX;
    public final B dataY;
    public final C dataZ;
    public final D timestamp;
    public final E dataAbs;
    public final F dataAccuracy;

    public SensorData(A a, B b, C c, D d, E e, F f) {
        dataX = a;
        dataY = b;
        dataZ = c;
        timestamp = d;
        dataAbs = e;
        dataAccuracy = f;
    }
}
