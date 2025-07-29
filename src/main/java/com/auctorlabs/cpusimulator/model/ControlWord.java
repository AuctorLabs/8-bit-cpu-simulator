package com.auctorlabs.cpusimulator.model;

public class ControlWord {
    private final LogicalState[] word;

    public ControlWord(LogicalState[] word) {
        this.word = word;
    }

    public LogicalState[] getWord() {
        return this.word;
    }

}
