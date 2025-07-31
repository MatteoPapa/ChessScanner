package com.tutorials.chessscanner.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnalysisData(
    val fen: String = "",
    val sanMoves: List<String> = emptyList(),
    val bestLine: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    var id: String = "" // Document ID (optional, injected manually)
): Parcelable

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun saveAnalysis(fen: String, sanMoves: List<String>, bestLine: String?, title: String, id: String? = null) {
        val uid = getUserId() ?: return
        val data = mapOf(
            "fen" to fen,
            "sanMoves" to sanMoves,
            "bestLine" to bestLine,
            "title" to title,
            "timestamp" to System.currentTimeMillis()
        )

        val collectionRef = firestore
            .collection("users")
            .document(uid)
            .collection("analyses")

        if (id.isNullOrBlank()) {
            collectionRef.add(data).await() // CREATE new
        } else {
            collectionRef.document(id).set(data).await() // UPDATE existing
        }
    }


    suspend fun getAnalyses(): List<AnalysisData> {
        val uid = getUserId() ?: return emptyList()
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("analyses")
            .orderBy("timestamp")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(AnalysisData::class.java)?.apply {
                id = doc.id
            }
        }
    }

    suspend fun deleteAnalysis(analysis: AnalysisData) {
        val uid = getUserId() ?: return
        if (analysis.id.isBlank()) return

        firestore.collection("users")
            .document(uid)
            .collection("analyses")
            .document(analysis.id) // ← use correct document ID
            .delete()
            .await()
    }

}
