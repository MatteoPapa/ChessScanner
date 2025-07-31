package com.tutorials.chessscanner.screens.chessboard

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.tutorials.chessscanner.util.BoardUtils.boardIndexToSquare
import com.tutorials.chessscanner.util.BoardUtils.getPieceImageName
import com.tutorials.chessscanner.util.BoardUtils.parseBoardToMatrix
import com.tutorials.chessscanner.util.fetchStockfishEval
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import com.github.bhlangonijr.chesslib.Side
import com.tutorials.chessscanner.util.AnalysisData
import com.tutorials.chessscanner.util.FirebaseRepository
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

@Composable
fun ChessBoardScreen(analysis: AnalysisData) {
    val board = remember { Board() }
    val boardState = remember { mutableStateOf(board) }
    val moveHistory = remember { mutableStateListOf<Move>() }
    val currentMoveIndex = remember { mutableIntStateOf(0) }

    val evalState = remember { mutableStateOf<Double?>(null) }
    val mateState = remember { mutableStateOf<Int?>(null) }
    val continuationState = remember { mutableStateOf<String?>(null) }
    val evalCache = remember { mutableStateMapOf<String, Triple<Double?, Int?, String?>>() }

    //Firebase DB
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var analysisTitle by remember { mutableStateOf(analysis.title) }

    val arrowMove = remember(continuationState.value) {
        continuationState.value
            ?.split(" ")
            ?.firstOrNull()
            ?.takeIf { it.length >= 4 }
            ?.let { move ->
                val from = Square.valueOf(move.substring(0, 2).uppercase())
                val to = Square.valueOf(move.substring(2, 4).uppercase())
                from to to
            }
    }

    LaunchedEffect(analysis) {
        try {
            board.loadFromFen(analysis.fen)
            boardState.value = board
            moveHistory.clear()
            currentMoveIndex.intValue = 0

            val tempBoard = Board()
            tempBoard.loadFromFen(analysis.fen)

            analysis.sanMoves.forEach { san ->
                val legalMoves = tempBoard.legalMoves()
                val move = legalMoves.find { getSanLikeMove(tempBoard, it) == san }
                if (move != null) {
                    tempBoard.doMove(move)
                    board.doMove(move)
                    moveHistory.add(move)
                    currentMoveIndex.value += 1
                }
            }

            boardState.value = board
            requestEval(board.fen, evalState, mateState, evalCache, continuationState)
        } catch (_: Exception) {}
    }


    val tileSize = 48.dp
    val context = LocalContext.current

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var lastShakeTime by remember { mutableStateOf(0L) }
    var showResetDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val shakeThreshold = 12.0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val acceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

                    if (acceleration > shakeThreshold && now - lastShakeTime > 1000) {
                        lastShakeTime = now
                        showResetDialog = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }


    val selectedSquare = remember { mutableStateOf<Square?>(null) }
    val boardMatrix = parseBoardToMatrix(boardState.value)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Column(modifier = Modifier.matchParentSize()) {
                    for (row in 0..7) {
                        Row(Modifier.weight(1f)) {
                            for (col in 0..7) {
                                val isLight = (row + col) % 2 == 0
                                val square = boardIndexToSquare(row, col)
                                val piece = boardMatrix[row][col]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            when {
                                                selectedSquare.value == square -> Color.Yellow
                                                isLight -> Color(0xFFEEEED2)
                                                else -> Color(0xFF769656)
                                            }
                                        )
                                        .clickable {
                                            handleTap(
                                                square = square,
                                                selected = selectedSquare,
                                                boardState = boardState,
                                                moveHistory = moveHistory,
                                                currentMoveIndex = currentMoveIndex,
                                                evalState = evalState,
                                                mateState = mateState,
                                                evalCache = evalCache,
                                                continuationState = continuationState
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (piece != null && piece != Piece.NONE) {
                                        val imageName = getPieceImageName(piece)
                                        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                                        if (resId != 0) {
                                            Image(
                                                painter = painterResource(id = resId),
                                                contentDescription = imageName,
                                                modifier = Modifier.size(tileSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ARROW CANVAS OVERLAY
                if (arrowMove != null) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val (from, to) = arrowMove

                        val boardWidth = size.width
                        val boardHeight = size.height

                        val fromOffset = squareToOffset(from, boardWidth, boardHeight)
                        val toOffset = squareToOffset(to, boardWidth, boardHeight)
                        val arrowColor = Color.Red.copy(alpha = 0.6f) // 60% opacity


                        // Calculate direction
                        val angle = atan2(toOffset.y - fromOffset.y, toOffset.x - fromOffset.x)

                        val shaftBackOffset = size.minDimension / 23f // adjust as needed
                        val adjustedToOffset = Offset(
                            (toOffset.x - shaftBackOffset * cos(angle)).toFloat(),
                            (toOffset.y - shaftBackOffset * sin(angle)).toFloat()
                        )

                        drawLine(
                            color = arrowColor,
                            start = fromOffset,
                            end = adjustedToOffset,
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )

                        // Arrowhead dimensions (scaled based on tile size)
                        val arrowHeadLength = size.minDimension / 14f  // previously /10f

                        // Points of triangle
                        // Shift arrowhead base slightly *backward* along the line
                        val headBackOffset = -(size.minDimension / 40f)
                        val endX = toOffset.x - headBackOffset * cos(angle)
                        val endY = toOffset.y - headBackOffset * sin(angle)


                        val angle1 = angle - Math.toRadians(25.0)
                        val angle2 = angle + Math.toRadians(25.0)

                        val x1 = endX - arrowHeadLength * cos(angle1).toFloat()
                        val y1 = endY - arrowHeadLength * sin(angle1).toFloat()
                        val x2 = endX - arrowHeadLength * cos(angle2).toFloat()
                        val y2 = endY - arrowHeadLength * sin(angle2).toFloat()

                        val arrowPath = Path().apply {
                            moveTo(endX, endY)
                            lineTo(x1, y1)
                            lineTo(x2, y2)
                            close()
                        }

                        // Draw arrowhead
                        drawPath(path = arrowPath, color = arrowColor)

                    }

                }
            }

            // Evaluation bar
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).padding(vertical = 4.dp)) {
                val eval = evalState.value ?: 0.0
                val mate = mateState.value

                val targetScore = when {
                    mate != null -> if (mate > 0) 1.0 else 0.0
                    else -> (eval + 10) / 20.0
                }.coerceIn(0.0, 1.0)

                val animatedScore by animateFloatAsState(
                    targetValue = targetScore.toFloat(),
                    label = "evalBarAnimation"
                )
                val safeWhiteWeight = animatedScore.coerceIn(0.01f, 0.99f)
                val safeBlackWeight = (1f - safeWhiteWeight).coerceIn(0.01f, 0.99f)

                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(safeWhiteWeight).fillMaxHeight().background(Color.White), contentAlignment = Alignment.Center) {
                        if (mate != null && mate > 0 || eval >= 0) {
                            Text(text = mate?.let { "Mate in ${kotlin.math.abs(it)}" } ?: "+%.1f".format(eval), fontSize = 12.sp, color = Color.Black)
                        }
                    }
                    Box(Modifier.weight(safeBlackWeight).fillMaxHeight().background(Color.Black), contentAlignment = Alignment.Center) {
                        if (mate != null && mate < 0 || eval < 0) {
                            Text(text = mate?.let { "Mate in ${kotlin.math.abs(it)}" } ?: "%.1f".format(eval), fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentMoveIndex.intValue > 0) {
                            board.undoMove()
                            currentMoveIndex.value -= 1
                            requestEval(board.fen, evalState, mateState, evalCache, continuationState)
                        }
                    },
                    enabled = currentMoveIndex.intValue > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentMoveIndex.intValue > 0) Color(0xFF6f9438) else Color(0xFFCCCCCC))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Button(
                    onClick = {
                        if (currentMoveIndex.intValue < moveHistory.size) {
                            val move = moveHistory[currentMoveIndex.intValue]
                            board.doMove(move)
                            currentMoveIndex.value += 1
                            requestEval(board.fen, evalState, mateState, evalCache, continuationState)
                        }
                    },
                    enabled = currentMoveIndex.intValue < moveHistory.size,
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentMoveIndex.intValue < moveHistory.size) Color(0xFF6f9438) else Color(0xFFCCCCCC))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White)
                }
            }

            // SAN Move List
            val sanMoves = run {
                val tempBoard = Board()
                tempBoard.loadFromFen(analysis.fen)
                val moves = mutableListOf<String>()
                for (move in moveHistory) {
                    moves.add(getSanLikeMove(tempBoard, move))
                    tempBoard.doMove(move)
                }
                moves
            }

            val scrollState = rememberScrollState()
            LaunchedEffect(sanMoves.size) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            continuationState.value?.let { continuation ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .height(48.dp), // Fixed height
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECECEC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = continuationState.value?.let {
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Best line: ")
                                    }
                                    append(it)
                                }
                            } ?: buildAnnotatedString { append("Best line:") }, // fallback placeholder
                            fontSize = 13.sp,
                            color = Color(0xFF444444),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp, vertical = 4.dp).background(Color(0xFFF5F5F5))
            ) {
                Row(Modifier.horizontalScroll(scrollState).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    if (sanMoves.isEmpty()) {
                        Text("No moves yet", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    } else {
                        sanMoves.chunked(2).forEachIndexed { index, pair ->
                            Row(Modifier.padding(end = 12.dp)) {
                                Text("${index + 1}.", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                                Text(pair.getOrNull(0) ?: "", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                                Text(pair.getOrNull(1) ?: "", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { isDialogOpen = true },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
            ) {
                Text("Save Analysis", color = Color.White)
            }

            if (isDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isDialogOpen = false },
                    title = { Text("Save Analysis") },
                    text = {
                        Column {
                            Text("Enter a title for your analysis:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = analysisTitle,
                                onValueChange = { analysisTitle = it },
                                placeholder = { Text("e.g., King's Indian vs Sicilian") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDialogOpen = false
                                scope.launch {
                                    val sanMoves = run {
                                        val tempBoard = Board()
                                        tempBoard.loadFromFen(analysis.fen)
                                        val moves = mutableListOf<String>()
                                        for (move in moveHistory) {
                                            moves.add(getSanLikeMove(tempBoard, move))
                                            tempBoard.doMove(move)
                                        }
                                        moves
                                    }

                                    // Save to Firebase
                                    repository.saveAnalysis(
                                        fen = analysis.fen,
                                        sanMoves = sanMoves,
                                        bestLine = continuationState.value,
                                        title = analysisTitle.trim(),
                                        id = analysis.id // ← will update if it exists
                                    )

                                    saveMessage = "Analysis saved!"
                                    analysisTitle = ""
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            analysisTitle = analysis.title
                            isDialogOpen = true
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Restart Analysis?") },
                    text = { Text("Do you want to reset the board to the original position? This will delete every saved move until now.") },
                    confirmButton = {
                        TextButton(onClick = {
                            // Reset state
                            board.loadFromFen(analysis.fen)
                            boardState.value = Board().apply { loadFromFen(analysis.fen) }
                            moveHistory.clear()
                            currentMoveIndex.value = 0
                            evalState.value = null
                            mateState.value = null
                            continuationState.value = null
                            requestEval(boardState.value.fen, evalState, mateState, evalCache, continuationState)
                            showResetDialog = false
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            val localContext = LocalContext.current

            LaunchedEffect(saveMessage) {
                saveMessage?.let {
                    Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show()
                    saveMessage = null
                }
            }

        }
    }
}



