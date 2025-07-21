package com.auctorlabs.cpusimulator.instructionhandlers;

import com.auctorlabs.cpusimulator.cpumodules.Alu;
import com.auctorlabs.cpusimulator.cpumodules.Memory;
import com.auctorlabs.cpusimulator.cpumodules.Register;

public class MovBHandler extends InstructionHandler {
    public MovBHandler(
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
        bReg.load(getOperand());
    }
}
