package com.thinh.snaplet.data.repository.quickchat

import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickChatEmojiRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
) : QuickChatEmojiRepository {

    override suspend fun getRecentEmojis(
        defaultEmojis: List<String>,
        maxSlots: Int,
    ): List<String> {
        if (maxSlots <= 0) return emptyList()

        val recentOrderedNewestFirst = dataStoreManager.getQuickChatRecentEmojis()
        val result = ArrayList<String>(maxSlots)
        val seen = HashSet<String>(maxSlots * 2)

        appendUniqueUntilFull(
            source = recentOrderedNewestFirst,
            result = result,
            seen = seen,
            maxSlots = maxSlots,
        )
        appendUniqueUntilFull(
            source = defaultEmojis,
            result = result,
            seen = seen,
            maxSlots = maxSlots,
        )
        return result
    }

    override suspend fun recordEmojiUsage(emoji: String) {
        dataStoreManager.recordQuickChatEmojiUsage(emoji)
    }

    private fun appendUniqueUntilFull(
        source: List<String>,
        result: MutableList<String>,
        seen: MutableSet<String>,
        maxSlots: Int,
    ) {
        for (emoji in source) {
            if (result.size >= maxSlots) break
            if (seen.add(emoji)) {
                result.add(emoji)
            }
        }
    }
}
