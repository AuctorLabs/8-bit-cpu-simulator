package com.auctorlabs.cpusimulator.instructionhandlers;

import com.auctorlabs.cpusimulator.cpumodules.Alu;
import com.auctorlabs.cpusimulator.cpumodules.Memory;
import com.auctorlabs.cpusimulator.cpumodules.Register;

public class JmpHandler extends InstructionHandler {
    public JmpHandler(
            Register programCounter,
            Register instructionRegister,
            Register accumulator,
            Register bReg,
            Register flags,
            Memory memory,
            Alu alu) {
        super(programCounter, instructionRegister, accumulator, bReg, flags, memory, alu);
    }

    @Override
    public void execute() {
        programCounter.load(getOperand());
    }
}
