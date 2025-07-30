package com.auctorlabs.cpusimulator.cpumodules;

public class Rom {
    private final int[] data;

    public Rom(int size) {
        this.data = new int[size];
    }

    public int readFromAddress(int address) {
        return data[address];
    }

    public void writeToAddress(int address, int value) {
        this.data[address] = value;
    }
}
