package com.tty.launcher.domain.models

/** How a single line of terminal output is rendered. */
enum class LineStyle {
    NORMAL,
    DIM,
    ACCENT,
    SUCCESS,
    WARNING,
    ERROR,
    BANNER,
    COMMAND
}

data class OutputLine(
    val text: String,
    val style: LineStyle = LineStyle.NORMAL
) {
    val isBlank: Boolean get() = text.isBlank()
}

/** Result of a command execution. */
data class CommandOutcome(
    val lines: List<OutputLine> = emptyList(),
    val action: TerminalAction? = null,
    val clearScreen: Boolean = false
) {
    companion object {
        fun of(vararg lines: OutputLine): CommandOutcome = CommandOutcome(lines.toList())

        fun text(text: String, style: LineStyle = LineStyle.NORMAL): CommandOutcome =
            CommandOutcome(listOf(OutputLine(text, style)))

        fun error(message: String): CommandOutcome =
            CommandOutcome(listOf(OutputLine(message, LineStyle.ERROR)))
    }
}

/** Line builders keep command implementations readable. */
fun normal(text: String = "") = OutputLine(text, LineStyle.NORMAL)
fun dim(text: String) = OutputLine(text, LineStyle.DIM)
fun accent(text: String) = OutputLine(text, LineStyle.ACCENT)
fun success(text: String) = OutputLine(text, LineStyle.SUCCESS)
fun warning(text: String) = OutputLine(text, LineStyle.WARNING)
fun error(text: String) = OutputLine(text, LineStyle.ERROR)
fun banner(text: String) = OutputLine(text, LineStyle.BANNER)
