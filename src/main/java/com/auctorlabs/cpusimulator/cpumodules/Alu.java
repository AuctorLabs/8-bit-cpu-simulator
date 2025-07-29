package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class Alu extends GenericCpuModule {

    private LogicalState subtractInput = LogicalState.LOW;
    private final Accumulator accumulator;

    private final BRegister bRegister;

    private int value = 0;
    private LogicalState outputEnableInput;
    private LogicalState subtractionInput;

    public Alu(Bus bus, Accumulator accumulator, BRegister bRegister) {
        super(bus);
        this.accumulator = accumulator;
        this.bRegister = bRegister;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        if (controlWord.getWord()[ControlSignal.EO.getCode()] == LogicalState.HIGH) {
            this.willWrite = true;
        }
    }

    @Override
    protected void processClockSignal(boolean execute) {
        if (this.clockInput == LogicalState.HIGH) {
            if (this.outputEnableInput == LogicalState.HIGH) {
                if (this.subtractInput == LogicalState.HIGH) {
                    this.value = (this.accumulator.getValue() - this.bRegister.getValue());
                } else {
                    this.value = (this.accumulator.getValue() + this.bRegister.getValue());
                }
                if (execute) {
                    this.writeToBus();
                }
            }
        }
    }

    @Override
    public void writeToBus() {
        this.bus.setValue(this.value);
    }

    @Override
    public void readFromBus() {
    }

    public void setOutputEnableInput(LogicalState outputEnableInput) {
        this.outputEnableInput = outputEnableInput;
    }

    public LogicalState getOutputEnableInput() {
        return outputEnableInput;
    }

    public void setSubtractionInput(LogicalState subtractionInput) {
        this.subtractionInput = subtractionInput;
    }

    public LogicalState getSubtractionInput() {
        return subtractionInput;
    }
}
