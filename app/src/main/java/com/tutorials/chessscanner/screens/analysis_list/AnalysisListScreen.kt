package com.tutorials.chessscanner.screens.analysis_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tutorials.chessscanner.util.AnalysisData
import com.tutorials.chessscanner.util.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisListScreen(navController: NavController) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var analyses by remember { mutableStateOf<List<AnalysisData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf<AnalysisData?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            analyses = repository.getAnalyses()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Saved Analyses") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                analyses.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No saved analyses yet.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(analyses) { analysis ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier
                                    .padding(16.dp)
                                    .clickable {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("analysis", analysis)
                                        navController.navigate("chessboard")
                                    }) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            analysis.title.ifEmpty { "Untitled Analysis" },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        IconButton(onClick = { showDialog = analysis }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Analysis",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Moves: ${analysis.sanMoves.size}", style = MaterialTheme.typography.bodySmall)
                                    if (analysis.sanMoves.isNotEmpty()) {
                                        val movesText = analysis.sanMoves.joinToString(" ").take(50)
                                        Text("$movesText...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    if (showDialog != null) {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            title = { Text("Delete Analysis") },
                            text = { Text("Are you sure you want to delete this analysis?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.deleteAnalysis(showDialog!!)
                                        analyses = repository.getAnalyses() // Refresh list
                                        showDialog = null
                                    }
                                }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                }
            }
        }
    }
}
