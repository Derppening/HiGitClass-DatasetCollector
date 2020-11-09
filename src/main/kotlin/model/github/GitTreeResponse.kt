package model.github

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    val url: String,
    val tree: List<TreeNode>
) {

    @JsonClass(generateAdapter = true)
    data class TreeNode(
        val path: String,
        val url: String
    )
}