package com.thinh.snaplet.data.model.emoji

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EmojiLoader {

    private var cachedEmojis: List<EmojiEntry>? = null
    private var groupedCache: Map<EmojiTab, List<EmojiEntry>>? = null

    fun loadAll(context: Context): List<EmojiEntry> {
        cachedEmojis?.let { return it }

        val json = context.assets.open("emoji_compact.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<EmojiEntry>>() {}.type
        val all: List<EmojiEntry> = Gson().fromJson(json, type)

        val filtered = all
            .filter { it.group != null && it.group != 2 && it.unicode.isNotBlank() }
            .sortedBy { it.order ?: Int.MAX_VALUE }

        cachedEmojis = filtered
        return filtered
    }

    fun loadGrouped(context: Context): Map<EmojiTab, List<EmojiEntry>> {
        groupedCache?.let { return it }

        val all = loadAll(context)
        val result = EmojiTab.entries.associateWith { tab ->
            all.filter { it.group in tab.groupIds }
        }

        groupedCache = result
        return result
    }

    fun search(context: Context, query: String): List<EmojiEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return loadAll(context).filter { entry ->
            entry.label.contains(q) ||
                    entry.tags?.any { it.contains(q) } == true
        }
    }
}
