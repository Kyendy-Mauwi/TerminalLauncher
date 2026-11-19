package com.tty.launcher.presentation.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tty.launcher.domain.commands.ExpressionEvaluator
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.theme.LocalTerminalPalette

/**
 * Native calculator screen reachable from the `calculator` command. Uses the same safe
 * expression evaluator as the terminal, so no evaluation code is duplicated.
 */
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    val palette = LocalTerminalPalette.current
    var expression by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun evaluate() {
        val normalized = expression
            .replace("÷", "/")
            .replace("×", "*")
            .replace("−", "-")
        when (val outcome = ExpressionEvaluator.evaluate(normalized)) {
            is ExpressionEvaluator.Result.Value -> {
                result = ExpressionEvaluator.format(outcome.value)
                error = null
            }
            is ExpressionEvaluator.Result.Failure -> {
                error = outcome.reason
                result = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalButton(label = "< BACK", onClick = onBack, contentDescription = "Back")
            Text(
                text = "CALCULATOR",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        TerminalDivider(modifier = Modifier.padding(top = 6.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                color = palette.primary,
                fontSize = 30.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = error ?: result ?: " ",
                color = if (error != null) palette.error else palette.accent,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            )
        }

        val rows = listOf(
            listOf("C", "⌫", "(", ")"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "−"),
            listOf("0", ".", "%", "+")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    CalcKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "C") {
                                expression = ""
                                result = null
                                error = null
                            } else if (key == "⌫") {
                                expression = expression.dropLast(1)
                            } else {
                                if (expression.length < 64) expression += key
                                error = null
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 20.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalcKey(
                label = "=",
                modifier = Modifier.weight(1f),
                accent = true,
                onClick = ::evaluate
            )
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalTerminalPalette.current
    val borderColor = if (accent) palette.primary else palette.border
    Column(
        modifier = modifier
            .height(52.dp)
            .background(
                if (accent) palette.primary.copy(alpha = 0.12f) else palette.surface.copy(alpha = 0.5f),
                RoundedCornerShape(3.dp)
            )
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .semantics { contentDescription = "Calculator key $label" }
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (accent) palette.primary else palette.secondary,
            fontSize = 18.sp,
            fontWeight = if (accent) FontWeight.Bold else FontWeight.Normal
        )
    }
}
