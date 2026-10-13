package com.termuxide.shortcuts

import android.view.KeyEvent

/**
 * KeyboardShortcutEngine:
 * Strict single-modifier keyboard shortcut engine designed for Bluetooth
 * and OTG hardware keyboards attached to Android mobile devices/tablets.
 * 
 * STRICT MODIFIER RULE:
 * Maps all IDE actions exclusively to single-modifier keys (Ctrl + Key).
 * Explicitly intercepts and blocks any multi-modifier chords requiring Ctrl + Shift
 * to prevent layout confusion and IME lockouts on mobile OSs.
 */
class KeyboardShortcutEngine(
    private val onSave: () -> Unit,
    private val onCommandPalette: () -> Unit,
    private val onToggleTerminal: () -> Unit,
    private val onToggleFiles: () -> Unit,
    private val onBlockedChord: (chordName: String) -> Unit
) {

    /**
     * Intercepts and dispatches hardware key events.
     * Returns true if the key event was consumed.
     */
    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isCtrl = event.isCtrlPressed
        val isShift = event.isShiftPressed
        val isAlt = event.isAltPressed

        // 1. STRICT GUARD: Explicitly bypass and block multi-modifier chords requiring Ctrl + Shift
        if (isCtrl && isShift) {
            val keyLabel = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            onBlockedChord("Ctrl+Shift+$keyLabel")
            return true // Consume to protect external keyboard state
        }

        // 2. Strict Single-Modifier Actions (Ctrl + Key only)
        if (isCtrl && !isShift && !isAlt) {
            when (keyCode) {
                KeyEvent.KEYCODE_S -> {
                    onSave()
                    return true
                }
                KeyEvent.KEYCODE_P -> {
                    onCommandPalette()
                    return true
                }
                KeyEvent.KEYCODE_T -> {
                    onToggleTerminal()
                    return true
                }
                KeyEvent.KEYCODE_E -> {
                    onToggleFiles()
                    return true
                }
            }
        }

        return false
    }
}
