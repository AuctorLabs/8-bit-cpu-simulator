package com.auctorlabs.cpusimulator.model;

public enum LogicalState {
    LOW,
    HIGH;

    public static LogicalState not(LogicalState haltInput) {
        if (haltInput == LogicalState.HIGH) {
            return LogicalState.LOW;
        } else {
            return LogicalState.HIGH;
        }
    }
}
