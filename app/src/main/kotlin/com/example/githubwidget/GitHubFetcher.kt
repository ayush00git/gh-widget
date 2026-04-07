package com.example.githubwidget

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object GitHubFetcher {

    @Serializable
    data class GraphQLRequest(val query: String)

    @Serializable
    data class GithubResponse(val data: DataBlock? = null)

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
        if (username.isBlank()) {
            return null
        }

        val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
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
            val response: GithubResponse = client.post("https://api.github.com/graphql") {
                headers {
                    append("User-Agent", "GitHubWidget/1.0")
                    append("Accept", "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(GraphQLRequest(query))
            }.body()

            // Check for errors in response
            if (response.data?.user == null) {
                android.util.Log.e("GitHubFetcher", "User not found or invalid response: $response")
                return null
            }

            val weeks = response.data?.user?.contributionsCollection?.contributionCalendar?.weeks ?: emptyList()
            weeks.flatMapIndexed { index, week ->
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
        } catch (e: Exception) {
            android.util.Log.e("GitHubFetcher", "Error fetching contributions", e)
            null
        } finally {
            client.close()
        }
    }
}
