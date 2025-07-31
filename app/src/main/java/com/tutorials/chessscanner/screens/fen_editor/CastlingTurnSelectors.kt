package com.tutorials.chessscanner.screens.fen_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TurnSelector(
    turnState: MutableState<String>,
    onTurnSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        listOf("w" to "White", "b" to "Black").forEach { (code, label) ->
            val selected = turnState.value == code
            Button(
                onClick = {
                    turnState.value = code
                    onTurnSelected(code)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Color(0xFF6f9438) else Color(0xFFDADADA),
                    contentColor = if (selected) Color.White else Color.Black
                )
            ) {
                Text("$label to move")
            }
        }
    }
}

@Composable
fun CastlingRightsSelector(fenState: MutableState<String>) {
    val castlingState = remember(fenState.value) {
        mutableStateOf(parseCastlingRights(fenState.value))
    }

    fun updateFenFromCastling() {
        val parts = fenState.value.split(" ").toMutableList()
        if (parts.size >= 6) {
            parts[2] = castlingState.value.joinToString("").ifEmpty { "-" }
            fenState.value = parts.joinToString(" ")
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Castling Rights", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "White" to listOf("K" to "King-side", "Q" to "Queen-side"),
                "Black" to listOf("k" to "King-side", "q" to "Queen-side")
            ).forEach { (label, options) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label, modifier = Modifier.width(60.dp))

                    options.forEach { (flag, desc) ->
                        val isSelected = castlingState.value.contains(flag)
                        Button(
                            onClick = {
                                castlingState.value = castlingState.value.toMutableSet().apply {
                                    if (isSelected) remove(flag) else add(flag)
                                }
                                updateFenFromCastling()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF6f9438) else Color(0xFFDADADA),
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(flag)
                        }
                    }
                }
            }
        }
    }
}
