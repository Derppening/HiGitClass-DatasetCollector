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

    companion object {
        fun fromGithub(repository: Repository, topics: List<String>, readme: String?): RepositoryOutputFormat {
            return RepositoryOutputFormat(
                    user = repository.owner.login,
                    text = arrayOf(repository.description, readme).filterNotNull().joinToString(" "),
                    tags = topics,
                    super_label = null,
                    sub_label = null,
                    repo_name_seg = repository.name.replace(Regex("[^a-zA-Z\\d]"), " "),
                    repo_name = repository.full_name
            )
        }
    }
}