package com.auctorlabs.cpusimulator;

import com.auctorlabs.cpusimulator.cpumodules.*;
import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple CPU emulator with a Text-based User Interface (TUI) using the Lanterna library.
 * This application simulates a basic CPU with registers, memory, and an ALU,
 * allowing users to enter assembly-like code and step through its execution.
 * To run this, you need the Lanterna 3 library in your classpath.
 */

public class CpuSimulatorUI {
    private static final Logger logger = LogManager.getLogger(CpuSimulatorUI.class);
    public static final String FIRMWARE_TXT = "firmware.txt";
    // --- UI Components ---
    private Clock clock;
    private ControlUnit controlUnit;
    private TextBox codeEditor;
    private Label pcLabel, irLabel, accLabel, bRegLabel, zfLabel, haltLabel, clockLevelLabel, stepsLabel;
    private TextBox memoryView;
    private int[] program = new int[]{};
    private Ram ram;
    private ProgramCounter programCounter;
    private InstructionRegister instructionRegister;
    private Accumulator accumulator;
    private BRegister bRegister;
    private FlagsRegister flagsRegister;
    private Alu alu;
    private OutputRegister outputRegister;
    private MemoryAddressRegister memoryAddressRegister;
    private Rom romA;
    private Rom romB;
    private Bus bus;
    private Label busLabel;
    private Label outputLabel;
    private Thread cpuThread;

    public static void main(String[] args) {
        try {
            new CpuSimulatorUI().start();
        } catch (IOException e) {
            logger.error("An error occurred", e);
        }
    }

    public void start() throws IOException {
        // Set up the terminal and screen
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        terminalFactory.setTerminalEmulatorTitle("CPU Emulator");
        TerminalScreen terminalScreen = terminalFactory.createScreen();
        if (terminalScreen != null) {
            if (terminalScreen.getTerminal() instanceof SwingTerminalFrame) {
                SwingTerminalFrame swingTerminal = (SwingTerminalFrame) terminalScreen.getTerminal();
                SwingUtilities.invokeLater(() -> swingTerminal.setExtendedState(JFrame.MAXIMIZED_BOTH));
            }
            terminalScreen.startScreen();
        } else {
            return;
        }

        // Create the main window
        BasicWindow window = new BasicWindow("CPU Emulator");
        window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));

        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                KeyStroke ctrlL = new KeyStroke('l', false, true, false);
                KeyStroke ctrlS = new KeyStroke('s', false, true, false);
                KeyStroke ctrlR = new KeyStroke('r', false, true, false);

                boolean handled = false;
                if (keyStroke.equals(ctrlL)) {
                    loadCode();
                    handled = true;
                } else if (keyStroke.equals(ctrlS)) {
                    step();
                    handled = true;
                } else if (keyStroke.equals(ctrlR)) {
                    reset();
                    handled = true;
                }

                if (handled) {
                    // Consume the event, so it's not passed to other components (like the editor)
                    deliverEvent.set(false);
                }
            }
        });

        // Main panel with a border layout
        Panel mainPanel = new Panel(new BorderLayout());

        // --- Code Editor (Left) ---
        Panel editorPanel = new Panel(new BorderLayout());
        editorPanel.addComponent(new Label("Assembly Code Editor"), BorderLayout.Location.TOP);
        codeEditor = new TextBox(new TerminalSize(50, 15));
        codeEditor.setText("# Example Program:\n" +
                "LDI 10     ; Load immediate value 10 into the accumulator\n" +
                "STA 14     ; Store the value at memory address 14\n" +
                "LDI 5      ; Load immediate value 5 into the accumulator\n" +
                "ADD 14     ; Add value from memory address 14 to the accumulator (5 + 10)\n" +
                "OUT        ; Output the result (should be 15)\n" +
                "HLT        ; Halt the program");
        editorPanel.addComponent(codeEditor, BorderLayout.Location.CENTER);
        mainPanel.addComponent(editorPanel.withBorder(Borders.singleLine("Editor")), BorderLayout.Location.LEFT);


        // --- CPU Internals (Center) ---
        Panel cpuInternalsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        cpuInternalsPanel.addComponent(createInfoPanel());
        cpuInternalsPanel.addComponent(createControlPanel());
        cpuInternalsPanel.addComponent(createBottomPanel());

        mainPanel.addComponent(cpuInternalsPanel.withBorder(Borders.singleLine("CPU Core")), BorderLayout.Location.CENTER);

        window.setComponent(mainPanel);

        initializeCircuit();

        // Initial UI state update
        updateUI();

        // Create and start GUI
        WindowBasedTextGUI textGUI = new MultiWindowTextGUI(terminalScreen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        textGUI.addWindowAndWait(window);
    }

    private Component createBottomPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        panel.addComponent(createMemoryPanel());
        panel.addComponent(createStatusPanel());
        return panel;
    }

    private Component createStatusPanel() {
        Panel panel = new Panel(new GridLayout(1));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        busLabel = new Label("Bus: 0 1 0 1 0 1 0 1");
        panel.addComponent(busLabel);
        outputLabel = new Label("Output: 0 1 0 1 0 1 0 1");
        panel.addComponent(outputLabel);
        return panel.withBorder(Borders.singleLine("Status"));
    }

    private Border createInfoPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        panel.addComponent(createRegistersPanel());
        panel.addComponent(createAluPanel());
        panel.addComponent(createClockPanel());
        panel.addComponent(createControlUnitPanel());
        return panel.withBorder(Borders.singleLine("Info"));
    }

    private Component createRegistersPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        panel.addComponent(new Label("PC:"));
        pcLabel = new Label("0");
        panel.addComponent(pcLabel);
        panel.addComponent(new Label("IR:"));
        irLabel = new Label("0x0000");
        panel.addComponent(irLabel);
        panel.addComponent(new Label("ACC:"));
        accLabel = new Label("0");
        panel.addComponent(accLabel);
        panel.addComponent(new Label("B REG:"));
        bRegLabel = new Label("0");
        panel.addComponent(bRegLabel);
        return panel.withBorder(Borders.singleLine("Registers"));
    }

    private Component createAluPanel() {
        Panel panel = new Panel(new GridLayout(2));
        panel.addComponent(new Label("Zero Flag:"));
        zfLabel = new Label("false");
        panel.addComponent(zfLabel);
        return panel.withBorder(Borders.singleLine("ALU Flags"));
    }

    private Component createControlUnitPanel() {
        Panel panel = new Panel(new GridLayout(2));
        panel.addComponent(new Label("Step:"));
        stepsLabel = new Label("T" + (this.controlUnit != null ? this.controlUnit.getStateCounter() : 0));
        panel.addComponent(stepsLabel);
        return panel.withBorder(Borders.singleLine("Control Unit"));
    }

    private Component createClockPanel() {
        Panel panel = new Panel(new GridLayout(2));
        panel.addComponent(new Label("HALT:"));
        haltLabel = new Label("false");
        panel.addComponent(haltLabel);
        panel.addComponent(new Label("Level:"));
        clockLevelLabel = new Label("HIGH");
        panel.addComponent(clockLevelLabel);
        return panel.withBorder(Borders.singleLine("Clock"));
    }

    // --- FIX: Changed return type from Panel to Component ---
    private Component createMemoryPanel() {
        Panel panel = new Panel(new BorderLayout());
        memoryView = new TextBox(new TerminalSize(50, 30));
        memoryView.setReadOnly(true);
        panel.addComponent(memoryView);
        return panel.withBorder(Borders.singleLine("Memory View (Addr: Val)"));
    }

    // --- FIX: Changed return type from Panel to Component ---
    private Component createControlPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        panel.addComponent(new Button("Load Code", this::loadCode));
        panel.addComponent(new Button("Toggle Clock", this::toggleClock));
        panel.addComponent(new Button("Run", this::run));
        panel.addComponent(new Button("Halt", this::halt));
        panel.addComponent(new Button("Step", this::step));
        panel.addComponent(new Button("Reset", this::reset));
        return panel.withBorder(Borders.singleLine("Controls"));
    }

    private void halt() {
        this.cpuThread.interrupt();
    }

    private void run() {
        clock.setHaltInput(LogicalState.LOW);
        cpuThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    clock.tick(false);
                    updateUI();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        cpuThread.start();
    }

    private void toggleClock() {
        this.clock.setHaltInput(LogicalState.not(this.clock.getHaltInput()));
        this.haltLabel.setText(this.clock.getHaltInput().name());
    }

    private void loadCode() {
        String code = codeEditor.getText();
        this.program = AssemblyParser.parse(code);
        this.clock.reset();
        this.controlUnit.reset();
        this.ram.writeAll(this.program);
        updateUI();
    }

    private void step() {
        try {
            clock.tick(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        updateUI();
    }

    private void reset() {
        this.cpuThread.interrupt();
        controlUnit.reset();
        updateUI();
    }

    private void updateUI() {
        // Update registers
        pcLabel.setText(String.valueOf(programCounter.getValue()));
        irLabel.setText(String.format("0x%04X", instructionRegister.getValue()));
        accLabel.setText(String.valueOf(accumulator.getValue()));
        bRegLabel.setText(String.valueOf(bRegister.getValue()));

        clockLevelLabel.setText(String.valueOf(clock.getState()));
        haltLabel.setText(String.valueOf(clock.getHaltInput()));
        stepsLabel.setText("T" + (this.controlUnit != null ? this.controlUnit.getStateCounter() : 0));

        busLabel.setText(String.format("Bus: %8s", Integer.toBinaryString(this.bus.getValue())).replace(" ", "0"));
        outputLabel.setText(String.format("Output: %8s", Integer.toBinaryString(this.outputRegister.getValue())).replace(" ", "0"));

        // Update ALU flags
        zfLabel.setText(String.valueOf(((flagsRegister.getValue() & 0xFF) >>> 7) == 1 ? LogicalState.HIGH : LogicalState.LOW));

        // Update memory view
        StringBuilder memSb = new StringBuilder();

        int endAddress = this.ram.getLastNonZeroAddress() + 1;
        for (int i = 0; i <= endAddress; i++) {
            if (this.ram.readFromAddress(i) != 0 || (i < this.program.length)) {
                memSb.append(String.format(
                        "%s%02d: 0x%04X (%d)\n",
                        i == this.programCounter.getValue() ? ">" : " ",
                        i,
                        this.ram.readFromAddress(i),
                        this.ram.readFromAddress(i) & 0xFF));
            }
        }
        memoryView.setText(memSb.toString());
        logger.debug("Finished updating the UI");
    }

    private void initializeCircuit() throws IOException {
        this.bus = new Bus();
        this.programCounter = new ProgramCounter(bus);
        this.instructionRegister = new InstructionRegister(bus);
        this.accumulator = new Accumulator(bus);
        this.bRegister = new BRegister(bus);
        this.flagsRegister = new FlagsRegister(bus);
        this.alu = new Alu(bus, accumulator, bRegister);
        this.outputRegister = new OutputRegister(bus);
        this.memoryAddressRegister = new MemoryAddressRegister(bus);
        this.romA = new Rom(512);
        this.romB = new Rom(512);
        this.ram = new Ram(512, bus, LogicalState.LOW, LogicalState.LOW, memoryAddressRegister);

        GenericCpuModule[] cpuModules = new GenericCpuModule[10];

        this.clock = new Clock(10, LogicalState.HIGH, cpuModules);
        this.controlUnit = new ControlUnit(clock, bus, programCounter, instructionRegister,
                accumulator, bRegister, outputRegister, flagsRegister, memoryAddressRegister, alu, romA, romB, ram);

        cpuModules[0] = controlUnit;
        cpuModules[1] = accumulator;
        cpuModules[2] = ram;
        cpuModules[3] = programCounter;
        cpuModules[4] = instructionRegister;
        cpuModules[5] = bRegister;
        cpuModules[6] = flagsRegister;
        cpuModules[7] = alu;
        cpuModules[8] = memoryAddressRegister;
        cpuModules[9] = outputRegister;

        this.initializeFirmware();
    }

    private void initializeFirmware() throws IOException {
        int[] firmwareCode = parseFirmwareFile();

        for (int i = 0; i < firmwareCode.length; i++) {
            int low = firmwareCode[i] & 0xFF;
            int high = firmwareCode[i] >> 8;
            this.romA.writeToAddress(i, low);
            this.romB.writeToAddress(i, high);
        }
    }

    private static int[] parseFirmwareFile() throws IOException {
        int[] firmwareCode = new int[512];
        // First, attempt to get the resource stream
        InputStream inputStream = CpuSimulatorUI.class.getClassLoader().getResourceAsStream(FIRMWARE_TXT);

        if (inputStream == null) {
            throw new IOException("Failed to find the firmware resource: " + FIRMWARE_TXT);
        }

        try (InputStream is = inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int address = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                firmwareCode[address] = Integer.parseInt(line.substring(0, 16), 2);
                address++;
            }
        } catch (IOException e) {
            System.err.println("Error reading resource: " + e.getMessage());
            e.printStackTrace();
        }
        return firmwareCode;
    }

}
