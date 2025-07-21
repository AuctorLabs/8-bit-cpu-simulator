package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.Instruction;

import java.util.HashMap;
import java.util.Map;

public class InstructionDecoder {
    private final Map<Integer, Instruction> mapping;
    private final Register instructionRegister;

    public InstructionDecoder(Register instructionRegister) {
        this.instructionRegister = instructionRegister;
        mapping = new HashMap<>();
        mapping.put(0X00, Instruction.HLT);
        mapping.put(0X01, Instruction.LDA);
        mapping.put(0X02, Instruction.STA);
        mapping.put(0X03, Instruction.ADD);
        mapping.put(0X04, Instruction.SUB);
        mapping.put(0X05, Instruction.MOV_I);
        mapping.put(0X06, Instruction.MOV);
        mapping.put(0X07, Instruction.JMP);
        mapping.put(0X08, Instruction.JEZ);
    }
    public Instruction decode() {
        int opcode = instructionRegister.read() >> 8; // Higher 8 bits
        return mapping.getOrDefault(opcode, Instruction.NOOP);
    }
}
