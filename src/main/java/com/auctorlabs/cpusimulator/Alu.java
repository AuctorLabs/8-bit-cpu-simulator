package com.auctorlabs.cpusimulator;

public class Alu {
    private final Register acc;

    public Alu(Register acc) {
        this.acc = acc;
    }

    public void add(int operand) {
        acc.load(acc.read() + operand);
    }

    public void sub(int operand) {
        acc.load(acc.read() - operand);
    }
}
