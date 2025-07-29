package com.auctorlabs.cpusimulator;

import com.auctorlabs.cpusimulator.cpumodules.*;
import com.auctorlabs.cpusimulator.model.GenericCpuModule;
import com.auctorlabs.cpusimulator.model.LogicalState;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private AtomicReference<Label> busLabel = new AtomicReference<>();
    private AtomicReference<Label> outputLabel = new AtomicReference<>();
    private Thread cpuThread;
    private BasicWindow window;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Object pauseLock = new Object();
    private volatile boolean shouldStop = false;

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
                Font monospacedFont = new Font("Courier New", Font.PLAIN, 16); // or any preferred monospaced font
                swingTerminal.setFont(monospacedFont);
                SwingUtilities.invokeLater(() -> swingTerminal.setExtendedState(JFrame.MAXIMIZED_BOTH));
                swingTerminal.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                swingTerminal.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        // Kill CPU thread if running
                        if (cpuThread != null && cpuThread.isAlive()) {
                            try {
                                shouldStop = true;
                                paused.set(false);
                                cpuThread.interrupt();
                                cpuThread.join();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // Then cleanly shut down Lanterna
                        try {
                            terminalScreen.stopScreen();  // Close Lanterna properly
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        System.exit(0);  // Finally exit the app
                    }
                });

            }
            terminalScreen.startScreen();
        } else {
            return;
        }

        // Create the main window
        this.window = new BasicWindow("CPU Emulator");
        this.window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));

        this.window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                KeyStroke ctrlF = new KeyStroke('f', false, true, false);
                KeyStroke ctrlL = new KeyStroke('l', false, true, false);
                KeyStroke ctrlS = new KeyStroke('s', false, true, false);
                KeyStroke ctrlR = new KeyStroke('r', false, true, false);

                boolean handled = false;
                if (keyStroke.equals(ctrlF)) {
                    showLoadModal(basePane.getTextGUI());
                    handled = true;
                } else
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

        // Create and start GUI
        WindowBasedTextGUI textGUI = new MultiWindowTextGUI(terminalScreen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        textGUI.addWindowAndWait(window);

        // Initial UI state update
        updateUI();
    }

    private Component createBottomPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        Component memoryPanel = createMemoryPanel();
        memoryPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Beginning, LinearLayout.GrowPolicy.None));
        panel.addComponent(memoryPanel);

        Component statusPanel = createStatusPanel();
        statusPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
        panel.addComponent(statusPanel);

        return panel;
    }


    private Component createStatusPanel() {
        Panel panel = new Panel(new GridLayout(1));
        panel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        createRegisterPanel(busLabel, panel, "Bus");

        createRegisterPanel(outputLabel, panel, "Output");

        return panel.withBorder(Borders.singleLine("Status"));
    }

    private void createRegisterPanel(AtomicReference<Label> registerLabel, Panel parentPanel, String panelLabel) {
        Panel registerPanel = new Panel();
        registerLabel.set(new Label("0 1 0 1 0 1 0 1"));
        registerPanel.addComponent(registerLabel.get());
        parentPanel.addComponent(registerPanel.withBorder(Borders.singleLine(panelLabel)));
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
        panel.addComponent(new Button("Load File", () -> this.showLoadModal(this.window.getTextGUI())));
        panel.addComponent(new Button("Load Code", this::loadCode));
        panel.addComponent(new Button("Toggle Clock", this::toggleClock));
        panel.addComponent(new Button("Run", this::run));
        panel.addComponent(new Button("Halt", this::halt));
        panel.addComponent(new Button("Step", this::step));
        panel.addComponent(new Button("Reset", this::reset));
        return panel.withBorder(Borders.singleLine("Controls"));
    }

    private void halt() {
        paused.set(true);
        clock.setHaltInput(LogicalState.HIGH);
    }

    private void run() {
        if (cpuThread != null && cpuThread.isAlive() && !shouldStop) {
            // Thread is alive and not stopped — resume if paused
            if (paused.get()) {
                paused.set(false);
                synchronized (pauseLock) {
                    pauseLock.notifyAll();
                }
            }
            return;
        }

        // Start or restart thread
        shouldStop = false;
        paused.set(false);
        running.set(true);
        clock.setHaltInput(LogicalState.LOW);

        cpuThread = new Thread(() -> {
            try {
                while (!shouldStop) {
                    synchronized (pauseLock) {
                        while (paused.get() && !shouldStop) {
                            pauseLock.wait();
                        }
                    }

                    if (shouldStop) break;

                    clock.tick(false);
                    updateUI();
                    Thread.sleep(100);

                    if (clock.getHaltInput() == LogicalState.HIGH) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
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
            // Fully stop any running thread before stepping
            if (cpuThread != null && cpuThread.isAlive()) {
                shouldStop = true;
                paused.set(false);
                cpuThread.interrupt();
                cpuThread.join();
            }

            clock.tick(true);
            updateUI();

            if (clock.getHaltInput() == LogicalState.HIGH) {
                shouldStop = true;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void reset() {
        try {
            if (cpuThread != null && cpuThread.isAlive()) {
                shouldStop = true;
                paused.set(false);
                cpuThread.interrupt();
                cpuThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        controlUnit.reset();
        updateUI();
    }

    private void updateUI() {
        this.window.getTextGUI().getGUIThread().invokeLater(() -> {
            // Update registers
            pcLabel.setText(String.valueOf(programCounter.getValue()));
            irLabel.setText(String.format("0x%04X", instructionRegister.getValue()));
            accLabel.setText(String.valueOf(accumulator.getValue()));
            bRegLabel.setText(String.valueOf(bRegister.getValue()));

            clockLevelLabel.setText(String.valueOf(clock.getState()));
            haltLabel.setText(String.valueOf(clock.getHaltInput()));
            stepsLabel.setText("T" + (this.controlUnit != null ? this.controlUnit.getStateCounter() : 0));

            int busValue = this.bus.getValue() > 255 ? 0 : this.bus.getValue();
            String busBinary = String.format("%8s", Integer.toBinaryString(busValue)).replace(" ", "0");
            String spacedBusBinary = String.join(" ", busBinary.split(""));
            busLabel.get().setText(spacedBusBinary);

            int outputRegisterValue = this.outputRegister.getValue() > 255 ? 0 : this.outputRegister.getValue();
            String outputBinary = String.format("%8s", Integer.toBinaryString(outputRegisterValue)).replace(" ", "0");
            String spacedOutputBinary = String.join(" ", outputBinary.split(""));
            outputLabel.get().setText(spacedOutputBinary);

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
        });
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

    private void showLoadModal(WindowBasedTextGUI gui) {
        final Window modal = new BasicWindow("Load Program from File");
        Panel contentPanel = new Panel(new LinearLayout(Direction.VERTICAL));

        Panel filenameRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        filenameRow.addComponent(new Label("Filename:"));
        TextBox filenameInput = new TextBox(new TerminalSize(60, 1));
        String homeDir = System.getProperty("user.home") + "/";
        filenameInput.setText(homeDir);
        filenameInput.setCaretPosition(homeDir.length() + 1);
        filenameRow.addComponent(filenameInput);

        contentPanel.addComponent(filenameRow);
        contentPanel.addComponent(new EmptySpace(new TerminalSize(1, 1))); // Adds vertical spacing

        Panel buttonPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Button browseButton = new Button("Browse", () -> showFileBrowser(gui, homeDir, filenameInput));


        buttonPanel.addComponent(browseButton);

        Button loadButton = new Button("Load", () -> {
            String filename = filenameInput.getText();
            try {
                String code = new String(Files.readAllBytes(Paths.get(filename)));
                codeEditor.setText(code);
            } catch (IOException e) {
                MessageDialog.showMessageDialog(gui, "Error", "Failed to load file: " + e.getMessage());
            }
            modal.close();
        });
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(1, 1))); // Adds vertical spacing
        buttonPanel.addComponent(loadButton);
        contentPanel.addComponent(buttonPanel);
        modal.setComponent(contentPanel.withBorder(Borders.singleLine("Load Program")));
        modal.setHints(Collections.singletonList(Window.Hint.MODAL));

        gui.addWindow(modal);

        centerWindow(gui, modal);
    }

    private static void centerWindow(WindowBasedTextGUI gui, Window modal) {
        gui.getGUIThread().invokeLater(() -> {
            TerminalSize screenSize = gui.getScreen().getTerminalSize();
            TerminalSize modalSize = modal.getComponent().getPreferredSize();
            int x = (screenSize.getColumns() - modalSize.getColumns()) / 2;
            int y = (screenSize.getRows() - modalSize.getRows()) / 2;
            modal.setPosition(new TerminalPosition(Math.max(0, x), Math.max(0, y)));
        });
    }

    private void showFileBrowser(WindowBasedTextGUI gui, String initialPath, TextBox filenameInput) {
        File currentDir = new File(initialPath);
        BasicWindow fileBrowserWindow = new BasicWindow("Browse Files");
        Panel filePanel = new Panel(new LinearLayout(Direction.VERTICAL));

        Label pathLabel = new Label("Current Path: " + currentDir.getAbsolutePath());
        filePanel.addComponent(pathLabel);

        ActionListBox listBox = new ActionListBox(new TerminalSize(60, 20));

        File[] entries = currentDir.listFiles();
        if (entries != null) {
            Arrays.sort(entries, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            // Parent directory
            if (currentDir.getParentFile() != null) {
                listBox.addItem(".. (Parent Directory)", () -> {
                    fileBrowserWindow.close();
                    showFileBrowser(gui, currentDir.getParent(), filenameInput);
                });
            }

            for (File f : entries) {
                if (f.isDirectory()) {
                    listBox.addItem("[DIR] " + f.getName(), () -> {
                        fileBrowserWindow.close();
                        showFileBrowser(gui, f.getAbsolutePath(), filenameInput);
                    });
                } else if (f.getName().endsWith(".txt") || f.getName().endsWith(".asm")) {
                    listBox.addItem(f.getName(), () -> {
                        filenameInput.setText(f.getAbsolutePath());
                        fileBrowserWindow.close();
                    });
                }
            }
        }

        filePanel.addComponent(listBox);
        fileBrowserWindow.setComponent(filePanel.withBorder(Borders.singleLine("Files")));
        fileBrowserWindow.setHints(Collections.singletonList(Window.Hint.MODAL));
        gui.addWindow(fileBrowserWindow);
        centerWindow(gui, fileBrowserWindow);
    }

}
