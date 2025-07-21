package com.auctorlabs.cpusimulator;

import com.auctorlabs.cpusimulator.cpumodules.ControlUnit;
import com.auctorlabs.cpusimulator.cpumodules.Memory;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
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
    // --- UI Components ---
    private final ControlUnit controlUnit = new ControlUnit();
    private TextBox codeEditor;
    private Label pcLabel, irLabel, accLabel, bRegLabel, zfLabel;
    private TextBox memoryView;
    private int[] program = new int[] {};

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
                    // Consume the event so it's not passed to other components (like the editor)
                    deliverEvent.set(false);
                }
            }
        });

        // Main panel with a border layout
        Panel mainPanel = new Panel(new BorderLayout());

        // --- Code Editor (Left) ---
        Panel editorPanel = new Panel(new BorderLayout());
        editorPanel.addComponent(new Label("Assembly Code Editor"), BorderLayout.Location.TOP);
        codeEditor = new TextBox(new TerminalSize(80, 15));
        codeEditor.setText("# Example Program:\n" +
                "MOVB 10     # Move 10 into Register B\n" +
                "MOVAB       # Move value from B to Accumulator\n" +
                "STA 20      # Store Accumulator at memory address 20\n" +
                "MOVB 5      # Move 5 into Register B\n" +
                "MOVAB       # Move value from B to Accumulator\n" +
                "SUB 20      # Subtract value at mem[20] from Accumulator (5-10)\n" +
                "STA 21      # Store result (-5) at mem[21]\n" +
                "HLT         # Halt execution");
        editorPanel.addComponent(codeEditor, BorderLayout.Location.CENTER);
        mainPanel.addComponent(editorPanel.withBorder(Borders.singleLine("Editor")), BorderLayout.Location.LEFT);


        // --- CPU Internals (Center) ---
        Panel cpuInternalsPanel = new Panel(new GridLayout(2));
        cpuInternalsPanel.addComponent(createRegistersPanel());
        cpuInternalsPanel.addComponent(createAluPanel());
        cpuInternalsPanel.addComponent(createMemoryPanel());
        cpuInternalsPanel.addComponent(createControlPanel());

        mainPanel.addComponent(cpuInternalsPanel.withBorder(Borders.singleLine("CPU Core")), BorderLayout.Location.CENTER);

        window.setComponent(mainPanel);

        // Initial UI state update
        updateUI();

        // Create and start GUI
        WindowBasedTextGUI textGUI = new MultiWindowTextGUI(terminalScreen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        textGUI.addWindowAndWait(window);
    }

    // --- FIX: Changed return type from Panel to Component ---
    private Component createRegistersPanel() {
        Panel panel = new Panel(new GridLayout(2));
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

    // --- FIX: Changed return type from Panel to Component ---
    private Component createAluPanel() {
        Panel panel = new Panel(new GridLayout(2));
        panel.addComponent(new Label("Zero Flag:"));
        zfLabel = new Label("false");
        panel.addComponent(zfLabel);
        return panel.withBorder(Borders.singleLine("ALU Flags"));
    }

    // --- FIX: Changed return type from Panel to Component ---
    private Component createMemoryPanel() {
        Panel panel = new Panel(new BorderLayout());
        memoryView = new TextBox(new TerminalSize(60, 30));
        memoryView.setReadOnly(true);
        panel.addComponent(memoryView);
        return panel.withBorder(Borders.singleLine("Memory View (Addr: Val)"));
    }

    // --- FIX: Changed return type from Panel to Component ---
    private Component createControlPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(new Button("Load Code", this::loadCode));
        panel.addComponent(new Button("Step", this::step));
        panel.addComponent(new Button("Reset", this::reset));
        return panel.withBorder(Borders.singleLine("Controls"));
    }

    private void loadCode() {
        String code = codeEditor.getText();
        this.program = AssemblyParser.parse(code);
        controlUnit.reset();
        controlUnit.loadProgram(program);
        updateUI();
    }

    private void step() {
        controlUnit.step();
        updateUI();
    }

    private void reset() {
        controlUnit.reset();
        updateUI();
    }

    private void updateUI() {
        // Update registers
        pcLabel.setText(String.valueOf(controlUnit.getPc()));
        irLabel.setText(String.format("0x%04X", controlUnit.getIr()));
        accLabel.setText(String.valueOf(controlUnit.getAcc()));
        bRegLabel.setText(String.valueOf(controlUnit.getBReg()));

        // Update ALU flags
        zfLabel.setText(String.valueOf(controlUnit.isZeroFlag()));

        // Update memory view
        StringBuilder memSb = new StringBuilder();
        Memory memory = controlUnit.getMemory();
        int endAddress = memory.getLastNonZeroAddress() + 1;
        for (int i = 0; i <= endAddress; i++) {
            if (memory.readFromAddress(i) != 0 || (i < this.program.length)) {
                memSb.append(String.format("%s%02d: 0x%04X (%d)\n",
                        i == controlUnit.getPc() ? ">" : " ", i, memory.readFromAddress(i), memory.readFromAddress(i)));
            }
        }
        memoryView.setText(memSb.toString());
        logger.debug("Finished updating the UI");
    }
}
