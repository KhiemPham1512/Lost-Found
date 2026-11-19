package com.lostfound.app.data.remote

import org.json.JSONObject
import org.json.JSONArray

data class VerificationQuestion(
    val id: String,
    val position: Int,
    val questionText: String
)

data class QuestionsResponse(
    val postId: String,
    val questions: List<VerificationQuestion>,
    val attemptsLeft: Int
)

data class ClaimCreatedResponse(
    val claimId: String,
    val status: String
)

data class ClaimAnswer(
    val questionId: String,
    val answerText: String
)

class VerificationRepository(
    private val apiClient: ApiClient = ApiClient()
) {

    suspend fun getQuestions(
        postId: String
    ): Result<QuestionsResponse> {
        val result = apiClient.get("/posts/$postId/questions")

        return result.fold(
            onSuccess = { response ->
                try {
                    if (!response.isSuccessful) {
                        return@fold Result.failure(
                            Exception(extractError(response.body))
                        )
                    }

                    val json = JSONObject(response.body)
                    val questionsJson = json.getJSONArray("questions")

                    val questions = buildList {
                        for (i in 0 until questionsJson.length()) {
                            val item = questionsJson.getJSONObject(i)

                            add(
                                VerificationQuestion(
                                    id = item.getString("id"),
                                    position = item.getInt("position"),
                                    questionText = item.getString("question_text")
                                )
                            )
                        }
                    }

                    Result.success(
                        QuestionsResponse(
                            postId = json.getString("post_id"),
                            questions = questions,
                            attemptsLeft = json.getInt("attempts_left")
                        )
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun submitClaim(
        postId: String,
        answers: List<ClaimAnswer>
    ): Result<ClaimCreatedResponse> {
        val answersJson = JSONArray()

        answers.forEach { answer ->
            answersJson.put(
                JSONObject().apply {
                    put("question_id", answer.questionId)
                    put("answer_text", answer.answerText)
                }
            )
        }

        val requestBody = JSONObject().apply {
            put("answers", answersJson)
        }.toString()

        val result = apiClient.post(
            path = "/posts/$postId/claims",
            jsonBody = requestBody
        )

        return result.fold(
            onSuccess = { response ->
                try {
                    if (!response.isSuccessful) {
                        return@fold Result.failure(
                            Exception(extractError(response.body))
                        )
                    }

                    val json = JSONObject(response.body)

                    Result.success(
                        ClaimCreatedResponse(
                            claimId = json.getString("claim_id"),
                            status = json.getString("status")
                        )
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private fun extractError(body: String): String {
        return try {
            val json = JSONObject(body)
            json.optString(
                "detail",
                json.optString("message", "API request thất bại")
            )
        } catch (_: Exception) {
            "API request thất bại: $body"
        }
    }
}
