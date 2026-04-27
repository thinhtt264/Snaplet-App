package com.thinh.snaplet.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * Lenient UTC Date deserializer for backend ISO-8601 timestamps.
 *
 * Tries multiple patterns to handle variants like:
 * - 2026-03-18T12:34:56.789Z
 * - 2026-03-18T12:34:56Z
 * - 2026-03-18T12:34:56.789+07:00
 * - 2026-03-18T12:34:56+07:00
 *
 * On failure, returns Date(0) instead of throwing to avoid crashing the app.
 */
class UtcDateDeserializer : JsonDeserializer<Date>, JsonSerializer<Date> {

    override fun serialize(
        src: Date,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val iso = Instant.ofEpochMilli(src.time)
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        return JsonPrimitive(iso)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        val raw = json.asString.takeIf { it.isNotBlank() } ?: return Date(0)

        return try {
            val instant = OffsetDateTime.parse(raw).toInstant()
            Date.from(instant)
        } catch (_: DateTimeParseException) {
            try {
                val instant = ZonedDateTime.parse(raw).toInstant()
                Date.from(instant)
            } catch (_: Exception) {
                Date(0)
            }
        }
    }
}