package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton

@Composable
fun EmptyMediaPage(
    onAddFriendClick: () -> Unit = {},
    firstName: String? = null,
) {
    val title = if (firstName != null) {
        stringResource(R.string.empty_media_filter_title)
    } else {
        stringResource(R.string.empty_media_title)
    }
    val subtitle = if (firstName != null) {
        stringResource(R.string.empty_media_filter_subtitle, firstName)
    } else {
        stringResource(R.string.empty_media_subtitle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(CommonImages.PhotoPlaceholder),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            BaseText(
                text = title,
                typography = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            BaseText(
                text = subtitle,
                typography = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        if (firstName == null) {
            PrimaryButton(
                onClick = onAddFriendClick,
                title = stringResource(R.string.empty_media_add_friend),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                contentPadding = PaddingValues(all = 16.dp),
                titleColor = Color.Black,
                typography = MaterialTheme.typography.titleMedium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
            )
        }
    }
}