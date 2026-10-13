export interface AndroidBundleFile {
  path: string;
  content: string;
}

export const ANDROID_BUNDLE_FILES: AndroidBundleFile[] = [
  {
    path: 'settings.gradle.kts',
    content: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TermuxIDE"
include(":app")
`,
  },
  {
    path: 'build.gradle.kts',
    content: `plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}
`,
  },
  {
    path: 'app/build.gradle.kts',
    content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.termuxide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.termuxide"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
`,
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.termuxide">

    <permission
        android:name="com.termux.permission.RUN_COMMAND"
        android:protectionLevel="dangerous" />
    <uses-permission android:name="com.termux.permission.RUN_COMMAND" />

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:label="TermuxIDE"
        android:theme="@style/Theme.TermuxIDE"
        android:hardwareAccelerated="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape"
            android:windowSoftInputMode="adjustResize|stateHidden"
            android:configChanges="orientation|screenSize|screenLayout|keyboard|keyboardHidden|navigation">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".ipc.TermuxResultReceiver"
            android:exported="true" />
    </application>

</manifest>
`,
  },
  {
    path: 'app/src/main/java/com/termuxide/MainActivity.kt',
    content: `package com.termuxide

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.termuxide.databinding.ActivityMainBinding
import com.termuxide.ipc.TermuxBridge
import com.termuxide.model.ProjectFile
import com.termuxide.shortcuts.KeyboardShortcutEngine

/**
 * MainActivity:
 * Landscape max-space Android IDE layout using DrawerLayout and single-modifier shortcuts.
 */
class MainActivity : AppCompatActivity(), TermuxBridge.ExecutionCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shortcutEngine: KeyboardShortcutEngine

    private val workspaceFiles = mutableListOf(
        ProjectFile("1", "main.rs", "src/main.rs", "// TermuxIDE Rust Core\\nfn main() {\\n    println!(\\"TermuxIDE Ready\\");\\n}", "rust"),
        ProjectFile("2", "script.py", "scripts/script.py", "# Termux Python\\nprint(\\"Offline Python on Android\\")", "python")
    )
    private var activeFileIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TermuxBridge.setExecutionCallback(this)

        binding.btnToggleFiles.setOnClickListener { toggleLeftDrawer() }
        binding.btnToggleTerminal.setOnClickListener { toggleRightDrawer() }
        binding.btnRunTermux.setOnClickListener { runActiveFileInTermux() }

        setupKeyboardShortcuts()
        loadActiveFile()
    }

    private fun setupKeyboardShortcuts() {
        shortcutEngine = KeyboardShortcutEngine(
            onSave = { saveActiveFile() },
            onCommandPalette = { showCommandPalette() },
            onToggleTerminal = { toggleRightDrawer() },
            onToggleFiles = { toggleLeftDrawer() },
            onBlockedChord = { chordName ->
                Toast.makeText(this, "Blocked chord [\$chordName]: Mobile stability guard active", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadActiveFile() {
        val file = workspaceFiles[activeFileIndex]
        binding.tvActiveFileName.text = file.name
        binding.tvLanguageMode.text = "\${file.language.uppercase()} (Tree-sitter)"
        binding.nativeCodeEditor.setText(file.content, file.language)
    }

    private fun saveActiveFile() {
        val file = workspaceFiles[activeFileIndex]
        file.content = binding.nativeCodeEditor.getText()
        Toast.makeText(this, "Saved \${file.name} (Ctrl+S)", Toast.LENGTH_SHORT).show()
    }

    private fun toggleLeftDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) binding.drawerLayout.closeDrawer(GravityCompat.START)
        else binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun toggleRightDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) binding.drawerLayout.closeDrawer(GravityCompat.END)
        else binding.drawerLayout.openDrawer(GravityCompat.END)
    }

    private fun runActiveFileInTermux() {
        val file = workspaceFiles[activeFileIndex]
        TermuxBridge.runProjectFile(this, file.name, binding.nativeCodeEditor.getText())
        if (!binding.drawerLayout.isDrawerOpen(GravityCompat.END)) binding.drawerLayout.openDrawer(GravityCompat.END)
    }

    private fun showCommandPalette() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_command_palette)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (shortcutEngine.handleKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onCommandDispatched(command: String, args: Array<String>, workDir: String) {}
    override fun onOutputReceived(stdout: String, stderr: String, exitCode: Int) {}
    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }
}
`,
  },
  {
    path: 'app/src/main/java/com/termuxide/editor/NativeCodeEditor.kt',
    content: `package com.termuxide.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.termuxide.editor.treesitter.TreeSitterEngine
import com.termuxide.editor.treesitter.TokenType

class NativeCodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val treeSitterEngine = TreeSitterEngine("rust")
    private var codeLines = mutableListOf<String>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E4E4E7")
        textSize = 34f
        typeface = Typeface.MONOSPACE
    }

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#52525B")
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }

    private val gutterBgPaint = Paint().apply { color = Color.parseColor("#0E0E11") }
    private val dividerPaint = Paint().apply { color = Color.parseColor("#27272A"); strokeWidth = 2f }

    private var lineHeight = 52f
    private var gutterWidth = 120f

    fun setText(text: String, language: String) {
        codeLines = text.split("\\n").toMutableList()
        treeSitterEngine.setLanguage(language)
        invalidate()
    }

    fun getText(): String = codeLines.joinToString("\\n")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, gutterWidth, height.toFloat(), gutterBgPaint)
        canvas.drawLine(gutterWidth, 0f, gutterWidth, height.toFloat(), dividerPaint)

        for ((idx, line) in codeLines.withIndex()) {
            val y = idx * lineHeight + 40f
            canvas.drawText("\${idx + 1}", 20f, y, gutterPaint)
            val tokens = treeSitterEngine.tokenizeLine(idx, line)
            var x = gutterWidth + 16f
            for (token in tokens) {
                val tokenText = line.substring(token.start, token.end)
                canvas.drawText(tokenText, x, y, textPaint)
                x += textPaint.measureText(tokenText)
            }
        }
    }
}
`,
  },
  {
    path: 'app/src/main/java/com/termuxide/ipc/TermuxBridge.kt',
    content: `package com.termuxide.ipc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object TermuxBridge {
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"

    interface ExecutionCallback {
        fun onCommandDispatched(command: String, args: Array<String>, workDir: String)
        fun onOutputReceived(stdout: String, stderr: String, exitCode: Int)
        fun onError(error: String)
    }

    private var activeCallback: ExecutionCallback? = null

    fun setExecutionCallback(callback: ExecutionCallback?) {
        activeCallback = callback
    }

    fun execute(
        context: Context,
        commandPath: String = "/data/data/com.termux/files/usr/bin/bash",
        arguments: Array<String> = arrayOf("-c", "ls -la"),
        workDir: String = "/data/data/com.termux/files/home",
        runInBackground: Boolean = true
    ): Boolean {
        return try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setPackage(TERMUX_PACKAGE)
                setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
                putExtra("com.termux.RUN_COMMAND_PATH", commandPath)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arguments)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", runInBackground)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
            true
        } catch (e: Exception) {
            activeCallback?.onError(e.message ?: "Failed to start Termux service")
            false
        }
    }

    fun runProjectFile(context: Context, fileName: String, content: String): Boolean {
        val ext = fileName.substringAfterLast('.', "")
        val cmd = when (ext.lowercase()) {
            "rs" -> "rustc \$fileName && ./\${fileName.removeSuffix(\".rs\")}"
            "py" -> "python \$fileName"
            "cpp" -> "g++ \$fileName -o app && ./app"
            else -> "cat \$fileName"
        }
        return execute(context, arguments = arrayOf("-c", cmd))
    }

    fun notifyResult(stdout: String, stderr: String, exitCode: Int) {
        activeCallback?.onOutputReceived(stdout, stderr, exitCode)
    }
}
`,
  },
  {
    path: 'app/src/main/java/com/termuxide/shortcuts/KeyboardShortcutEngine.kt',
    content: `package com.termuxide.shortcuts

import android.view.KeyEvent

class KeyboardShortcutEngine(
    private val onSave: () -> Unit,
    private val onCommandPalette: () -> Unit,
    private val onToggleTerminal: () -> Unit,
    private val onToggleFiles: () -> Unit,
    private val onBlockedChord: (chordName: String) -> Unit
) {
    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isCtrl = event.isCtrlPressed
        val isShift = event.isShiftPressed

        // STRICT GUARD: Intercept and block Ctrl+Shift chords
        if (isCtrl && isShift) {
            val keyLabel = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            onBlockedChord("Ctrl+Shift+\$keyLabel")
            return true
        }

        // Single-modifier handlers (Ctrl + Key only)
        if (isCtrl && !isShift && !event.isAltPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_S -> { onSave(); return true }
                KeyEvent.KEYCODE_P -> { onCommandPalette(); return true }
                KeyEvent.KEYCODE_T -> { onToggleTerminal(); return true }
                KeyEvent.KEYCODE_E -> { onToggleFiles(); return true }
            }
        }
        return false
    }
}
`,
  },
  {
    path: 'gradle/wrapper/gradle-wrapper.properties',
    content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
`,
  },
  {
    path: '.github/workflows/build-apk.yml',
    content: `name: Build Android APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build-apk:
    name: Build & Package TermuxIDE APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Set up Java JDK 17 (Temurin)
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Grant Execute Permission to Gradle Wrapper
        run: |
          if [ -f "android/gradlew" ]; then
            chmod +x android/gradlew
          fi

      - name: Build Android Debug APK
        working-directory: ./android
        run: |
          if [ -f "./gradlew" ]; then
            ./gradlew assembleDebug --stacktrace
          else
            gradle assembleDebug --stacktrace
          fi

      - name: Upload APK to GitHub Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: TermuxIDE-Debug-APK
          path: android/app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
          retention-days: 30
`,
  },
];
