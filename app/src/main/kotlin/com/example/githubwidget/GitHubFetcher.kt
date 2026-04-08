package com.example.githubwidget

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

const val TAG_FETCHER = "GitHubFetcher"

private val jsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object GitHubFetcher {
    private const val TAG = TAG_FETCHER

    @Serializable
    data class GraphQLRequest(val query: String)

    @Serializable
    data class GithubResponse(
        val data: DataBlock? = null,
        val errors: List<GraphQLError>? = null,
        val message: String? = null  // For API error responses
    )

    @Serializable
    data class GraphQLError(
        val message: String
    )

    @Serializable
    data class DataBlock(val user: UserBlock? = null)

    @Serializable
    data class UserBlock(val contributionsCollection: ContributionsCollection? = null)

    @Serializable
    data class ContributionsCollection(val contributionCalendar: ContributionCalendar? = null)

    @Serializable
    data class ContributionCalendar(val weeks: List<Week> = emptyList())

    @Serializable
    data class Week(val contributionDays: List<ContributionDay> = emptyList())

    @Serializable
    data class ContributionDay(val contributionCount: Int, val date: String)

    suspend fun fetchContributions(username: String): List<Int>? {
        Log.d(TAG, "Starting fetch for username: $username")
        
        if (username.isBlank()) {
            Log.w(TAG, "Username is blank")
            return null
        }

        val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val query = """
            query {
              user(login: "$username") {
                contributionsCollection {
                  contributionCalendar {
                    weeks {
                      contributionDays {
                        contributionCount
                        date
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return try {
            Log.d(TAG, "Sending GraphQL query to GitHub API")
            
            val httpResponse = client.post("https://api.github.com/graphql") {
                headers {
                    append("User-Agent", "GitHubWidget/1.0")
                    append("Accept", "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(GraphQLRequest(query))
            }
            
            val responseBody = httpResponse.bodyAsText()
            Log.d(TAG, "Raw response body: $responseBody")
            
            val response: GithubResponse = jsonParser.decodeFromString(responseBody)
            Log.d(TAG, "Parsed response: $response")
            Log.d(TAG, "Got response from GitHub API")
            
            // Check for API errors (rate limit, etc)
            if (response.message != null) {
                Log.e(TAG, "API Error: ${response.message}")
                return null
            }
            
            // Check for GraphQL errors
            if (!response.errors.isNullOrEmpty()) {
                Log.e(TAG, "GraphQL Errors: ${response.errors}")
                response.errors.forEach { error ->
                    Log.e(TAG, "Error: ${error.message}")
                }
                return null
            }

            // Check for null user
            if (response.data == null) {
                Log.e(TAG, "Response data is null - response was: $response")
                return null
            }

            if (response.data.user == null) {
                Log.e(TAG, "User '$username' not found or API blocked request")
                return null
            }

            val weeks = response.data.user.contributionsCollection?.contributionCalendar?.weeks ?: emptyList()
            
            if (weeks.isEmpty()) {
                Log.w(TAG, "No weeks data returned for user: $username")
                return null
            }

            Log.d(TAG, "Successfully fetched ${weeks.size} weeks of data")
            
            val result = weeks.flatMapIndexed { index, week ->
                val days = week.contributionDays.map { it.contributionCount }
                if (days.size < 7) {
                    if (index == 0) {
                        List(7 - days.size) { 0 } + days
                    } else {
                        days + List(7 - days.size) { 0 }
                    }
                } else {
                    days
                }
            }
            
            Log.d(TAG, "Returning ${result.size} total contribution counts")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetch: ${e.message}", e)
            e.printStackTrace()
            null
        } finally {
            client.close()
        }
    }
}
