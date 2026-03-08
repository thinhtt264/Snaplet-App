package com.thinh.snaplet.ui.screens.my_profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.navigation.CollectNavResult
import com.thinh.snaplet.navigation.NavResultKeys
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.DarkGray
import com.thinh.snaplet.ui.theme.GoldenPollen
import com.thinh.snaplet.ui.theme.Gray
import com.thinh.snaplet.ui.theme.Red
import pressScaleClickable

private sealed interface ProfileMenuItem {
    val icon: ImageVector
    val label: String

    data class Standard(
        override val icon: ImageVector,
        override val label: String,
        val subtitle: String? = null,
        val onClick: () -> Unit = {},
    ) : ProfileMenuItem

    data class Toggle(
        override val icon: ImageVector,
        override val label: String,
        val isChecked: Boolean,
        val onToggle: () -> Unit = {},
    ) : ProfileMenuItem

    data class Danger(
        override val icon: ImageVector,
        override val label: String,
        val onClick: () -> Unit = {},
    ) : ProfileMenuItem
}

private data class ProfileSection(
    val title: String,
    val items: List<ProfileMenuItem>,
)

private data class ProfileStrings(
    val back: String,
    val editPhoto: String,
    val inviteFriendsTitle: String,
    val shareInvite: String,
    val sectionWidgetSettings: String,
    val addWidget: String,
    val howToAddWidget: String,
    val widgetChain: String,
    val sectionGeneral: String,
    val notifications: String,
    val editName: String,
    val changeEmail: String,
    val sectionPrivacySecurity: String,
    val blockedAccounts: String,
    val privacyAndData: String,
    val sectionAbout: String,
    val tiktok: String,
    val instagram: String,
    val xTwitter: String,
    val shareSnaplet: String,
    val termsOfService: String,
    val privacyPolicy: String,
    val sectionDangerZone: String,
    val deleteAccount: String,
    val logout: String,
)

@Composable
fun MyProfile(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToImageCrop: (Uri) -> Unit,
    viewModel: MyProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoPickerDismissed()
            onNavigateToImageCrop(uri)
        } else {
            viewModel.onPhotoPickerDismissed()
        }
    }

    CollectNavResult(NavResultKeys.CroppedUri) { uriStr ->
        viewModel.onPhotoPicked(uriStr.toUri())
    }

    LaunchedEffect(uiState.showPhotoPicker) {
        if (uiState.showPhotoPicker) {
            viewModel.onPhotoPickerLaunched()
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    val strings = ProfileStrings(
        back = stringResource(R.string.profile_back),
        editPhoto = stringResource(R.string.profile_edit_photo),
        inviteFriendsTitle = stringResource(R.string.profile_invite_friends_title),
        shareInvite = stringResource(R.string.profile_share_invite),
        sectionWidgetSettings = stringResource(R.string.profile_section_widget_settings),
        addWidget = stringResource(R.string.profile_add_widget),
        howToAddWidget = stringResource(R.string.profile_how_to_add_widget),
        widgetChain = stringResource(R.string.profile_widget_chain),
        sectionGeneral = stringResource(R.string.profile_section_general),
        notifications = stringResource(R.string.profile_notifications),
        editName = stringResource(R.string.profile_edit_name),
        changeEmail = stringResource(R.string.profile_change_email),
        sectionPrivacySecurity = stringResource(R.string.profile_section_privacy_security),
        blockedAccounts = stringResource(R.string.profile_blocked_accounts),
        privacyAndData = stringResource(R.string.profile_privacy_and_data),
        sectionAbout = stringResource(R.string.profile_section_about),
        tiktok = stringResource(R.string.profile_tiktok),
        instagram = stringResource(R.string.profile_instagram),
        xTwitter = stringResource(R.string.profile_x_twitter),
        shareSnaplet = stringResource(R.string.profile_share_snaplet),
        termsOfService = stringResource(R.string.profile_terms_of_service),
        privacyPolicy = stringResource(R.string.profile_privacy_policy),
        sectionDangerZone = stringResource(R.string.profile_section_danger_zone),
        deleteAccount = stringResource(R.string.profile_delete_account),
        logout = stringResource(R.string.logout),
    )
    val sections = buildProfileSections(
        widgetChainEnabled = uiState.widgetChainEnabled,
        onWidgetChainToggle = viewModel::onWidgetChainToggle,
        onLogoutClick = viewModel::onLogout,
        displayName = uiState.displayName,
        email = uiState.email,
        strings = strings,
    )

    MyProfileContent(
        uiState = uiState,
        sections = sections,
        editPhotoLabel = strings.editPhoto,
        inviteFriendsTitle = strings.inviteFriendsTitle,
        onBackClick = onBackClick,
        onEditPhotoClick = viewModel::onEditPhoto,
        onShareInviteClick = { },
        modifier = modifier
    )
}

@Composable
private fun MyProfileContent(
    uiState: MyProfileUiState,
    sections: List<ProfileSection>,
    editPhotoLabel: String,
    inviteFriendsTitle: String,
    onBackClick: () -> Unit,
    onEditPhotoClick: () -> Unit,
    onShareInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(32.dp)
                    .pressScaleClickable(onClick = onBackClick),
            )
        }

        ProfileHeader(
            avatarUrl = uiState.avatarUrl,
            firstName = uiState.firstName,
            displayName = uiState.displayName,
            editPhotoLabel = editPhotoLabel,
            onEditPhotoClick = onEditPhotoClick,
            isAvatarUploading = uiState.isAvatarChanging
        )

        Spacer(modifier = Modifier.height(16.dp))

        InviteCard(
            avatarUrl = uiState.avatarUrl,
            firstName = uiState.firstName,
            userName = uiState.userName,
            inviteTitle = inviteFriendsTitle,
            onShareClick = onShareInviteClick,
        )

        sections.forEach { section ->
            ProfileSectionView(section)
        }

        Spacer(
            modifier = Modifier
                .height(32.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun ProfileHeader(
    avatarUrl: String?,
    firstName: String,
    displayName: String,
    isAvatarUploading: Boolean,
    editPhotoLabel: String,
    onEditPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            avatarUrl = avatarUrl,
            firstName = firstName,
            isUploading = isAvatarUploading,
            isConnectedUser = true,
            size = 120.dp,
            borderWidth = 4.dp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        BaseText(
            text = displayName,
            typography = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        BaseText(
            text = editPhotoLabel,
            typography = MaterialTheme.typography.bodyMedium,
            color = GoldenPollen,
            modifier = Modifier
                .pressScaleClickable(onClick = onEditPhotoClick)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun InviteCard(
    avatarUrl: String?,
    firstName: String,
    userName: String,
    inviteTitle: String,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .pressScaleClickable(onClick = onShareClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUrl = avatarUrl,
            firstName = firstName,
            isConnectedUser = true,
            size = 40.dp,
            borderWidth = 1.dp,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            BaseText(
                text = inviteTitle,
                typography = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            BaseText(
                text = "snaplet.cam/$userName",
                typography = MaterialTheme.typography.bodySmall,
                color = Gray,
            )
        }

        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = "share-invite",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileSectionView(
    section: ProfileSection, modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(20.dp))

        BaseText(
            text = section.title,
            typography = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        section.items.forEach { item ->
            ProfileMenuItemRow(item)
        }
    }
}

@Composable
private fun ProfileMenuItemRow(
    item: ProfileMenuItem, modifier: Modifier = Modifier
) {
    val textColor = when (item) {
        is ProfileMenuItem.Danger -> Red
        else -> Color.White
    }
    val onClick: () -> Unit = when (item) {
        is ProfileMenuItem.Standard -> item.onClick
        is ProfileMenuItem.Toggle -> item.onToggle
        is ProfileMenuItem.Danger -> item.onClick
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick, scaleOnPress = 0.98f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = Color.LightGray,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = item.label,
                typography = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (item is ProfileMenuItem.Standard && item.subtitle != null) {
                BaseText(
                    text = item.subtitle,
                    typography = MaterialTheme.typography.bodySmall,
                    color = Gray,
                )
            }
        }

        if (item is ProfileMenuItem.Toggle) {
            Switch(
                checked = item.isChecked, onCheckedChange = null, colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GoldenPollen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = DarkGray,
                )
            )
        }
    }
}

private fun buildProfileSections(
    widgetChainEnabled: Boolean,
    onWidgetChainToggle: () -> Unit,
    onLogoutClick: () -> Unit,
    displayName: String,
    email: String,
    strings: ProfileStrings,
): List<ProfileSection> = listOf(
    ProfileSection(
        title = strings.sectionWidgetSettings, items = listOf(
            ProfileMenuItem.Standard(
                icon = Icons.Filled.AddBox,
                label = strings.addWidget,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = strings.howToAddWidget,
            ),
            ProfileMenuItem.Toggle(
                icon = Icons.Filled.LocalFireDepartment,
                label = strings.widgetChain,
                isChecked = widgetChainEnabled,
                onToggle = onWidgetChainToggle,
            ),
        )
    ),
    ProfileSection(
        title = strings.sectionGeneral, items = listOf(
            ProfileMenuItem.Standard(
                icon = Icons.Filled.NotificationsActive,
                label = strings.notifications,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Person,
                label = strings.editName,
                subtitle = displayName,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Email,
                label = strings.changeEmail,
                subtitle = email,
            ),
        )
    ),
    ProfileSection(
        title = strings.sectionPrivacySecurity, items = listOf(
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Block,
                label = strings.blockedAccounts,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.VerifiedUser,
                label = strings.privacyAndData,
            ),
        )
    ),
    ProfileSection(
        title = strings.sectionAbout, items = listOf(
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.MusicNote,
                label = strings.tiktok,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.CameraAlt,
                label = strings.instagram,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Tag,
                label = strings.xTwitter,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Share,
                label = strings.shareSnaplet,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Description,
                label = strings.termsOfService,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.Security,
                label = strings.privacyPolicy,
            ),
        )
    ),
    ProfileSection(
        title = strings.sectionDangerZone, items = listOf(
            ProfileMenuItem.Danger(
                icon = Icons.Outlined.Delete,
                label = strings.deleteAccount,
            ),
            ProfileMenuItem.Standard(
                icon = Icons.Outlined.WavingHand,
                label = strings.logout,
                onClick = onLogoutClick,
            ),
        )
    ),
)