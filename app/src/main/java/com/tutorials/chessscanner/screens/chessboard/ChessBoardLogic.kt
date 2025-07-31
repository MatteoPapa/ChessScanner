package com.tutorials.chessscanner.screens.chessboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.tutorials.chessscanner.util.fetchStockfishEval

fun handleTap(
    square: Square,
    selected: MutableState<Square?>,
    boardState: MutableState<Board>,
    moveHistory: SnapshotStateList<Move>,
    currentMoveIndex: MutableState<Int>,
    evalState: MutableState<Double?>,
    mateState: MutableState<Int?>,
    evalCache: MutableMap<String, Triple<Double?, Int?, String?>>,
    continuationState: MutableState<String?>
) {
    val board = boardState.value
    if (selected.value == null) {
        val piece = board.getPiece(square)
        if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
            selected.value = square
        }
    } else {
        val from = selected.value!!
        val piece = board.getPiece(from)

        val isPromotion = piece.pieceType == PieceType.PAWN && (
                (piece.pieceSide == Side.WHITE && square.rank.ordinal == 7) ||
                        (piece.pieceSide == Side.BLACK && square.rank.ordinal == 0)
                )

        val move = if (isPromotion) {
            val promotionPiece = if (piece.pieceSide == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
            Move(from, square, promotionPiece)
        } else {
            Move(from, square)
        }

        if (board.legalMoves().contains(move)) {
            board.doMove(move)
            if (currentMoveIndex.value < moveHistory.size) {
                moveHistory.removeRange(currentMoveIndex.value, moveHistory.size)
            }
            moveHistory.add(move)
            currentMoveIndex.value += 1
            requestEval(board.fen, evalState, mateState, evalCache, continuationState)
        }
        selected.value = null
    }
}

fun requestEval(
    fen: String,
    evalState: MutableState<Double?>,
    mateState: MutableState<Int?>,
    cache: MutableMap<String, Triple<Double?, Int?, String?>>,
    continuationState: MutableState<String?>
) {
    val cached = cache[fen]
    if (cached != null) {
        evalState.value = cached.first
        mateState.value = cached.second
        continuationState.value = cached.third
    } else {
        fetchStockfishEval(fen) { eval, mate, continuation ->
            evalState.value = eval
            mateState.value = mate
            continuationState.value = continuation
            cache[fen] = Triple(eval, mate, continuation)
        }
    }
}

fun getSanLikeMove(board: Board, move: Move): String {
    val piece = board.getPiece(move.from)
    val isCapture = board.getPiece(move.to) != Piece.NONE
    val pieceChar = when (piece.pieceType) {
        PieceType.KING -> "K"
        PieceType.QUEEN -> "Q"
        PieceType.ROOK -> "R"
        PieceType.BISHOP -> "B"
        PieceType.KNIGHT -> "N"
        PieceType.PAWN -> ""
        else -> ""
    }
    val fromSquare = move.from.toString().lowercase()
    val toSquare = move.to.toString().lowercase()
    return when {
        piece.pieceType == PieceType.PAWN && isCapture -> "${fromSquare[0]}x$toSquare"
        piece.pieceType == PieceType.PAWN -> toSquare
        isCapture -> "${pieceChar}x$toSquare"
        else -> "$pieceChar$toSquare"
    }
}

fun squareToOffset(square: Square, width: Float, height: Float): Offset {
    val file = square.file.ordinal
    val rank = 7 - square.rank.ordinal

    val tileWidth = width / 8
    val tileHeight = height / 8

    val x = file * tileWidth + tileWidth / 2
    val y = rank * tileHeight + tileHeight / 2

    return Offset(x, y)
}