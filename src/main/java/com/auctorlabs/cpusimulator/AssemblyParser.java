package com.auctorlabs.cpusimulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyParser {
    private static final Map<String, Integer> OPCODES = new HashMap<>();

    static {
        OPCODES.put("HLT", 0x00);
        OPCODES.put("LDA", 0x01);
        OPCODES.put("STA", 0x02);
        OPCODES.put("ADD", 0x03);
        OPCODES.put("SUB", 0x04);
        OPCODES.put("MOVB", 0x05); // Simplified: MOV B, val -> MOVB val
        OPCODES.put("MOVAB", 0x06); // Simplified: MOV A, B -> MOVAB
        OPCODES.put("JMP", 0x07);
        OPCODES.put("JEZ", 0x08);
    }

    public static int[] parse(String code) {
        List<String> lines = Arrays.asList(code.split("\n"));
        int[] program = new int[lines.size()];
        int counter = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.isEmpty() || line.startsWith("#")) { // Skip empty lines and comments
                continue;
            }
            String[] parts = line.split("\\s+");
            String mnemonic = parts[0];
            int opcode = OPCODES.getOrDefault(mnemonic, 0);
            int operand = 0;
            if (parts.length > 1) {
                try {
                    operand = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    operand = 0; // Default if parsing fails
                }
            }
            program[counter] = (opcode << 8) | (operand & 0xFF);
            counter++;
        }
        return program;
    }
}
