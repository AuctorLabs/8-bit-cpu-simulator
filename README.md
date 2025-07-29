# CPU Simulator (80s Terminal Style)

A retro-style CPU simulator built with [Lanterna](https://github.com/mabe02/lanterna), a Java library for creating terminal-based UIs. This application simulates a CPU layout with labeled boxes representing core components and includes a multi-line assembly editor with keyboard input support.

Inspired by 1980s text-mode interface aesthetics.

---

## ✨ Features

- Text-based UI with classic 80s terminal feel
- Boxes representing CPU components:
    - Program Counter
    - Instruction Register
    - ALU
    - Registers
    - Memory
    - Assembly Editor
- Multi-line input editor that advances on `Enter`
- Labels appear at the top-right of each box
- Built entirely with Java and Lanterna
- Fully portable with Gradle wrapper

---

## 📦 Requirements

- Java 17+ (OpenJDK recommended)
- Works on macOS, Linux (including WSL2), and Windows terminals

---

## 🚀 Getting Started

### 1. Clone or Download

Unzip the repo or clone it:

```bash
git clone https://github.com/your-username/cpu-simulator.git
cd cpu-simulator
```

### 2. Run with Gradle Wrapper (No Installation Needed)

```bash
chmod +x gradlew
./gradlew run
```

💡 You can also build:

```bash
./gradlew build
```

---

## 🧠 Project Structure

```text
cpu-simulator/
├── build.gradle               # Gradle build file
├── gradlew / gradlew.bat     # Gradle wrapper scripts
├── gradle/wrapper/           # Gradle wrapper config and JAR
├── settings.gradle           # Root project settings
└── src/main/java/
    └── CPUSimulatorUI.java   # Main UI entry point
```

---

## 🛠 How It Works

This project uses `Lanterna`'s `MultiWindowTextGUI` to draw each CPU component as a bordered panel. The `Assembly Editor` is a multi-line `TextBox` with an input filter that advances on `Enter`.

All UI components are positioned in a grid and rendered using Lanterna's layout system.

---

## 📸 Screenshot (Conceptual Layout)

```
+--------------------+     +------------------------------+
|          PC        |     |      Instruction Register    |
+--------------------+     +------------------------------+
|         ALU        |     |           Registers          |
+--------------------+     +------------------------------+
+--------------------------------------------------------+
|                        Memory                          |
+--------------------------------------------------------+
+--------------------------------------------------------+
|                    Assembly Editor                     |
| MOV A, 01                                               |
| ADD B                                                   |
| ...                                                     |
+--------------------------------------------------------+
```

---

## 🧱 Built With

- Java 17+
- [Lanterna 3.1.1](https://github.com/mabe02/lanterna)
- Gradle 8.5 (via wrapper)

---

## 🧩 Future Ideas

- Parse and execute assembly instructions
- Simulate instruction cycles
- Display ARegister and memory states
- Add syntax highlighting in the editor

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🤝 Contributions

PRs and issues are welcome! Help expand the simulation or improve the UI.

---

## 👨‍🔧 Maintained by AuctorLabs

For updates, releases, and more projects, visit [AuctorLabs](https://auctorlabs.com/).
