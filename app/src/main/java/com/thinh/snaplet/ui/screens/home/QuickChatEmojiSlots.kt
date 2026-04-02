package com.thinh.snaplet.ui.screens.home

object QuickChatEmojiSlots {
    const val MaxSlots = 3

    val DefaultEmojis: List<String> = listOf("❤️", "🔥", "😍")

    fun mergeForDisplay(recentOrderedNewestFirst: List<String>): List<String> {
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (emoji in recentOrderedNewestFirst) {
            if (emoji !in seen && result.size < MaxSlots) {
                seen.add(emoji)
                result.add(emoji)
            }
        }
        for (emoji in DefaultEmojis) {
            if (emoji !in seen && result.size < MaxSlots) {
                seen.add(emoji)
                result.add(emoji)
            }
        }
        return result
    }
}
