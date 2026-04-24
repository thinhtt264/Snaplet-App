package com.thinh.snaplet.utils.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thinh.snaplet.utils.UtcDateDeserializer
import java.util.Date

object GsonHolder {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, UtcDateDeserializer())
        .create()
}