package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiExtractedDefinition(
    val term: String,
    val definition: String,
    val matchedSnippet: String
)

data class AiPageExtractionResult(
    val suggestedTitle: String,
    val sourceType: String,
    val extractedText: String,
    val definitions: List<AiExtractedDefinition>
)

class EduFlowAiService {

    companion object {
        private const val TAG = "EduFlowAiService"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val TIMEOUT_MS = 60_000L
    }

    private val pageExtractionSchema = Schema.obj(
        properties = mapOf(
            "suggestedTitle" to Schema.string(
                description = "A concise 2-6 word study title summarizing the page"
            ),
            "sourceType" to Schema.enumeration(
                values = listOf("printed", "handwritten"),
                description = "Whether the page is mainly printed text or clean handwriting"
            ),
            "extractedText" to Schema.string(
                description = "Clean, accurate editable transcription of all text from the page, preserving paragraphs and bullet lists"
            ),
            "keyDefinitions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string(description = "The key concept, vocabulary word, or topic"),
                        "definition" to Schema.string(description = "The clear, student-friendly definition or explanation"),
                        "matchedSnippet" to Schema.string(description = "Exact sentence or phrase from extractedText that contains this definition")
                    )
                ),
                description = "Key definitions, formulas, or important facts found on the page"
            )
        )
    )

    private val definitionsOnlySchema = Schema.obj(
        properties = mapOf(
            "keyDefinitions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string(description = "The key concept or term"),
                        "definition" to Schema.string(description = "Clear explanation or definition"),
                        "matchedSnippet" to Schema.string(description = "Exact sentence or phrase from the input text")
                    )
                ),
                description = "Key definitions extracted from the student note text"
            )
        )
    )

    private val recapSchema = Schema.obj(
        properties = mapOf(
            "recapPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concise, encouraging study bullet points reviewing the subject"
            )
        )
    )

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pageExtractionSchema
            }
        )
    }

    private val definitionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = definitionsOnlySchema
            }
        )
    }

    private val recapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recapSchema
            }
        )
    }

    suspend fun extractNoteFromBitmap(bitmap: Bitmap): Result<AiPageExtractionResult> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                extractionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "You are helping a young student organize class notes. " +
                                "Transcribe all readable text accurately from this photo of a printed page or clean handwriting into clean, editable text. " +
                                "Also identify the most important key definitions, vocabulary terms, or key facts so the student can highlight them. " +
                                "Make sure matchedSnippet is an exact substring from extractedText whenever possible."
                        )
                    }
                )
            }

            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(jsonText)
            val title = json.optString("suggestedTitle", "Scanned Study Note").ifBlank { "Scanned Study Note" }
            val sourceType = json.optString("sourceType", "printed").let {
                if (it == "handwritten") "handwritten" else "printed"
            }
            val extractedText = json.optString("extractedText", "").trim()
            val defsArray = json.optJSONArray("keyDefinitions")
            val defs = buildList {
                if (defsArray != null) {
                    for (i in 0 until defsArray.length()) {
                        val item = defsArray.optJSONObject(i) ?: continue
                        val term = item.optString("term", "").trim()
                        val def = item.optString("definition", "").trim()
                        val snippet = item.optString("matchedSnippet", "").trim()
                        if (term.isNotEmpty() && def.isNotEmpty()) {
                            add(AiExtractedDefinition(term = term, definition = def, matchedSnippet = snippet))
                        }
                    }
                }
            }
            if (extractedText.isBlank() && defs.isEmpty()) {
                Result.failure(IllegalStateException("Could not read clear text from this photo. Try a brighter or closer shot."))
            } else {
                Result.success(
                    AiPageExtractionResult(
                        suggestedTitle = title,
                        sourceType = sourceType,
                        extractedText = extractedText,
                        definitions = defs
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Photo extraction timed out", e)
            Result.failure(Exception("Reading the page timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI photo extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to extract text from photo."))
        }
    }

    suspend fun findDefinitionsInText(noteText: String): Result<List<AiExtractedDefinition>> {
        if (noteText.isBlank()) {
            return Result.failure(IllegalArgumentException("Add or extract some note text first."))
        }
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                definitionsModel.generateContent(
                    "Extract the key definitions and important study terms from these student notes. " +
                        "For matchedSnippet, copy the exact sentence or phrase from the notes:\n\n$noteText"
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(jsonText)
            val defsArray = json.optJSONArray("keyDefinitions")
            val defs = buildList {
                if (defsArray != null) {
                    for (i in 0 until defsArray.length()) {
                        val item = defsArray.optJSONObject(i) ?: continue
                        val term = item.optString("term", "").trim()
                        val def = item.optString("definition", "").trim()
                        val snippet = item.optString("matchedSnippet", "").trim()
                        if (term.isNotEmpty() && def.isNotEmpty()) {
                            add(AiExtractedDefinition(term = term, definition = def, matchedSnippet = snippet))
                        }
                    }
                }
            }
            Result.success(defs)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Definition detection timed out", e)
            Result.failure(Exception("Finding definitions timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI definition detection failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not detect key definitions."))
        }
    }

    suspend fun generateSubjectRecap(
        subjectName: String,
        definitionsSummary: String
    ): Result<List<String>> {
        if (definitionsSummary.isBlank()) {
            return Result.failure(IllegalArgumentException("Highlight a few definitions in $subjectName first!"))
        }
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                recapModel.generateContent(
                    "Create 3 to 5 cheerful, crystal-clear study recap bullets for a young student reviewing '$subjectName' based on these key definitions:\n\n$definitionsSummary"
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty recap.")
            val json = JSONObject(jsonText)
            val arr = json.optJSONArray("recapPoints")
            val points = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val pt = arr.optString(i, "").trim()
                        if (pt.isNotEmpty()) add(pt)
                    }
                }
            }
            if (points.isEmpty()) {
                Result.failure(IllegalStateException("Could not generate a study recap right now."))
            } else {
                Result.success(points)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Recap generation timed out", e)
            Result.failure(Exception("Recap timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI recap failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not generate study recap."))
        }
    }
}
