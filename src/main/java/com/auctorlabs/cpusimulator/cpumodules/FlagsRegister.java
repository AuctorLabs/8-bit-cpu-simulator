package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.GenericRegister;
import com.auctorlabs.cpusimulator.model.LogicalState;

public class FlagsRegister extends GenericRegister {

    private Alu alu;

    public FlagsRegister(Bus bus, Alu alu) {
        super(bus);
        this.alu = alu;
    }

    @Override
    protected void processClockSignal(boolean execute) {
        if (execute && this.clockInput == LogicalState.HIGH) {
            if (this.loadInput == LogicalState.HIGH) {
                this.value = this.alu.getZeroFlag() << 1 | this.alu.getCarryFlag();
            }
        }
    }

    public void setAlu(Alu alu) {
        this.alu = alu;
    }
}
