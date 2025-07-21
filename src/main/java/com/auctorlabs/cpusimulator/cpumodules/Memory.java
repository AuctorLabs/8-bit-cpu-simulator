package com.auctorlabs.cpusimulator.cpumodules;

import java.util.Arrays;

public class Memory {
    private int[] data;
    private final int size;

    public Memory(int size) {
        this.data = new int[size];
        this.size = size;
    }

    public int readFromAddress(int address) {
        return data[address];
    }

    public void writeToAddress(int address, int value) {
        this.data[address] = value;
    }

    public void writeAll(int[] data) {
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public void fillWithZeros() {
        Arrays.fill(this.data, 0);
    }

    public int getSize() {
        return size;
    }

    public int getLastNonZeroAddress() {
        for (int i = this.data.length - 1; i >= 0; i--) {
            if (this.data[i] != 0) {
                return i;
            }
        }
        return -1;
    }
}
