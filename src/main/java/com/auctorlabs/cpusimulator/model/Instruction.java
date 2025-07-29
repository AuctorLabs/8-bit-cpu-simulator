package com.auctorlabs.cpusimulator.model;

import java.util.HashMap;
import java.util.Map;

public enum Instruction {
    NOP(0),
    LDA(1),
    ADD(2),
    SUB(3),
    STA(4),
    LDI(5),
    JMP(6),
    JC(7),
    JZ(8),
    OUT(9),
    HLT(10);

    private final int code;

    Instruction(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static final Map<Integer, Instruction> BY_CODE = new HashMap<>();

    static {
        for (Instruction status : values()) {
            BY_CODE.put(status.code, status);
        }
    }

    // Lookup method
    public static Instruction fromCode(int code) {
        return BY_CODE.get(code);
    }
}
