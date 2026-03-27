package com.thinh.snaplet.data.repository.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import com.thinh.snaplet.BuildConfig
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder.gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
) : DeviceRepository {

    private val currentFingerprint = AtomicReference<String?>(null)

    override suspend fun getOrCreateFingerprint(): String {
        currentFingerprint.get()?.let { return it }

        val storedFingerprint = dataStoreManager.loadFingerprint()
        if (storedFingerprint != null) {
            currentFingerprint.set(storedFingerprint)
            Logger.d("📱 Fingerprint loaded from DataStore")
            return storedFingerprint
        }

        val fingerprint = generateFingerprint()

        dataStoreManager.saveFingerprint(fingerprint)
        currentFingerprint.set(fingerprint)

        Logger.d("📱 Fingerprint generated and saved to DataStore")
        return fingerprint
    }

    override suspend fun clearFingerprint() {
        dataStoreManager.clearFingerprint()
        currentFingerprint.set(null)
        Logger.d("🗑️ Fingerprint cleared from cache and DataStore")
    }

    override fun getFingerprintSync(): String {
        currentFingerprint.get()?.let { return it }

        runBlocking {
            dataStoreManager.loadFingerprint()
        }?.let { storedFingerprint ->
            currentFingerprint.set(storedFingerprint)
            return storedFingerprint
        }

        return generateFingerprint().also { generatedFingerprint ->
            currentFingerprint.set(generatedFingerprint)
        }
    }

    private fun generateFingerprint(): String {
        return try {
            val deviceInfo = getDeviceInfo()
            val json = gson.toJson(deviceInfo)
            val base64 = Base64.encodeToString(
                json.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            Logger.d("📱 Fingerprint generated for device: ${deviceInfo.deviceId}")
            base64
        } catch (_: Exception) {
            Base64.encodeToString(
                "{}".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
        }
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(),
            platform = "android",
            model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            appVersion = BuildConfig.VERSION_NAME,
            ip = getIpAddress(),
            userAgent = getUserAgent()
        )
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-device-id"
    }

    private fun getIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress != null) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && !hostAddress.contains(":")) {
                            return hostAddress
                        }
                    }
                }
            }
            "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }

    private fun getUserAgent(): String {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val version = BuildConfig.VERSION_NAME
        val platform = "android".replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return "$appName/$version ($platform; $model)"
    }
}