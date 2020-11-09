package comp5331

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
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

class MainCmd : CliktCommand() {

    private val token: String? by option(help = "Github Token")
    private val output: Path by option(help = "JSON Output Path").path().default(Paths.get("", "output.json"))
    private val numToFetch: Int by argument("NUM_TO_FETCH", help = "Number of repositories to fetch").int()

    override fun run() {
        val config = GithubFetcher.Config(token = token)

        val repos = GithubFetcher.fetchRepos(numToFetch, config)

        // TODO: Add parallel fetching of topics and readmes
        val topics = repos.map { GithubFetcher.fetchTopics(it, config) }
        val readmes = repos.map { GithubFetcher.fetchReadme(it, config) }

        val outData = List(repos.size) {
            RepositoryOutputFormat.fromGithub(repos[it], topics[it], readmes[it])
        }

        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, RepositoryOutputFormat::class.java)
        val jsonAdapter = moshi.adapter<List<RepositoryOutputFormat>>(type)

        Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .write(jsonAdapter.toJson(outData))
    }
}

fun main(args: Array<String>) = MainCmd().main(args)