package com.tutorials.chessscanner.screens.fen_editor

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.tutorials.chessscanner.util.AnalysisData
import com.tutorials.chessscanner.util.BoardUtils.boardIndexToSquare
import com.tutorials.chessscanner.util.BoardUtils.getPieceImageName
import com.tutorials.chessscanner.util.BoardUtils.parseBoardToMatrix

@Composable
fun FenEditorScreen(initialFen: String = Board().fen, navController: NavController) {
    val context = LocalContext.current

    val fenState = remember { mutableStateOf(initialFen) }
    LaunchedEffect(fenState.value) {
        Log.d("FenEditor", "Current FEN: ${fenState.value}")
    }

    val selectedPiece = remember { mutableStateOf<Piece?>(null) }

    val board = remember(fenState.value) {
        Board().apply { loadFromFen(fenState.value) }
    }

    val boardMatrix = parseBoardToMatrix(board)
    val turnState = remember { mutableStateOf("w") }

    LaunchedEffect(fenState.value) {
        val parts = fenState.value.split(" ")
        if (parts.size >= 2) {
            turnState.value = parts[1]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    for (row in 0..7) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (col in 0..7) {
                                val isLight = (row + col) % 2 == 0
                                val square = boardIndexToSquare(row, col)
                                val piece = boardMatrix[row][col]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(if (isLight) Color(0xFFEEEED2) else Color(0xFF769656))
                                        .clickable {
                                            onEditorSquareTap(
                                                square = square,
                                                selectedPiece = selectedPiece,
                                                fenState = fenState
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    piece?.takeIf { it != Piece.NONE }?.let {
                                        val imageName = getPieceImageName(it)
                                        val resId = context.resources.getIdentifier(
                                            imageName, "drawable", context.packageName
                                        )
                                        if (resId != 0) {
                                            Image(
                                                painter = painterResource(id = resId),
                                                contentDescription = imageName,
                                                modifier = Modifier.fillMaxSize(0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PieceToolbar(
                selectedPiece = selectedPiece,
                onPieceSelected = { selectedPiece.value = it }
            )

            TurnSelector(
                turnState = turnState,
                onTurnSelected = { turn ->
                    val parts = fenState.value.split(" ").toMutableList()
                    if (parts.size >= 2) {
                        parts[1] = turn
                        fenState.value = parts.joinToString(" ")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            CastlingRightsSelector(fenState)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isLegalFen(fenState.value)) {
                        val analysis = AnalysisData(
                            fen = fenState.value,
                            sanMoves = emptyList(),
                            bestLine = null,
                            title = ""
                        )

                        navController.currentBackStackEntry?.savedStateHandle?.set("analysis", analysis)
                        navController.navigate("chessboard")

                    } else {
                        Toast.makeText(context, "Illegal or invalid FEN", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6f9438)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Analysis")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Start")
            }
        }
    }
}