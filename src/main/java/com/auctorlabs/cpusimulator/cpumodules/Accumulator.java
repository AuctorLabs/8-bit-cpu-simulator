package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericRegister;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class Accumulator extends GenericRegister {

    public Accumulator(Bus bus) {
        super(bus);
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        if (controlWord.getWord()[ControlSignal.AO.getCode()] == LogicalState.HIGH) {
            this.willWrite = true;
        }
    }
}
