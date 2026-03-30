package com.thinh.snaplet.data.model.emoji

import com.google.gson.annotations.SerializedName

data class EmojiEntry(
    @SerializedName("unicode") val unicode: String,
    @SerializedName("label") val label: String,
    @SerializedName("group") val group: Int? = null,
    @SerializedName("order") val order: Int? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("hexcode") val hexcode: String? = null,
)

enum class EmojiTab(
    val groupIds: List<Int>,
    val icon: String,
    val displayName: String,
) {
    SMILEYS_PEOPLE(listOf(0, 1), "😊", "Smileys & People"),
    ANIMALS_NATURE(listOf(3), "🐶", "Animals & Nature"),
    FOOD_DRINK(listOf(4), "🍔", "Food & Drink"),
    ACTIVITIES(listOf(6), "⚽", "Activities"),
    TRAVEL_PLACES(listOf(5), "✈️", "Travel & Places"),
    OBJECTS(listOf(7), "💡", "Objects"),
    SYMBOLS(listOf(8), "💯", "Symbols"),
    FLAGS(listOf(9), "🏳️", "Flags"),
}
