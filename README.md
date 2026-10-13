# TermuxIDE (AndroDE) - Ultra-Powerful Native Android IDE

A high-performance Android IDE built specifically for horizontal mobile and tablet layouts. Features incremental syntax highlighting via Tree-sitter, direct offline Termux IPC integration (`com.termux.RUN_COMMAND`), and a single-modifier hardware keyboard shortcut engine.

---

## Key Features

1. **Max-Space Landscape Architecture**
   - Edge-to-edge canvas with Android `DrawerLayout`.
   - Micro Top Status Bar (<28px) displaying file name, dirty status, encoding (UTF-8), language mode, and RAM metrics (9.4 MB native).
   - Left slide-over drawer for Project File Explorer (`Ctrl+E`).
   - Right slide-over drawer for live Termux terminal output stream (`Ctrl+T`).
   - Dark theme palette (`#09090B` editor background, `#E4E4E7` text, `#18181B` panels).

2. **Tree-sitter Incremental Syntax Highlighting**
   - JNI bindings and incremental parser wrapper for Python, Rust, C++, and JavaScript.
   - Re-parses modified lines in sub-millisecond time for 60fps scrolling and instant colorization.
   - Scope-aware token highlighting for keywords, functions, types, strings, numbers, and comments.

3. **Termux IPC Bridge**
   - Broadcast intent integration via `com.termux.RUN_COMMAND`.
   - Direct execution of compilers (`rustc`, `gcc`, `clang`) and interpreters (`python`, `node`, `bash`) completely offline.
   - Captures stdout, stderr, and exit codes via `TermuxResultReceiver`.

4. **Single-Modifier Keyboard Shortcut Engine**
   - Tuned for Bluetooth and OTG external keyboards.
   - `Ctrl+S`: Save file
   - `Ctrl+P`: Command Palette fuzzy search
   - `Ctrl+T`: Toggle Termux Terminal stream
   - `Ctrl+E`: Toggle File Explorer
   - **Strict Guard**: Explicitly intercepts and discards any `Ctrl+Shift` chord to protect mobile keyboard layout stability.

---

## Project Structure

```
├── android/
│   ├── build.gradle.kts           # Root Gradle build script
│   ├── settings.gradle.kts        # Root settings script
│   └── app/
│       ├── build.gradle.kts       # Android App module build script (SDK 34)
│       └── src/main/
│           ├── AndroidManifest.xml # Permissions (com.termux.RUN_COMMAND, storage)
│           ├── java/com/termuxide/
│           │   ├── MainActivity.kt               # Landscape DrawerLayout & shortcuts
│           │   ├── editor/
│           │   │   ├── NativeCodeEditor.kt       # Canvas-drawn gutter & editor
│           │   │   └── treesitter/
│           │   │       ├── TreeSitterEngine.kt   # Incremental CST parser
│           │   │       ├── SyntaxToken.kt        # Token model & types
│           │   │       └── LanguageGrammar.kt    # Language keywords & scopes
│           │   ├── ipc/
│           │   │   ├── TermuxBridge.kt           # com.termux.RUN_COMMAND IPC
│           │   │   └── TermuxResultReceiver.kt   # Intent result receiver
│           │   ├── model/
│           │   │   └── ProjectFile.kt            # File data model
│           │   └── shortcuts/
│           │       └── KeyboardShortcutEngine.kt # Single-modifier engine
│           └── res/
│               ├── layout/                       # DrawerLayout & views
│               └── values/                       # Colors, themes, strings
├── src/                                          # Interactive Live Web Preview
│   ├── App.tsx                                   # Full landscape IDE simulation
│   └── main.tsx
└── package.json                                  # AI Studio dev server & build
```