package comp5331

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import model.RepositoryOutputFormat
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture

class MainCmd : CliktCommand() {

    private val token: String? by option(help = "Github Token")
    private val query: String? by option(help = "Github Query String Override")
    private val parallel: Boolean by option(help = "Fetch topics and README asynchronously").flag()
    private val pretty: Boolean by option(help = "Pretty-print the JSON output").flag()
    private val output: Path by option(help = "JSON Output Path").path().default(Paths.get("", "output.json"))
    private val numToFetch: Int by argument("NUM_TO_FETCH", help = "Number of repositories to fetch").int()

    override fun run() {
        val config = GithubFetcher.Config(token = token, query = query)

        val repos = GithubFetcher.fetchRepos(numToFetch, config)

        val topics: List<List<String>>
        val readmes: List<String?>
        if (parallel) {
            val topicsFuture = CompletableFuture.supplyAsync {
                repos.map { GithubFetcher.fetchTopics(it, config) }
            }
            val readmesFuture = CompletableFuture.supplyAsync {
                repos.map { GithubFetcher.fetchReadme(it, config) }
            }

            topics = topicsFuture.join()
            readmes = readmesFuture.join()
        } else {
            topics = repos.map { GithubFetcher.fetchTopics(it, config) }
            readmes = repos.map { GithubFetcher.fetchReadme(it, config) }
        }

        val outData = List(repos.size) {
            RepositoryOutputFormat.fromGithub(repos[it], topics[it], readmes[it])
        }

        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, RepositoryOutputFormat::class.java)
        val jsonAdapter = moshi.adapter<List<RepositoryOutputFormat>>(type)
            .let {
                if (pretty) {
                    it.indent(" ".repeat(4))
                } else it
            }

        Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use {
                it.write(jsonAdapter.toJson(outData))
            }
    }
}

fun main(args: Array<String>) = MainCmd().main(args)