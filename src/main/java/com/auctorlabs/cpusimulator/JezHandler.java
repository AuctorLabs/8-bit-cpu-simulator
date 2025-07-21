package com.auctorlabs.cpusimulator;

public class JezHandler extends InstructionHandler {
    protected JezHandler(
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
        if (isZeroFlag()) {
            programCounter.load(getOperand());
        }
    }
}
