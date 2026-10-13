package com.termuxide.ipc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * TermuxBridge:
 * Native IPC bridge communicating with Termux via Android Broadcast Intents
 * using com.termux.RUN_COMMAND.
 * 
 * Supports executing compilers (rustc, gcc, clang, javac), interpreters (python, node, bash),
 * and custom toolchains completely offline within Termux's environment.
 */
object TermuxBridge {

    private const val TAG = "TermuxBridge"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"

    // Termux Intent Extras
    private const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    // Result callback listener interface
    interface ExecutionCallback {
        fun onCommandDispatched(command: String, args: Array<String>, workDir: String)
        fun onOutputReceived(stdout: String, stderr: String, exitCode: Int)
        fun onError(error: String)
    }

    private var activeCallback: ExecutionCallback? = null

    fun setExecutionCallback(callback: ExecutionCallback?) {
        this.activeCallback = callback
    }

    /**
     * Dispatches an executable command directly to Termux's RunCommandService via intent.
     * 
     * @param context Application context
     * @param commandPath Absolute path to binary inside Termux (e.g. /data/data/com.termux/files/usr/bin/bash)
     * @param arguments Arguments array (e.g. arrayOf("-c", "python main.py"))
     * @param workDir Working directory in Termux (e.g. /data/data/com.termux/files/home/project)
     * @param runInBackground True for headless IPC streaming, False to launch Termux foreground session
     */
    fun execute(
        context: Context,
        commandPath: String = "/data/data/com.termux/files/usr/bin/bash",
        arguments: Array<String> = arrayOf("-c", "ls -la"),
        workDir: String = "/data/data/com.termux/files/home",
        runInBackground: Boolean = true
    ): Boolean {
        try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setPackage(TERMUX_PACKAGE)
                setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
                putExtra(EXTRA_RUN_COMMAND_PATH, commandPath)
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arguments)
                putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
                putExtra(EXTRA_RUN_COMMAND_BACKGROUND, runInBackground)
                putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, "0")

                // PendingIntent for IPC result receiver callback
                val resultIntent = Intent(context, TermuxResultReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                putExtra(EXTRA_PENDING_INTENT, pendingIntent)
            }

            context.startService(intent)
            Log.d(TAG, "Dispatched Termux IPC intent for: $commandPath ${arguments.joinToString(" ")}")
            activeCallback?.onCommandDispatched(commandPath, arguments, workDir)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch Termux command", e)
            activeCallback?.onError("Failed to dispatch Termux IPC: ${e.message}")
            return false
        }
    }

    /**
     * Convenience method to run project files depending on their language
     */
    fun runProjectFile(
        context: Context,
        fileName: String,
        fileContent: String,
        workDir: String = "/data/data/com.termux/files/home/workspace"
    ): Boolean {
        val extension = fileName.substringAfterLast('.', "")
        val command = when (extension.lowercase()) {
            "rs" -> "rustc $fileName && ./${fileName.removeSuffix(".rs")}"
            "py" -> "python $fileName"
            "cpp" -> "g++ $fileName -o app && ./app"
            "c" -> "gcc $fileName -o app && ./app"
            "js" -> "node $fileName"
            "ts" -> "npx tsx $fileName"
            "go" -> "go run $fileName"
            "sh" -> "bash $fileName"
            else -> "cat $fileName"
        }

        return execute(
            context = context,
            commandPath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = arrayOf("-c", command),
            workDir = workDir,
            runInBackground = true
        )
    }

    fun notifyResult(stdout: String, stderr: String, exitCode: Int) {
        activeCallback?.onOutputReceived(stdout, stderr, exitCode)
    }
}
