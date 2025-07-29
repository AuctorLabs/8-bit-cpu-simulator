package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Clock {
    private LogicalState state = LogicalState.LOW;
    private final AtomicLong frequency;
    private final AtomicReference<LogicalState> haltInput;
    private final GenericCpuModule[] cpuModules;
    private boolean debug = false;

    public Clock(int frequency, LogicalState haltInput, GenericCpuModule[] cpuModules) {
        this.frequency = new AtomicLong(frequency);
        this.haltInput = new AtomicReference<>(haltInput);
        this.cpuModules = cpuModules;
    }

    public LogicalState getState() {
        return state;
    }

    public long getFrequency() {
        return frequency.get();
    }

    public void setFrequency(long freq) {
        frequency.set(freq);
    }

    public void tick(boolean debug) throws InterruptedException {
        this.debug = debug;

        if (!this.debug && this.haltInput.get() == LogicalState.HIGH) {
            return;
        }

        this.state = this.state == LogicalState.LOW ? LogicalState.HIGH : LogicalState.LOW;

        for (int i = 0; i < this.cpuModules.length; i++) {
            this.cpuModules[i].setClockInput(this.state, false);
            this.cpuModules[i].setWillWrite(((ControlUnit) this.cpuModules[0]).getControlWord());
        }

        List<GenericCpuModule> sortedWithWritersOnTop = Arrays
                .stream(this.cpuModules)
                .sorted(Comparator.comparing(GenericCpuModule::getWillWrite).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < this.cpuModules.length; i++) {
            sortedWithWritersOnTop
                    .get(i)
                    .setClockInput(this.state, true);
        }

        long periodNanos = (long)(1_000_000_000.0 / frequency.get());
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < periodNanos) {
            // Busy wait to ensure accurate timing
            Thread.onSpinWait(); // Java 9+; safe to skip on older versions
        }
    }

    public LogicalState getHaltInput() {
        return haltInput.get();
    }

    public void setHaltInput(LogicalState haltInput) {
        if (!this.debug) {
            this.haltInput.set(haltInput);
        }
    }

    public void reset() {
        this.debug = false;
        this.state = LogicalState.LOW;
        this.haltInput.set(LogicalState.HIGH);
    }
}
