package comp5331

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import model.github.GitTreeResponse
import model.github.Repository
import model.github.RepositoryResponse
import model.github.TopicsResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import okio.IOException
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

object GithubFetcher {

    private object Endpoints {
        const val ROOT = "https://api.github.com"
        const val REPOSITORIES = "$ROOT/search/repositories"
    }

    data class Config(
            val token: String?,
            val query: String?
    )

    private val httpClient = OkHttpClient()
            .newBuilder()
            .build()
    private val moshi = Moshi.Builder()
            .build()
            ?: throw RuntimeException("Unable to create JSON deserializer")

    private val jsonAdapters = mutableMapOf<KClass<out Any>, JsonAdapter<out Any>>()

    fun fetchRepos(numToFetch: Int, config: Config): List<Repository> {
        val query = config.query ?: "stars:>=1"
        val repos = mutableListOf<Repository>()

        var page = 1
        while (repos.size < numToFetch) {
            val url = "${Endpoints.REPOSITORIES}?q=${query}&sort=stars&order=desc&page=$page"

            val request = Request.Builder()
                    .apply {
                        config.token?.let { addHeader("Authorization", "token $it") }
                        url(url)
                    }
                    .build()

            println("Fetching page $page of Repositories (Current have ${repos.size})")
            val repositories = fetchFromGithub(request).use {
                if (!it.isSuccessful) {
                    throw IOException("Unexpected code ${it.code}")
                }

                deserializeJson<RepositoryResponse>(checkNotNull(it.body).source())
            }

            repos.addAll(repositories.items)
            ++page
        }

        return repos.sortedByDescending { it.stargazers_count }.take(numToFetch)
    }

    fun fetchTopics(repository: Repository, config: Config): List<String> {
        val request = Request.Builder()
                .apply {
                    addHeader("Accept", "application/vnd.github.mercy-preview+json")
                    config.token?.let { addHeader("Authorization", "token $it") }
                    url("${repository.url}/topics")
                }
                .build()

        println("Fetching topics for ${repository.full_name}")
        return fetchFromGithub(request).use {
            val response = deserializeJson<TopicsResponse>(checkNotNull(it.body).source())

            response.names
        }
    }

    fun fetchReadme(repository: Repository, config: Config): String? {
        val refRequest = Request.Builder()
                .apply {
                    config.token?.let { addHeader("Authorization", "token $it") }
                    url(repository.trees_url.replace("{/sha}", "/${repository.default_branch}"))
                }
                .build()

        println("Fetching Git Tree for ${repository.full_name}")
        val readme = fetchFromGithub(refRequest).use {
            if (!it.isSuccessful) {
                throw IOException("Unexpected code ${it.code}")
            }

            val response = deserializeJson<GitTreeResponse>(checkNotNull(it.body).source())

            response.tree.firstOrNull { it.path.contains("README", ignoreCase = true) }
        } ?: run {
            println("${repository.full_name} does not have a README")
            return null
        }

        val readmeRequest = Request.Builder()
                .apply {
                    config.token?.let { addHeader("Authorization", "token $it") }
                    url("https://raw.githubusercontent.com/${repository.full_name}/${repository.default_branch}/${readme.path}")
                }
                .build()

        println("Fetching README for ${repository.full_name}")
        return fetchFromGithub(readmeRequest).use {
            if (!it.isSuccessful) {
                throw IOException("Unexpected code ${it.code} (${it.message})")
            }

            checkNotNull(it.body).string()
        }
    }

    private fun fetchFromGithub(request: Request): Response {
        while (true) {
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                return response
            }

            val headers = response.headers
            if (headers["X-Ratelimit-Remaining"]?.toInt()?.equals(0) == true) {
                val ratelimitReset = headers["X-Ratelimit-Reset"]?.toLong()
                    ?: throw RuntimeException("Cannot find X-Ratelimit-Reset header")
                val sleepUntil = Instant.ofEpochSecond(ratelimitReset)

                println("Request \"${request.url}\" rate-limited! Sleeping until $sleepUntil")

                Thread.sleep(Duration.between(Instant.now(), sleepUntil).toMillis())
            } else {
                throw IOException("Unexpected code when fetching ${request.url}: ${response.code}")
            }
        }
    }

    private inline fun <reified T : Any> deserializeJson(source: BufferedSource): T {
        val adapter = jsonAdapters.getOrPut(T::class) { moshi.adapter(T::class.java) }

        return checkNotNull(adapter.fromJson(source)) as T
    }
}