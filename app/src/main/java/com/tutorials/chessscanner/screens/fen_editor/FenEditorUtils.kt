package com.tutorials.chessscanner.screens.fen_editor

import android.util.Log
import androidx.compose.runtime.MutableState
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square

fun parseCastlingRights(fen: String): Set<String> {
    return try {
        val parts = fen.split(" ")
        if (parts.size >= 3 && parts[2] != "-") {
            parts[2].map { it.toString() }.toSet()
        } else {
            emptySet()
        }
    } catch (e: Exception) {
        emptySet()
    }
}

fun onEditorSquareTap(
    square: Square,
    selectedPiece: MutableState<Piece?>,
    fenState: MutableState<String>
) {
    val board = Board().apply { loadFromFen(fenState.value) }
    val selected = selectedPiece.value
    val pieceOnSquare = board.getPiece(square)

    when {
        selected == null -> {
            // Trash selected → remove piece if any
            if (pieceOnSquare != Piece.NONE) {
                board.unsetPiece(pieceOnSquare, square)
            }
        }

        selected == pieceOnSquare -> {
            // Clicking same piece → remove it
            board.unsetPiece(pieceOnSquare, square)
        }

        else -> {
            // Replace whatever is there
            if (pieceOnSquare != Piece.NONE) {
                board.unsetPiece(pieceOnSquare, square)
            }
            board.setPiece(selected, square)
        }
    }

    fenState.value = board.fen
}

fun isLegalFen(fen: String): Boolean {
    return try {
        val board = Board()
        board.loadFromFen(fen)

        var whiteKings = 0
        var blackKings = 0
        var whitePawns = 0
        var blackPawns = 0

        for (square in Square.values()) {
            when (val piece = board.getPiece(square)) {
                Piece.WHITE_KING -> whiteKings++
                Piece.BLACK_KING -> blackKings++
                Piece.WHITE_PAWN -> {
                    whitePawns++
                    if (square.rank.ordinal == 0 || square.rank.ordinal == 7) return false // white pawn on 1st or 8th rank
                }
                Piece.BLACK_PAWN -> {
                    blackPawns++
                    if (square.rank.ordinal == 0 || square.rank.ordinal == 7) return false // black pawn on 1st or 8th rank
                }
                else -> {}
            }
        }

        if (whiteKings != 1 || blackKings != 1){
            Log.e("Error","Too Many Kings")
            return false
        }
        if (whitePawns > 8 || blackPawns > 8){
            Log.e("Error","Too Many Pawns")
            return false
        }

        // Check if side not to move is delivering check (invalid)
        // Save current side
        val originalSide = board.sideToMove

        // Flip side to move to check if they're delivering check
        val flippedSide = if (originalSide == com.github.bhlangonijr.chesslib.Side.WHITE)
            com.github.bhlangonijr.chesslib.Side.BLACK
        else
            com.github.bhlangonijr.chesslib.Side.WHITE

        board.sideToMove = flippedSide
        val illegalCheck = board.isKingAttacked()

        // Restore original side
        board.sideToMove = originalSide

        if (illegalCheck){
            Log.e("Error","Illegal Check")
            return false
        }


        // Final check: legal moves exist, or it's mate/stalemate
        val legalMoves = board.legalMoves()
        return legalMoves.isNotEmpty() || board.isMated || board.isStaleMate
    } catch (e: Exception) {
        Log.e("Error", "Exception: $e")
        false
    }
}