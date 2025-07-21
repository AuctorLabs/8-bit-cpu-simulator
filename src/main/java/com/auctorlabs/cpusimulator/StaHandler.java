package com.auctorlabs.cpusimulator;

public class StaHandler extends InstructionHandler{
    protected StaHandler(
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
        memory[getOperand()] = accumulator.read();
    }
}
