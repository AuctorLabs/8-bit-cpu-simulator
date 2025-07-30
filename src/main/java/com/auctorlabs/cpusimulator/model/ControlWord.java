package com.auctorlabs.cpusimulator.model;

public class ControlWord {
    private final LogicalState[] word;

    public ControlWord(LogicalState[] word) {
        this.word = word;
    }

    public LogicalState[] getWord() {
        return this.word;
    }

    @Override
    public String toString() {
        StringBuilder sbResult = new StringBuilder();
        for (int i = 0; i < this.word.length; i++) {
            sbResult.append(this.word[i].name() + " ");
        }
        return sbResult.toString().trim();
    }
}
