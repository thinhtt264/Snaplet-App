package com.thinh.snaplet.data.repository.quickchat

interface QuickChatEmojiRepository {
    suspend fun getRecentEmojis(
        defaultEmojis: List<String>,
        maxSlots: Int,
    ): List<String>

    suspend fun recordEmojiUsage(emoji: String)
}
