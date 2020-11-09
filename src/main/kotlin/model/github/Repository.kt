package model.github

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Repository(
    val name: String,
    val full_name: String,
    val owner: Owner,
    val description: String,
    val url: String,
    val trees_url: String,
    val stargazers_count: Int,
    val default_branch: String
) {

    @JsonClass(generateAdapter = true)
    data class Owner(
        val login: String
    )
}