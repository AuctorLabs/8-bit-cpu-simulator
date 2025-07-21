package com.auctorlabs.cpusimulator;

public class SubHandler extends InstructionHandler {
    protected SubHandler(
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
        alu.sub(memory[getOperand()]);
    }
}
