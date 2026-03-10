package com.thinh.snaplet.platform.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.thinh.snaplet.utils.InviteConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class ShareContent(
    val str: String
)

data class ShareApp(
    val packageName: String,
    val displayName: String,
    val appIcon: Drawable,
    val iconBackgroundColor: Int? = null
)

interface ShareManager {
    fun getTopShareApps(): List<ShareApp>
    fun shareToApp(packageName: String, content: ShareContent)
    fun openSystemChooser(content: ShareContent)
    fun buildInviteShareContent(userName: String?): ShareContent
}

class ShareManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ShareManager {

    companion object {
        private val PREFERRED_PACKAGE_SUBSTRINGS = listOf(
            "whatsapp",
            "messenger",
            "facebook.orca",
            "instagram",
            "zalo",
            "telegram",
            "messaging",
            "line",
            "viber",
            "signal",
            "discord",
            "slack",
            "facebook.lite",
            // Social
            "facebook.katana",
            "twitter",
            "snapchat",
            "linkedin",
            "pinterest",
            "tiktok",
            // Email / drive / browser
            "gmail",
            "android.gm",
            "mail",
            "drive",
            "chrome",
            "outlook"
        )
    }

    override fun getTopShareApps(): List<ShareApp> {
        val pm = context.packageManager

        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        val flags = PackageManager.MATCH_ALL

        val all = pm.queryIntentActivities(intent, flags).distinctBy { it.activityInfo.packageName }

        val withRank = all.map { info ->
            val pkg = info.activityInfo.packageName.lowercase()
            val rank = PREFERRED_PACKAGE_SUBSTRINGS.indexOfFirst { pkg.contains(it) }.let {
                if (it < 0) Int.MAX_VALUE else it
            }
            info to rank
        }

        val top3 = withRank.sortedBy { it.second }.take(3).map { it.first }

        return top3.map {
            val icon = it.loadIcon(pm)
            val backgroundColor =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                    val bg = icon.background
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bg is ColorDrawable) bg.color else null
                } else {
                    null
                }
            ShareApp(
                packageName = it.activityInfo.packageName,
                displayName = it.loadLabel(pm).toString(),
                appIcon = icon,
                iconBackgroundColor = backgroundColor
            )
        }
    }

    override fun shareToApp(packageName: String, content: ShareContent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content.str)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun openSystemChooser(content: ShareContent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content.str)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun buildInviteShareContent(userName: String?): ShareContent {
        val inviteUrl = if (!userName.isNullOrBlank()) {
            "${InviteConstants.BASE_URL}?userName=${Uri.encode(userName)}"
        } else {
            InviteConstants.BASE_URL
        }
        return ShareContent(str = inviteUrl)
    }
}