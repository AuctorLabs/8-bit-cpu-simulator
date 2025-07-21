package com.auctorlabs.cpusimulator.instructionhandlers;

import com.auctorlabs.cpusimulator.cpumodules.Alu;
import com.auctorlabs.cpusimulator.cpumodules.Register;

public abstract class InstructionHandler {
    protected final Register programCounter;
    protected final Register instructionRegister;
    protected final Register accumulator;
    protected final Register bReg;
    protected final Register flags;
    protected final int[] memory;
    protected final Alu alu;

    protected InstructionHandler(
            Register programCounter,
            Register instructionRegister,
            Register accumulator,
            Register bReg,
            Register flags,
            int[] memory, Alu alu) {
        this.programCounter = programCounter;
        this.instructionRegister = instructionRegister;
        this.accumulator = accumulator;
        this.bReg = bReg;
        this.flags = flags;
        this.memory = memory;
        this.alu = alu;
    }

    public abstract void execute();

    protected int getOperand() {
        return instructionRegister.read() & 0xFF; // Lower 8 bits
    }

    protected boolean isZeroFlag() {
        return (flags.read() & 0x01) == 1;
    }
}
