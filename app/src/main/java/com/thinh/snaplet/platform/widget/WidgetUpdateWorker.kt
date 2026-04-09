package com.thinh.snaplet.platform.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.ui.widget.SnapletWidget
import com.thinh.snaplet.ui.widget.SnapletWidgetStateKeys
import com.thinh.snaplet.utils.network.ApiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val postRepository: PostRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = manager.getGlanceIds(SnapletWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        return try {
            val latestPost = when (val payloadResult = postRepository.getPostsActivity()) {
                is ApiResult.Success -> payloadResult.data
                is ApiResult.Failure -> {
                    return Result.success()
                }
            }

            val shouldShowError = latestPost == null || latestPost.imageUrl.isBlank()
            if (shouldShowError) {
                updateWidgetToErrorState(glanceIds)
                return Result.success()
            }

            val unreadCount = latestPost.unreadCount
            var hasAnyChange = false

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context = applicationContext,
                    glanceId = glanceId,
                ) { prefs ->
                    val imageChanged = prefs.updateNullableString(
                        key = SnapletWidgetStateKeys.POST_IMAGE_URL,
                        newValue = latestPost.imageUrl,
                    )
                    val postIdChanged = prefs.updateNullableString(
                        key = SnapletWidgetStateKeys.POST_ID,
                        newValue = latestPost.postId,
                    )
                    val captionChanged = prefs.updateNullableString(
                        key = SnapletWidgetStateKeys.POST_CAPTION,
                        newValue = latestPost.caption,
                    )
                    val avatarChanged = prefs.updateNullableString(
                        key = SnapletWidgetStateKeys.SENDER_AVATAR_URL,
                        newValue = latestPost.senderAvatarUrl,
                    )

                    val oldUnreadCount = prefs[SnapletWidgetStateKeys.UNREAD_COUNT] ?: 0
                    val unreadChanged = oldUnreadCount != unreadCount
                    if (unreadChanged) prefs[SnapletWidgetStateKeys.UNREAD_COUNT] = unreadCount
                    val errorWasSet = prefs[SnapletWidgetStateKeys.IS_ERROR] != false
                    if (errorWasSet) prefs[SnapletWidgetStateKeys.IS_ERROR] = false

                    val hasContentChanged = listOf(
                        postIdChanged,
                        imageChanged,
                        captionChanged,
                        avatarChanged,
                    ).any { it }
                    val hasChanged =
                        hasContentChanged || unreadChanged || errorWasSet
                    if (hasChanged) {
                        prefs[SnapletWidgetStateKeys.LAST_UPDATED_AT] = System.currentTimeMillis()
                    }
                    hasAnyChange = hasAnyChange || hasChanged
                }
            }

            if (hasAnyChange) SnapletWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private suspend fun updateWidgetToErrorState(glanceIds: List<GlanceId>) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = applicationContext,
                glanceId = glanceId,
            ) { prefs ->
                val errorChanged = prefs[SnapletWidgetStateKeys.IS_ERROR] != true
                if (errorChanged) prefs[SnapletWidgetStateKeys.IS_ERROR] = true

                prefs.updateNullableString(
                    key = SnapletWidgetStateKeys.POST_ID,
                    newValue = null,
                )
                prefs.updateNullableString(
                    key = SnapletWidgetStateKeys.POST_IMAGE_URL,
                    newValue = null,
                )
                prefs.updateNullableString(
                    key = SnapletWidgetStateKeys.POST_CAPTION,
                    newValue = null,
                )
                prefs.updateNullableString(
                    key = SnapletWidgetStateKeys.SENDER_AVATAR_URL,
                    newValue = null,
                )

                if ((prefs[SnapletWidgetStateKeys.UNREAD_COUNT] ?: 0) != 0) {
                    prefs[SnapletWidgetStateKeys.UNREAD_COUNT] = 0
                }

                if (errorChanged) prefs[SnapletWidgetStateKeys.LAST_UPDATED_AT] =
                    System.currentTimeMillis()
            }
        }

        // Requirement: always update UI when error condition is met.
        SnapletWidget().updateAll(applicationContext)
    }
}

private fun MutablePreferences.updateNullableString(
    key: Preferences.Key<String>,
    newValue: String?,
): Boolean {
    val oldValueRaw = this[key]
    // Glance UI treats blank values as "empty", but we want to persist them canonically as `null`
    // so that state removal triggers a widget refresh reliably.
    val oldValue = oldValueRaw?.takeIf { it.isNotBlank() }
    val normalizedNewValue = newValue?.takeIf { it.isNotBlank() }

    if (oldValue == normalizedNewValue) return false
    if (normalizedNewValue == null) remove(key) else this[key] = normalizedNewValue
    return true
}
