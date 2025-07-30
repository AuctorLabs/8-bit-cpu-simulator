package com.auctorlabs.cpusimulator.cpumodules;

import com.auctorlabs.cpusimulator.CpuSimulatorUI;
import com.auctorlabs.cpusimulator.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ControlUnit extends GenericCpuModule {
    private static final Logger logger = LogManager.getLogger(ControlUnit.class);
    private int stateCounter;
    private ControlWord controlWord = new ControlWord(new LogicalState[] {
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW,
            LogicalState.LOW
    });

    public void reset() {
        this.stateCounter = 0;
        this.programCounter.setValue(0);
        this.accumulator.setValue(0);
        this.bRegister.setValue(0);
        this.flagsRegister.setValue(0);
        this.instructionRegister.setValue(0);
        this.memoryAddressRegister.setValue(0);
        this.outputRegister.setValue(0);
        this.clock.reset();
        this.bus.setValue(0);
        this.alu.setValue(0);
        this.ram.fillWithZeros();
    }

    private final Clock clock;

    private final ProgramCounter programCounter;  // Program Counter

    private final InstructionRegister instructionRegister;

    private final Accumulator accumulator;

    private final BRegister bRegister;
    private final OutputRegister outputRegister;

    private final FlagsRegister flagsRegister;

    private final MemoryAddressRegister memoryAddressRegister;

    private final Alu alu;

    private final Rom romA;

    private final Rom romB;

    private final Ram ram;

    public int getStateCounter() {
        return this.stateCounter;
    }

    public ControlUnit(
            Clock clock, Bus bus,
            ProgramCounter programCounter,
            InstructionRegister instructionRegister,
            Accumulator accumulator,
            BRegister bRegister,
            OutputRegister outputRegister, FlagsRegister flags,
            MemoryAddressRegister memoryAddressRegister, Alu alu,
            Rom romA, Rom romB, Ram ram) {
        super(bus);
        this.clock = clock;
        this.programCounter = programCounter;
        this.instructionRegister = instructionRegister;
        this.accumulator = accumulator;
        this.bRegister = bRegister;
        this.outputRegister = outputRegister;
        this.flagsRegister = flags;
        this.memoryAddressRegister = memoryAddressRegister;
        this.alu = alu;
        this.romA = romA;
        this.romB = romB;
        this.ram = ram;
        this.stateCounter = 0;
    }

    public void fetchDecodeExecute(boolean execute) {
        int controlWordAddress = this.assembleControlWordAddress();
        this.fetchControlWord(controlWordAddress);
        this.handleControlWord(this.controlWord);
        if (execute) {
            logger.debug("Executing control word: " + this.controlWord);
            logger.debug("At program line: " + this.programCounter.getValue());
            logger.debug("With MAR pointing to: " + this.memoryAddressRegister.getValue());
            logger.debug("Where RAM contains: " + this.ram.readFromAddress(this.memoryAddressRegister.getValue()));
            logger.debug("At ROM address: " + controlWordAddress);
            logger.debug("With Flags: " + this.flagsRegister.getValue());
            logger.debug("At microstep: T" + this.stateCounter);
            logger.debug("For CPU instruction: " + this.instructionRegister.getValue());
            logger.debug("Accumulator has: " + this.accumulator.getValue());
            logger.debug("-------------------------------------------------\n\n");
            this.stateCounter++;
            if (this.stateCounter > 7) {
                this.stateCounter = 0;
            }
        }
    }

    private int assembleControlWordAddress() {
        int flagBits = ((this.alu.getZeroFlag() << 1) | this.alu.getCarryFlag()) << 7;

        int programInstructionBits = (((this.instructionRegister.getValue() & 0xF0) >> 4) << 3);

        int stateCounterBits = (this.stateCounter & 0x07);

        return flagBits | programInstructionBits | stateCounterBits;
    }

    private void handleControlWord(ControlWord controlWord) {
        try {
            this.clock.setHaltInput(controlWord.getWord()[ControlSignal.HLT.getCode()]);
            this.memoryAddressRegister.setLoadInput(controlWord.getWord()[ControlSignal.MI.getCode()]);
            this.ram.setWriteEnableInput(controlWord.getWord()[ControlSignal.RI.getCode()]);
            this.ram.setOutputEnableInput(controlWord.getWord()[ControlSignal.RO.getCode()]);
            this.instructionRegister.setOutputEnableInput(controlWord.getWord()[ControlSignal.IO.getCode()]);
            this.instructionRegister.setLoadInput(controlWord.getWord()[ControlSignal.II.getCode()]);
            this.accumulator.setLoadInput(controlWord.getWord()[ControlSignal.AI.getCode()]);
            this.accumulator.setOutputEnableInput(controlWord.getWord()[ControlSignal.AO.getCode()]);
            this.alu.setOutputEnableInput(controlWord.getWord()[ControlSignal.EO.getCode()]);
            this.alu.setSubtractionInput(controlWord.getWord()[ControlSignal.SU.getCode()]);
            this.bRegister.setLoadInput(controlWord.getWord()[ControlSignal.BI.getCode()]);
            this.outputRegister.setLoadInput(controlWord.getWord()[ControlSignal.OI.getCode()]);
            this.programCounter.setIncrementInput(controlWord.getWord()[ControlSignal.CE.getCode()]);
            this.programCounter.setOutputEnableInput(controlWord.getWord()[ControlSignal.CO.getCode()]);
            this.programCounter.setLoadInput(controlWord.getWord()[ControlSignal.J.getCode()]);
            this.flagsRegister.setLoadInput(controlWord.getWord()[ControlSignal.FI.getCode()]);
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    @Override
    public void setWillWrite(ControlWord controlWord) {
        this.willWrite = false;
    }

    @Override
    public void processClockSignal(boolean execute) {
        if (this.clockInput == LogicalState.HIGH) {
            this.fetchDecodeExecute(execute);
        }
    }

    @Override
    public void writeToBus() {

    }

    @Override
    public void readFromBus() {

    }

    private void fetchControlWord(int address) {
        int low = this.romA.readFromAddress(address);
        int high = this.romB.readFromAddress(address);
        int content = ((high << 8) | low);
        LogicalState[] word = new LogicalState[16];
        for (int i = 0; i < 16; i++) {
            if (((content & ((int) Math.pow(2, i))) >> i) == 1) {
                word[i] = LogicalState.HIGH;
            } else {
                word[i] = LogicalState.LOW;
            }
        }
        this.controlWord = new ControlWord(word);
    }

    public ControlWord getControlWord() {
        return controlWord;
    }
}
