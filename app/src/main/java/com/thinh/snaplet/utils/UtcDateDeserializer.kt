package com.thinh.snaplet.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
class UtcDateDeserializer : JsonDeserializer<Date> {

    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    private val formats: List<SimpleDateFormat> = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
    ).onEach { it.timeZone = utcTimeZone }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        val raw = try {
            json.asString
        } catch (e: UnsupportedOperationException) {
            return Date(0)
        } catch (e: ClassCastException) {
            return Date(0)
        } catch (e: IllegalStateException) {
            return Date(0)
        }

        if (raw.isNullOrBlank()) return Date(0)

        for (format in formats) {
            try {
                return format.parse(raw)
            } catch (_: ParseException) {
                // try next format
            }
        }

        // As a last resort, let Gson try its default parsing once.
        return try {
            context.deserialize<Date>(json, Date::class.java)
        } catch (_: JsonParseException) {
            Date(0)
        } catch (_: Exception) {
            Date(0)
        }
    }
}