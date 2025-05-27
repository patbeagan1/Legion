package io.github.patbeagan1.legion.util

import java.io.BufferedReader
import java.io.InputStreamReader

object RunCommand {
    data class CommandResult(val output: String, val exitCode: Int)

    fun runCommand(command: List<String>): CommandResult = try {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream
            .let { InputStreamReader(it) }
            .let { BufferedReader(it) }
            .use { reader -> reader.readText() }

        val exitCode = process.waitFor()

        CommandResult(output.trim(), exitCode)
    } catch (e: Exception) {
        CommandResult("Error: ${e.message}", -1)
    }
}