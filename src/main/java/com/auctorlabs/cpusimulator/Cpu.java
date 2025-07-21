package com.auctorlabs.cpusimulator;

import java.util.Arrays;

public class Cpu {
    // Registers
    private final Register pc = new Register();  // Program Counter
    private final Register ir = new Register();  // Instruction Register
    private final Register acc = new Register(); // Accumulator Register
    private final Register bReg = new Register();// General Purpose Register B
    private final Alu alu = new Alu(acc);

    // Flags
    private boolean zeroFlag = false;

    // Memory
    private final int[] memory = new int[256]; // 256 words of memory

    public void reset() {
        pc.load(0);
        ir.load(0);
        acc.load(0);
        bReg.load(0);
        zeroFlag = false;
        Arrays.fill(memory, 0);
    }

    // The main CPU cycle
    public void step() {
        if (pc.read() >= memory.length) return; // End of memory

        // 1. Fetch
        ir.load(memory[pc.read()]);
        pc.load(pc.read() + 1);

        // 2. Decode & 3. Execute
        int opcode = ir.read() >> 8; // Higher 8 bits
        int operand = ir.read() & 0xFF; // Lower 8 bits

        switch (opcode) {
            case 0x01: // LDA addr -> Load Accumulator from memory
                acc.load(memory[operand]);
                break;
            case 0x02: // STA addr -> Store Accumulator to memory
                memory[operand] = acc.read();
                break;
            case 0x03: // ADD addr -> Add value from memory to Accumulator
                alu.add(memory[operand]);
                break;
            case 0x04: // SUB addr -> Subtract value from memory from Accumulator
                alu.sub(memory[operand]);
                break;
            case 0x05: // MOV B, val -> Move immediate value to B register
                bReg.load(operand);
                break;
            case 0x06: // MOV A, B -> Move value from B to A
                acc.load(bReg);
                break;
            case 0x07: // JMP addr -> Jump to address
                pc.load(operand);
                break;
            case 0x08: // JEZ addr -> Jump if Zero Flag is true
                if (zeroFlag) {
                    pc.load(operand);
                }
                break;
            case 0x00: // HLT -> Halt
                pc.load(pc.read() - 1); // Stay on HLT instruction
                break;
            default: // NOP (No Operation) for unknown opcodes
                break;
        }

        // Update Zero Flag after every operation
        zeroFlag = (acc.read() == 0);
    }

    public void loadProgram(int[] program) {
        System.arraycopy(program, 0, memory, 0, program.length);
    }

    // Getters for UI
    public int getPc() {
        return pc.read();
    }

    public int getIr() {
        return ir.read();
    }

    public int getAcc() {
        return acc.read();
    }

    public int getBReg() {
        return bReg.read();
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    public int[] getMemory() {
        return memory;
    }
}
