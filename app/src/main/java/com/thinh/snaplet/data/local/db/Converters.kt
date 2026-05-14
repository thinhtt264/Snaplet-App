package com.thinh.snaplet.data.local.db

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.thinh.snaplet.data.model.chat.MessageMediaStatus
import com.thinh.snaplet.data.model.chat.MessageReaction
import com.thinh.snaplet.utils.network.GsonHolder
import java.util.Date

class Converters {
    @TypeConverter
    fun fromDate(value: Date?): Long? = value?.time

    @TypeConverter
    fun toDate(value: Long?): Date? = value?.let(::Date)

    @TypeConverter
    fun fromReactions(reactions: List<MessageReaction>?): String =
        GsonHolder.gson.toJson(reactions ?: emptyList<MessageReaction>())

    @TypeConverter
    fun toReactions(raw: String?): List<MessageReaction> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            GsonHolder.gson.fromJson<List<MessageReaction>>(raw, REACTIONS_TYPE)
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromMessageMediaStatus(value: MessageMediaStatus?): String? = value?.name

    @TypeConverter
    fun toMessageMediaStatus(value: String?): MessageMediaStatus =
        value?.let { raw ->
            runCatching { MessageMediaStatus.valueOf(raw) }.getOrNull()
        } ?: MessageMediaStatus.AVAILABLE

    private companion object {
        private val REACTIONS_TYPE = object : TypeToken<List<MessageReaction>>() {}.type
    }
}
