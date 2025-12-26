package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.PrimaryButton

@Composable
internal fun UserProfileCard(
    userProfile: UserProfile,
    onSendRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFF3D3D3D),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(
            avatarUrl = userProfile.avatarUrl,
            displayName = userProfile.displayName
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppText(
            text = userProfile.displayName,
            typography = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            onClick = onSendRequest,
            title = stringResource(R.string.send_request),
            typography = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .height(50.dp)
                .padding(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            titleColor = Color.Black,
        )
    }
}

