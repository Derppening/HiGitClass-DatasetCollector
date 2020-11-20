package model

import com.squareup.moshi.JsonClass
import model.github.Repository

@JsonClass(generateAdapter = true)
data class RepositoryOutputFormat(
    val user: String,
    val text: String,
    val tags: List<String>,
    val super_label: String? = null,
    val sub_label: String? = null,
    val repo_name_seg: String,
    val repo_name: String
) {

    fun asDocument(): String {
        return arrayOf(
            text,
            tags.joinToString(" "),
            repo_name_seg,
            user
        ).joinToString(" ")
    }

    companion object {

        fun preprocessText(input: String): String {
            return input
                .trim()
                .replace("\r?\n", " ")
                .replace(Regex("[^A-Za-z0-9(),.!?\"\'-]"), " ")
                .replace("\'s", " \'s")
                .replace("\"", " \" ")
                .replace("\'ve", " \'ve")
                .replace("n\'t", " n\'t")
                .replace("\'m", " \'m")
                .replace("\'re", " \'re")
                .replace("\'d", " \'d")
                .replace("\'ll", " \'ll")
                .replace(",", " , ")
                .replace(".", " . ")
                .replace("!", " ! ")
                .replace("\$", " $ ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replace("?", " ? ")
                .replace(Regex("\\s{2,}"), " ")
        }

        fun fromGithub(repository: Repository, topics: List<String>, readme: String?): RepositoryOutputFormat {
            return RepositoryOutputFormat(
                user = repository.owner.login,
                text = arrayOf(repository.description, readme).filterNotNull().joinToString(" "),
                tags = topics,
                super_label = null,
                sub_label = null,
                repo_name_seg = repository.name,
                repo_name = repository.full_name
            )
        }
    }
}