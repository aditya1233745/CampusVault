package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ListingDraftAssist
import com.example.model.StudyGuideResult
import com.example.model.StudyQuizQuestion
import com.example.model.VaultListing
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class StudyAiHelper {

    private val draftAssistModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "A concise 1-2 sentence study or item overview."),
                "polishedDescription" to Schema.string(description = "A helpful, student-friendly description for the listing (2-3 sentences)."),
                "tags" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 short tags (subject, semester, or key topic, max 22 chars each)."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val studyGuideModel by lazy {
        val quizQuestionSchema = Schema.obj(
            properties = mapOf(
                "question" to Schema.string(description = "Clear practice multiple-choice question."),
                "options" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 4 answer choices."
                ),
                "correctIndex" to Schema.integer(description = "0-based index (0..3) of the right option."),
                "explanation" to Schema.string(description = "Brief 1-sentence explanation of why that answer is right.")
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Clear 2-3 sentence overview of what these study materials cover."),
                "keyTopics" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 core topics or exam takeaways."
                ),
                "quiz" to Schema.array(
                    items = quizQuestionSchema,
                    description = "Exactly 3 multiple-choice practice questions."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    suspend fun generateListingAssist(
        title: String,
        description: String,
        category: String,
        attachmentName: String?,
        resourceLink: String?,
        previewBitmap: Bitmap?
    ): ListingDraftAssist {
        return try {
            val promptText = buildString {
                appendLine("You help university students write clear, accurate campus marketplace & study-notes listings.")
                appendLine("Category: $category")
                appendLine("Title: $title")
                if (description.isNotBlank()) appendLine("Draft description: $description")
                if (!attachmentName.isNullOrBlank()) appendLine("Attached filename: $attachmentName")
                if (!resourceLink.isNullOrBlank()) appendLine("Shared link: $resourceLink")
                appendLine("Generate a concise summary, a polished 2-3 sentence description, and 3-5 short tags.")
            }

            val response = withTimeout(60_000L) {
                if (previewBitmap != null) {
                    draftAssistModel.generateContent(
                        content {
                            image(previewBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    draftAssistModel.generateContent(promptText)
                }
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(jsonText)
            val tagsArray = json.getJSONArray("tags")
            val tags = (0 until tagsArray.length())
                .map { tagsArray.getString(it).trim().take(24) }
                .filter { it.isNotEmpty() }
                .take(5)

            ListingDraftAssist(
                summary = json.getString("summary").trim().take(400),
                polishedDescription = json.getString("polishedDescription").trim().take(600),
                tags = tags
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI listing assist timed out", e)
            throw IllegalStateException("AI suggestion timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI listing assist failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not generate AI tags and summary.")
        }
    }

    suspend fun generateStudyGuideAndQuiz(
        listing: VaultListing,
        previewBitmap: Bitmap? = null
    ): StudyGuideResult {
        return try {
            val promptText = buildString {
                appendLine("You are a concise university study companion.")
                appendLine("Create a study summary, 3-5 key exam topics, and 3 multiple-choice practice questions (with 4 options each) based on this campus study listing:")
                appendLine("Category: ${listing.category}")
                appendLine("Title: ${listing.title}")
                appendLine("Description: ${listing.description}")
                if (listing.studyTags.isNotEmpty()) {
                    appendLine("Tags: ${listing.studyTags.joinToString(", ")}")
                }
                if (!listing.attachmentName.isNullOrBlank()) {
                    appendLine("File: ${listing.attachmentName}")
                }
            }

            val response = withTimeout(60_000L) {
                if (previewBitmap != null) {
                    studyGuideModel.generateContent(
                        content {
                            image(previewBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    studyGuideModel.generateContent(promptText)
                }
            }

            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(jsonText)
            val topicsJson = json.getJSONArray("keyTopics")
            val keyTopics = (0 until topicsJson.length())
                .map { topicsJson.getString(it).trim() }
                .filter { it.isNotEmpty() }

            val quizJson = json.getJSONArray("quiz")
            val questions = (0 until quizJson.length()).map { idx ->
                val qObj = quizJson.getJSONObject(idx)
                val optsArray = qObj.getJSONArray("options")
                val opts = (0 until optsArray.length()).map { optsArray.getString(it).trim() }
                StudyQuizQuestion(
                    question = qObj.getString("question").trim(),
                    options = opts,
                    correctIndex = qObj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    explanation = qObj.optString("explanation", "").trim()
                )
            }

            StudyGuideResult(
                summary = json.getString("summary").trim(),
                keyTopics = keyTopics,
                quiz = questions
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI study guide timed out", e)
            throw IllegalStateException("AI study guide timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI study guide failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not generate AI study guide.")
        }
    }

    private companion object {
        const val TAG = "StudyAiHelper"
    }
}
