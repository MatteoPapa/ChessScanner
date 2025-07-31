package com.tutorials.chessscanner.screens.fen_editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Piece
import com.tutorials.chessscanner.util.BoardUtils.getPieceImageName

@Composable
fun PieceToolbar(
    selectedPiece: MutableState<Piece?>,
    onPieceSelected: (Piece?) -> Unit
) {
    val context = LocalContext.current
    val tileSize = 40.dp
    val trashSize = 48.dp

    val pieces = listOf(
        Piece.WHITE_KING, Piece.WHITE_QUEEN, Piece.WHITE_ROOK, Piece.WHITE_BISHOP,
        Piece.WHITE_KNIGHT, Piece.WHITE_PAWN,
        Piece.BLACK_KING, Piece.BLACK_QUEEN, Piece.BLACK_ROOK, Piece.BLACK_BISHOP,
        Piece.BLACK_KNIGHT, Piece.BLACK_PAWN
    )

    val firstRow = pieces.take(6)
    val secondRow = pieces.drop(6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color(0xFFF0F0F0))
            .padding(8.dp)
    ) {
        // Delete Button Column (Left)
        Box(
            modifier = Modifier
                .width(trashSize)
                .fillMaxHeight()
                .background(
                    if (selectedPiece.value == null) Color(0xFFEBBBBB) else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
                .border(
                    width = 1.dp,
                    color = if (selectedPiece.value == null) Color.Red else Color.Gray,
                    shape = MaterialTheme.shapes.small
                )
                .clickable { onPieceSelected(null) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }


        Spacer(modifier = Modifier.width(8.dp))

        // Pieces Column (Right)
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                firstRow.forEach { piece ->
                    PieceBox(piece, selectedPiece, onPieceSelected, tileSize, context)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                secondRow.forEach { piece ->
                    PieceBox(piece, selectedPiece, onPieceSelected, tileSize, context)
                }
            }
        }
    }
}

@Composable
private fun PieceBox(
    piece: Piece,
    selectedPiece: MutableState<Piece?>,
    onPieceSelected: (Piece?) -> Unit,
    tileSize: Dp,
    context: android.content.Context
) {
    val isSelected = selectedPiece.value == piece
    val imageName = getPieceImageName(piece)
    val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)

    Box(
        modifier = Modifier
            .size(tileSize)
            .background(
                if (isSelected) Color(0xFFCEE5B1) else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onPieceSelected(piece) },
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = imageName,
                modifier = Modifier.size(tileSize)
            )
        }
    }
}
