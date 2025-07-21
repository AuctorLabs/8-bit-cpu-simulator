package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.Instruction;
import com.auctorlabs.cpusimulator.instructionhandlers.*;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class Cpu {
    // Registers
    private final Register pc = new Register();  // Program Counter
    private final Register ir = new Register();  // Instruction Register
    private final Register acc = new Register(); // Accumulator Register
    private final Register bReg = new Register();// General Purpose Register B
    private final Register flags = new Register();// General Purpose Register B
    private final Alu alu = new Alu(acc);
    private final InstructionDecoder decoder = new InstructionDecoder(ir);
    private final Map<Instruction, InstructionHandler> handlers = new EnumMap<>(Instruction.class);

    // Flags
    private boolean zeroFlag = false;

    // Memory
    private final int[] memory = new int[256]; // 256 words of memory

    public Cpu() {
        this.handlers.put(Instruction.LDA, new LdaHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.STA, new StaHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.ADD, new AddHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.SUB, new SubHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.MOV_I, new MovIHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.MOV, new MovHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.JMP, new JmpHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.JEZ, new JezHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.HLT, new HltHandler(pc, ir, acc, bReg, ir, memory, alu));
        this.handlers.put(Instruction.NOOP, new NoopHandler(pc, ir, acc, bReg, ir, memory, alu));
        reset();
    }

    public void reset() {
        pc.load(0);
        ir.load(0);
        acc.load(0);
        bReg.load(0);
        zeroFlag = false;
        Arrays.fill(memory, 0);
        this.flags.load(0);
    }

    // The main CPU cycle
    public void step() {
        if (pc.read() >= memory.length) return; // End of memory

        // 1. Fetch
        ir.load(memory[pc.read()]);
        pc.load(pc.read() + 1);

        // 2. Decode
        Instruction instruction = decoder.decode();

        // 3. Execute
        InstructionHandler handler = getHandler(instruction);
        handler.execute();

        // Update Zero Flag after every operation
        flags.load(flags.read() | (acc.read() == 0 ? 1 : 0));
    }

    private InstructionHandler getHandler(Instruction instruction) {
        return this.handlers.get(instruction);
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
