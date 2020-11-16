package comp5331

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import model.RepositoryOutputFormat
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture

class MainCmd : CliktCommand() {

    override fun run() = Unit
}

class FetchCmd : CliktCommand(help = "Fetches a list of repositories from GitHub.", name = "fetch") {

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
            .serializeNulls()
            .let {
                if (pretty) {
                    it.indent(" ".repeat(4))
                } else it
            }

        Files.newBufferedWriter(
            output,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
            .use {
                it.write(jsonAdapter.toJson(outData))
            }
    }
}

class TransformDocumentCmd :
    CliktCommand(help = "Transforms a collected dataset to a JSON object per line.", name = "transform-doc") {

    private val input: Path by option(help = "Path to input JSON")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
    private val output: Path by option(help = "Path to output JSON")
        .path()
        .required()

    override fun run() {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, RepositoryOutputFormat::class.java)
        val oldJsonAdapter = moshi.adapter<List<RepositoryOutputFormat>>(type)

        val hier = checkNotNull(oldJsonAdapter.fromJson(Files.readString(input)))
//        val newHier = hier.map {
//            it.copy(text = it.text)
//        }

        val newJsonAdapter = moshi.adapter(RepositoryOutputFormat::class.java)

        Files.newBufferedWriter(
            output,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
            .use { writer ->
                hier.forEach {
//                newHier.forEach {
                    writer.write(newJsonAdapter.toJson(it))
                    writer.newLine()
                }
            }
    }
}

class TransformDatasetCmd :
    CliktCommand(help = "Emits necessary text files based on a collected JSON file.", name = "transform-dataset") {

    private val input: Path by option(help = "Path to input JSON")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
    private val outputDir: Path by option(help = "Directory to output files")
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)
        .default(Paths.get(""))

    override fun run() {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(RepositoryOutputFormat::class.java)

        val datasetPath = outputDir.resolve(DATASET_FILENAME)
        val labelsPath = outputDir.resolve(LABELS_FILENAME)

        Files.deleteIfExists(datasetPath)
        Files.deleteIfExists(labelsPath)

        var datasetWriter: BufferedWriter? = null
        var labelsWriter: BufferedWriter? = null
        try {
            datasetWriter = Files.newBufferedWriter(datasetPath)
            labelsWriter = Files.newBufferedWriter(labelsPath)

            Files.lines(input)
                .use { lines ->
                    lines.forEach { line ->
                        val inDataset = checkNotNull(jsonAdapter.fromJson(line))
                        val document = inDataset.asDocument()

                        checkNotNull(datasetWriter).let {
                            it.write(document)
                            it.newLine()
                        }
                        checkNotNull(labelsWriter).let {
                            it.write(requireNotNull(inDataset.sub_label))
                            it.newLine()
                        }
                    }
                }
        } finally {
            datasetWriter?.close()
            labelsWriter?.close()
        }
    }

    companion object {
        private const val DATASET_FILENAME = "dataset.txt"
        private const val LABELS_FILENAME = "labels.txt"
    }
}

fun main(args: Array<String>) = MainCmd()
    .subcommands(FetchCmd(), TransformDatasetCmd(), TransformDocumentCmd())
    .main(args)