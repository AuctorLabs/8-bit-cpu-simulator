package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericRegister;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class InstructionRegister extends GenericRegister {
    public InstructionRegister(Bus bus) {
        super(bus);
    }

    @Override
    public void writeToBus() {
        this.bus.setValue(this.value & 0xF);
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        if (controlWord.getWord()[ControlSignal.IO.getCode()] == LogicalState.HIGH) {
            this.willWrite = true;
        }
    }
}
