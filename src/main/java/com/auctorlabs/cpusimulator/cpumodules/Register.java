package com.auctorlabs.cpusimulator.cpumodules;

public class Register {
    private int value = 0;

    public void load(int value) {
        this.value = value;
    }

    public void load(Register register) {
        this.value = register.read();
    }

    public int read() {
        return value;
    }
}
