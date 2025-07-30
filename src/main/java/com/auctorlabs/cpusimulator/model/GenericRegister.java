package com.auctorlabs.cpusimulator.model;

import com.auctorlabs.cpusimulator.cpumodules.Bus;

public class GenericRegister extends GenericCpuModule {

    protected int value;

    protected LogicalState outputEnableInput = LogicalState.LOW;

    protected LogicalState loadInput = LogicalState.LOW;

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setOutputEnableInput(LogicalState state) {
        this.outputEnableInput = state;
    }

    public void setLoadInput(LogicalState state) {
        this.loadInput = state;
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        this.willWrite = false;
    }

    public GenericRegister(Bus bus) {
        super(bus);
    }

    @Override
    protected void processClockSignal(boolean execute) {
        if (execute && this.clockInput == LogicalState.HIGH) {
            if (this.outputEnableInput == LogicalState.HIGH) {
                this.writeToBus();
            } else if (this.loadInput == LogicalState.HIGH) {
                this.readFromBus();
            }
        }
    }

    @Override
    public void writeToBus() {
        this.bus.setValue(this.value);
    }

    @Override
    public void readFromBus() {
        this.value = this.bus.getValue();
    }
}
