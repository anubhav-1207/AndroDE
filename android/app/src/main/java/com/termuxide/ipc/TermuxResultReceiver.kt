package com.termuxide.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * TermuxResultReceiver:
 * Receives the broadcast intent result returned by Termux RunCommandService
 * after executing a command in the local environment.
 */
class TermuxResultReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TermuxResultReceiver"

        // Termux Result Intent Extras
        private const val EXTRA_STDOUT = "stdout"
        private const val EXTRA_STDERR = "stderr"
        private const val EXTRA_EXIT_CODE = "exitCode"
        private const val EXTRA_ERR = "err"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle == null) {
            Log.w(TAG, "Received empty result bundle from Termux")
            return
        }

        val stdout = bundle.getString(EXTRA_STDOUT) ?: ""
        val stderr = bundle.getString(EXTRA_STDERR) ?: ""
        val exitCode = bundle.getInt(EXTRA_EXIT_CODE, 0)
        val err = bundle.getString(EXTRA_ERR)

        Log.d(TAG, "Termux result received: exitCode=$exitCode, stdoutLen=${stdout.length}, stderrLen=${stderr.length}")

        if (err != null) {
            TermuxBridge.notifyResult(stdout, "Termux execution error: $err\n$stderr", exitCode)
        } else {
            TermuxBridge.notifyResult(stdout, stderr, exitCode)
        }
    }
}
