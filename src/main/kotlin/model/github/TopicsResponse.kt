package model.github

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicsResponse(
    val names: List<String>
)