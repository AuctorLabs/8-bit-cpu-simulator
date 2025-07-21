package com.auctorlabs.cpusimulator;

public class HltHandler extends InstructionHandler {
    protected HltHandler(
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
        programCounter.load(programCounter.read() - 1); // Stay on HLT instruction
    }
}
