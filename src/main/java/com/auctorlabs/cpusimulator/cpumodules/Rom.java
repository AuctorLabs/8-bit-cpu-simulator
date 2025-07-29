package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.LogicalState;

public class Rom {
    private final int[] data;

    private final int size;

    public Rom(int size) {
        this.data = new int[size];
        this.size = size;
    }

    public int readFromAddress(int address) {
        return data[address];
    }

    public void writeToAddress(int address, int value) {
        this.data[address] = value;
    }
}
