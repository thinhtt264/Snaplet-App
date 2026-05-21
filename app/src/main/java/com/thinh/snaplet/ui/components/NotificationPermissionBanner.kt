package com.thinh.snaplet.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.thinh.snaplet.R
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.ui.theme.SnapletTheme
import com.thinh.snaplet.ui.theme.Typography

private val BannerGradientStart = Color(0xFF3A2800)
private val BannerGradientEnd = Color(0xFF2E2000)

@Composable
fun NotificationPermissionBanner(
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        NotificationPermissionBannerInternal(modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationPermissionBannerInternal(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isGranted = ContextCompat.checkSelfPermission(
        context,
        Permission.Notifications.manifestPermission,
    ) == PackageManager.PERMISSION_GRANTED

    if (isGranted) return

    var permissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            })
    }

    PermissionHandler(
        permission = Permission.Notifications,
        onPermissionResult = { granted ->
            if (!granted) {
                permissionDeniedOnce = true
                val shouldShowRationale =
                    (context as? Activity)?.shouldShowRequestPermissionRationale(
                        Permission.Notifications.manifestPermission,
                    ) ?: false
                // shouldShowRationale == false after denial → permanently denied (incl. ghost tap)
                // → auto-open Settings so user doesn't need to tap again
                if (!shouldShowRationale) openSettings()
            }
        },
    ) { requestPermission ->
        Column(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(BannerGradientStart, BannerGradientEnd)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BaseText(
                    text = stringResource(R.string.notification_permission_banner_message),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                PrimaryButton(
                    onClick = {
                        val shouldShowRationale =
                            (context as? Activity)?.shouldShowRequestPermissionRationale(
                                Permission.Notifications.manifestPermission,
                            ) ?: false
                        if (!permissionDeniedOnce || shouldShowRationale) {
                            requestPermission()
                        } else {
                            openSettings()
                        }
                    },
                    title = stringResource(R.string.notification_permission_banner_action),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    typography = Typography.bodySmall.copy(fontSize = 12.sp),
                    titleColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionBannerPreview() {
    SnapletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(BannerGradientStart, BannerGradientEnd)),
                )
                .padding(vertical = 10.dp, horizontal = 16.dp),
        ) {
            BaseText(
                text = stringResource(R.string.notification_permission_banner_message),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
