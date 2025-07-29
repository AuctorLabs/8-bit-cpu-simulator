package com.auctorlabs.cpusimulator.model;

public class ProgramInstruction {
    private final int opcode;
    private final int operand;

    public ProgramInstruction(int opcode, int operand) {
        this.opcode = opcode;
        this.operand = operand;
    }
}
