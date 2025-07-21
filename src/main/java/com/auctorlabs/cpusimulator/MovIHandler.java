package com.auctorlabs.cpusimulator;

public class MovIHandler extends InstructionHandler {
    protected MovIHandler(
            Register programCounter,
            Register instructionRegister,
            Register accumulator,
            Register bReg,
            Register flags,
            int[] memory, Alu alu) {
        super(programCounter, instructionRegister, accumulator, bReg, flags, memory, alu);
    }

    @Override
    void execute() {
        bReg.load(getOperand());
    }
}
