package com.auctorlabs.cpusimulator.model;

import com.auctorlabs.cpusimulator.cpumodules.Bus;

public abstract class GenericCpuModule implements CpuModule {
    protected final Bus bus;

    protected boolean willWrite = false;

    protected LogicalState clockInput = LogicalState.LOW;

    public void setClockInput(LogicalState state, boolean execute) {
        this.clockInput = state;
        if (this.clockInput == LogicalState.HIGH) {
            this.processClockSignal(execute);
            if (execute) {
                this.willWrite = false;
            }
        }
    }

    public boolean getWillWrite() {
        return this.willWrite;
    }

    public abstract void setWillWrite(ControlWord controlWord);

    public GenericCpuModule(Bus bus) {
        this.bus = bus;
    }

    protected abstract void processClockSignal(boolean execute);

    @Override
    public abstract void writeToBus();

    @Override
    public abstract void readFromBus();
}
