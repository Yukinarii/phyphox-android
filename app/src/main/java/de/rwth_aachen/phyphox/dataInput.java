package de.rwth_aachen.phyphox;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//dataInput wraps all data-containers (currently only dataBuffer) and constant values as possible
// inputs. This allows analysis modules to access constant values as if they were buffers.

public class dataInput implements Serializable {
    boolean isBuffer = false;
    boolean isEmpty = false;
    double value = Double.NaN;
    dataBuffer buffer = null;
    boolean clearAfterRead = true;

    //Get value
    public double getValue() {
        if (isBuffer)
            return buffer.value;
        else
            return value;
    }

    //Constructor if this should contain a buffer
    protected dataInput(dataBuffer buffer, boolean clear) {
        this.clearAfterRead = clear;
        isBuffer = true;
        this.isEmpty = false;
        this.buffer = buffer;
    }

    //Constructor if this should contain a constant value
    protected dataInput(double value) {
        isBuffer = false;
        this.isEmpty = false;
        this.value = value;
    }

    protected dataInput() {
        this.isBuffer = false;
        this.isEmpty = true;
    }

    //Get the number of elements actually filled into the buffer
    public int getFilledSize() {
        if (isBuffer)
            return buffer.getFilledSize();
        else if (isEmpty)
            return 0;
        else
            return 1;
    }

    //Retrieve the iterator of the BlockingQueue
    public Iterator<Double> getIterator() {
        if (isBuffer)
            return buffer.getIterator();
        else
            return null;
    }

    //Get all values as a double array
    public Double[] getArray() {
        if (isBuffer) {
            return buffer.getArray();
        } else if (isEmpty) {
            return new Double[0];
        } else {
            Double ret[] = new Double[1];
            ret[0] = value;
            return ret;
        }
    }

    //Get all values as a short array. The data will be scaled so that (-/+)1 matches (-/+)Short.MAX_VALUE, used for audio data
    public short[] getShortArray() {
        if (isBuffer) {
            return buffer.getShortArray();
        } else if (isEmpty) {
            return new short[0];
        } else {
            short ret[] = new short[1];
            ret[0] = (short) (value * (Short.MAX_VALUE)); //Rescale data to short range;
            return ret;
        }
    }

    public void clear(boolean reset) {
        if (isBuffer)
            buffer.clear(reset);
    }

    protected dataInput copy() {
        if (this.isBuffer) {
            return new dataInput(this.buffer.copy(), this.clearAfterRead);
        } else if (isEmpty) {
            return new dataInput();
        } else {
            return new dataInput(this.value);
        }
    }
}