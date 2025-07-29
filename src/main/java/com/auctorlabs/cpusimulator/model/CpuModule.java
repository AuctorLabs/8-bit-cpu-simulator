package com.auctorlabs.cpusimulator.model;

import com.auctorlabs.cpusimulator.model.LogicalState;

public interface CpuModule {
    void readFromBus();

    void writeToBus();
}
