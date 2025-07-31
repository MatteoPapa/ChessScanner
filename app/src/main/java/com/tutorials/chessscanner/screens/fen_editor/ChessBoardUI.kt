package com.tutorials.chessscanner.screens.fen_editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.github.bhlangonijr.chesslib.Piece
import com.tutorials.chessscanner.util.BoardUtils.boardIndexToSquare
import com.tutorials.chessscanner.util.BoardUtils.getPieceImageName

@Composable
fun ChessBoardUI(
    boardMatrix: Array<Array<Piece>>,
    selectedPiece: MutableState<Piece?>,
    fenState: MutableState<String>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val isLight = (row + col) % 2 == 0
                    val square = boardIndexToSquare(row, col)
                    val piece = boardMatrix[row][col]
                    val color = if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(color)
                            .clickable {
                                onEditorSquareTap(square, selectedPiece, fenState)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.takeIf { it != Piece.NONE }?.let {
                            val imageName = getPieceImageName(it)
                            val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
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
