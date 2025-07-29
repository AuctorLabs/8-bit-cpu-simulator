package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.ControlSignal;
import com.auctorlabs.cpusimulator.model.ControlWord;
import com.auctorlabs.cpusimulator.model.GenericRegister;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class ProgramCounter extends GenericRegister {

    private LogicalState incrementInput = LogicalState.LOW;

    public ProgramCounter(Bus bus) {
        super(bus);
    }

    public void setIncrementInput(LogicalState incrementInput) {
        this.incrementInput = incrementInput;
    }

    @Override
    protected void processClockSignal(boolean execute) {
        super.processClockSignal(execute);
        if (execute && this.clockInput == LogicalState.HIGH) {
            if (this.incrementInput == LogicalState.HIGH) {
                this.value++;
            }
        }
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        if (controlWord.getWord()[ControlSignal.CO.getCode()] == LogicalState.HIGH) {
            this.willWrite = true;
        }
    }
}
