package com.auctorlabs.cpusimulator.instructionhandlers;

import com.auctorlabs.cpusimulator.cpumodules.Alu;
import com.auctorlabs.cpusimulator.cpumodules.Register;

public class LdaHandler extends InstructionHandler{
    public LdaHandler(
            Register programCounter,
            Register instructionRegister,
            Register accumulator,
            Register bReg,
            Register flags,
            int[] memory, Alu alu) {
        super(programCounter, instructionRegister, accumulator, bReg, flags, memory, alu);
    }

    @Override
    public void execute() {
        accumulator.load(memory[getOperand()]);
    }
}
