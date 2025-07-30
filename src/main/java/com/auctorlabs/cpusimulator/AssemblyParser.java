package com.auctorlabs.cpusimulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.auctorlabs.cpusimulator.utils.ParseUtils.parseSafeInt;

public class AssemblyParser {
    private static final Map<String, Integer> OPCODES = new HashMap<>();

    static {
        OPCODES.put("NOP", 0x00);
        OPCODES.put("LDA", 0x01);
        OPCODES.put("ADD", 0x02);
        OPCODES.put("SUB", 0x03);
        OPCODES.put("STA", 0x04);
        OPCODES.put("LDI", 0x05);
        OPCODES.put("JMP", 0x06);
        OPCODES.put("JC", 0x07);
        OPCODES.put("JZ", 0x08);
        OPCODES.put("OUT", 0x09);
        OPCODES.put("HLT", 0x10);
    }

    public static int[] parse(String code) {
        List<String> lines = Arrays
                .stream(code.split("\n"))
                .filter(l -> !l.startsWith("#"))
                .collect(Collectors.toList());
        int[] program = new int[lines.size()];
        int counter = 0;
        for (String s : lines) {
            String line = s.trim().toUpperCase();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String mnemonic = parts[0];
            int opcode = OPCODES.getOrDefault(mnemonic, 0);
            int operand = (parts.length > 1) ? parseSafeInt(parts[1]) : 0;
            program[counter++] = (opcode << 4) | (operand & 0xFF);
        }
        return program;
    }
}
