package com.auctorlabs.cpusimulator;

public class JmpHandler extends InstructionHandler {
    protected JmpHandler(
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
        programCounter.load(getOperand());
    }
}
