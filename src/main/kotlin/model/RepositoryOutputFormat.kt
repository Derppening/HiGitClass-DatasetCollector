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
            text.replace(Regex("\\s"), " "),
            tags.joinToString(" "),
            repo_name_seg,
            user
        ).joinToString(" ")
    }

    companion object {

//        fun transformText(input: String): String {
//            return input
//                .replace("\n", " ")
//                .replace(Regex("([^a-zA-Z\\d\\s])([a-zA-Z\\d\\s])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
//                .replace(Regex("([a-zA-Z\\d\\s])([^a-zA-Z\\d\\s])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
//        }

        fun fromGithub(repository: Repository, topics: List<String>, readme: String?): RepositoryOutputFormat {
            return RepositoryOutputFormat(
                user = repository.owner.login,
                text = arrayOf(repository.description, readme).filterNotNull().joinToString(" "),
//                text = transformText(arrayOf(repository.description, readme).filterNotNull().joinToString(" ")),
                tags = topics,
                super_label = null,
                sub_label = null,
                repo_name_seg = repository.name.replace(Regex("[^a-zA-Z\\d]"), " "),
                repo_name = repository.full_name
            )
        }
    }
}