package at.phatbl.shellexec

import java.io.*
import java.util.concurrent.TimeUnit

/**
 * Wrapper for running several commands inside a Bash shell.
 */
data class ShellCommand(
        val baseDir: File,
        val command: String
) {
    lateinit var process: Process

    var standardOutput: OutputStream? = null
    var errorOutput: OutputStream? = null
//    val standardInput: InputStream

    val stdout: String
        get() = stream2String(process.inputStream)

    val stderr: String
        get() = stream2String(process.errorStream)

    var exitValue: Int = -999

    val succeeded: Boolean
        get() = exitValue == 0

    val failed: Boolean
        get() = !succeeded

    /**
     * Runs the command.
     */
    fun start() {
        baseDir.mkdir()
        val pb = ProcessBuilder("bash", "-c", "cd $baseDir && $command")
        process = pb.start()

        if (standardOutput != null) {
            copy(input = process.inputStream, output = standardOutput!!)
        }
        if (errorOutput != null) {
            copy(input = process.errorStream, output = errorOutput!!)
        }

        // 20m
        process.waitFor(1200, TimeUnit.SECONDS)

        exitValue = process.exitValue()
    }

    /**
     * Utility function which converts an input stream into a string.
     */
    private fun stream2String(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val builder = StringBuilder()
        val lineSeparator = System.getProperty("line.separator")
        reader.forEachLine { line ->
            builder.append(line)
            builder.append(lineSeparator)
        }
        return builder.toString()
    }

    private val BUFFER_SIZE = 2 * 1024 * 1024

    @Throws(IOException::class)
    private fun copy(input: InputStream, output: OutputStream) {
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                output.write(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
            //If needed, close streams.
        } finally {
            input.close()
            output.close()
        }
    }
}