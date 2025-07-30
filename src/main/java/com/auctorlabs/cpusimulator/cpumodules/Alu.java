package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class Alu extends GenericCpuModule {

    private final Accumulator accumulator;

    private final BRegister bRegister;

    private int value = 0;
    private LogicalState outputEnableInput;
    private LogicalState subtractionInput;
    private int zeroFlag = 0;
    private int carryFlag = 0;

    public Alu(Bus bus, Accumulator accumulator, BRegister bRegister) {
        super(bus);
        this.accumulator = accumulator;
        this.bRegister = bRegister;
    }

    public void setValue(int value) {
        this.value = value;
        this.carryFlag = 0;
        this.zeroFlag = 0;
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
                if (this.subtractionInput == LogicalState.HIGH) {
                    this.value = (this.accumulator.getValue() - this.bRegister.getValue()) & 0xFF;
                    this.carryFlag = this.bRegister.getValue() >= this.accumulator.getValue() ? 1 : 0;
                } else {
                    this.value = (this.accumulator.getValue() + this.bRegister.getValue());
                    this.carryFlag = this.value > 255 ? 1 : 0;
                }
                this.zeroFlag = this.value % 256 == 0 ? 1 : 0;

                if (execute) {
                    this.writeToBus();
                }
            }
        }
    }

    @Override
    public void writeToBus() {
        this.bus.setValue(this.value % 256);
    }

    @Override
    public void readFromBus() {
    }

    public void setOutputEnableInput(LogicalState outputEnableInput) {
        this.outputEnableInput = outputEnableInput;
    }

    public void setSubtractionInput(LogicalState subtractionInput) {
        this.subtractionInput = subtractionInput;
    }

    public int getZeroFlag() {
        return zeroFlag;
    }

    public int getCarryFlag() {
        return carryFlag;
    }
}
