package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;

import java.util.Arrays;

public class Ram extends GenericCpuModule {
    private final int[] data;
    private LogicalState writeEnableInput;
    protected LogicalState outputEnableInput;
    private final MemoryAddressRegister memoryAddressRegister;

    public Ram(int size, Bus bus, LogicalState writeEnableInput, LogicalState outputEnableInput, MemoryAddressRegister memoryAddressRegister) {
        super(bus);
        this.data = new int[size];
        this.writeEnableInput = writeEnableInput;
        this.outputEnableInput = outputEnableInput;
        this.memoryAddressRegister = memoryAddressRegister;
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

    public int getLastNonZeroAddress() {
        for (int i = this.data.length - 1; i >= 0; i--) {
            if (this.data[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public void setWriteEnableInput(LogicalState writeEnableInput) {
        this.writeEnableInput = writeEnableInput;
    }

    public void setOutputEnableInput(LogicalState outputEnableInput) {
        this.outputEnableInput = outputEnableInput;
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        if (controlWord.getWord()[ControlSignal.RO.getCode()] == LogicalState.HIGH) {
            this.willWrite = true;
        }
    }

    @Override
    protected void processClockSignal(boolean execute) {
        if (execute && this.clockInput == LogicalState.HIGH) {
            if (this.writeEnableInput == LogicalState.HIGH) {
                this.writeToAddress(this.memoryAddressRegister.getValue(), this.bus.getValue());
            } else if (this.outputEnableInput == LogicalState.HIGH) {
                this.bus.setValue(this.readFromAddress(this.memoryAddressRegister.getValue()));
            }
        }
    }

    @Override
    public void writeToBus() {

    }

    @Override
    public void readFromBus() {

    }
}
