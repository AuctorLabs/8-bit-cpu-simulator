package com.auctorlabs.cpusimulator;

public class NoopHandler extends InstructionHandler {
    protected NoopHandler(
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
    }
}
