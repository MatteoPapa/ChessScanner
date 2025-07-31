package com.tutorials.chessscanner.util

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square

object BoardUtils {

    fun parseBoardToMatrix(board: Board): List<List<Piece?>> {
        val matrix = MutableList(8) { MutableList<Piece?>(8) { Piece.NONE } }
        for (square in Square.values()) {
            if (square.ordinal >= 64) continue // skip invalids
            val piece = board.getPiece(square)
            val row = 8 - square.rank.ordinal - 1
            val col = square.file.ordinal
            matrix[row][col] = piece
        }
        return matrix
    }

    fun boardIndexToSquare(row: Int, col: Int): Square {
        val file = 'A' + col
        val rank = 8 - row
        return Square.valueOf("$file$rank")
    }

    fun getPieceImageName(piece: Piece): String {
        return when (piece) {
            Piece.WHITE_PAWN   -> "wp"
            Piece.WHITE_ROOK   -> "wr"
            Piece.WHITE_KNIGHT -> "wn"
            Piece.WHITE_BISHOP -> "wb"
            Piece.WHITE_QUEEN  -> "wq"
            Piece.WHITE_KING   -> "wk"
            Piece.BLACK_PAWN   -> "bp"
            Piece.BLACK_ROOK   -> "br"
            Piece.BLACK_KNIGHT -> "bn"
            Piece.BLACK_BISHOP -> "bb"
            Piece.BLACK_QUEEN  -> "bq"
            Piece.BLACK_KING   -> "bk"
            else -> ""
        }
    }
}
