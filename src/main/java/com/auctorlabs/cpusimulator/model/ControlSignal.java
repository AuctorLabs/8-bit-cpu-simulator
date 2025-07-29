package com.auctorlabs.cpusimulator.model;

public enum ControlSignal {
    HLT(0),
    MI(1),
    RI(2),
    RO(3),
    IO(4),
    II(5),
    AI(6),
    AO(7),
    EO(8),
    SU(9),
    BI(10),
    OI(11),
    CE(12),
    CO(13),
    J(14),
    FI(15);

    private final int code;

    ControlSignal(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
