package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.emoji.EmojiEntry
import com.thinh.snaplet.data.model.emoji.EmojiLoader
import com.thinh.snaplet.data.model.emoji.EmojiTab
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography
import pressScaleClickable

private val ChatBg = Color(0xFF0D0D0D)
private val ChatSurface = Color(0xFF1A1C1C)
private val SeparatorColor = Color(0xFF1A1C1C)
private val EmojiKeyboardHeight = 260.dp
private const val EmojiGridColumns = 8

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: (text: String?) -> Unit,
    onAttach: () -> Unit,
    isRestricted: Boolean = false,
) {
    if (isRestricted) {
        RestrictedConversationBanner()
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val groupedEmojis = remember { EmojiLoader.loadGrouped(context) }
    val tabs = remember { EmojiTab.entries }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEmojiKeyboard by remember { mutableStateOf(false) }

    // Scaffold's innerPadding already shifts the content area up by navBarHeight,
    // so WindowInsets.ime (which is measured from the screen bottom) over-counts
    // by exactly navBarHeight. Subtract it so the Box matches the visual keyboard.
    val imeBottom =
        (WindowInsets.ime.getBottom(density) - WindowInsets.navigationBars.getBottom(density))
            .coerceAtLeast(0)
    val isKeyboardVisible = imeBottom > 0

    // Frozen while emoji is visible so the shared-space height stays stable
    // during emoji↔keyboard transitions.
    var savedKeyboardHeight by remember {
        mutableIntStateOf(with(density) { EmojiKeyboardHeight.toPx().toInt() })
    }
    if (!showEmojiKeyboard && imeBottom > savedKeyboardHeight) {
        savedKeyboardHeight = imeBottom
    }

    // When the user explicitly taps the emoji icon while the keyboard is already
    // at full height, we must NOT auto-dismiss: the keyboard is about to slide
    // away and the emoji panel should be revealed underneath.
    var suppressAutoDismiss by remember { mutableStateOf(false) }
    if (!isKeyboardVisible) suppressAutoDismiss = false

    // Auto-dismiss the emoji panel only when the keyboard slid up OVER it
    // (i.e. the user tapped the text field while emoji was open). At this moment
    // imeBottom == savedKeyboardHeight, so the formula gives the same Box height
    // on both sides of the condition → no layout jump.
    if (showEmojiKeyboard && !suppressAutoDismiss
        && savedKeyboardHeight > 0 && imeBottom >= savedKeyboardHeight
    ) {
        showEmojiKeyboard = false
    }

    if (isKeyboardVisible && !showEmojiKeyboard) {
        selectedTab = selectedTab.coerceIn(0, tabs.lastIndex)
    }

    // ── Animated height for pure emoji toggle (no keyboard involvement) ─────
    // When the keyboard IS animating, imeBottom already provides a smooth value
    // so we skip local animation and snap to the correct resting position once
    // the keyboard finishes. When the keyboard is NOT involved we run a spring.

    val emojiHeightAnim = remember { Animatable(0f) }
    var wasKeyboardVisible by remember { mutableStateOf(false) }

    // True only while the local emoji animation is intentionally active (open or
    // close without keyboard). Reset to false on auto-dismiss so the stale
    // Animatable value is never used after the keyboard dismisses.
    var emojiAnimActive by remember { mutableStateOf(false) }

    // True when the user opened the emoji panel while the keyboard was visible.
    // Only in this case should the one-frame gap (isKeyboardVisible just turned
    // false but emojiAnimActive not yet set) use the keyboard path to avoid a
    // jump. For a pure emoji open the gap frame should be 0 so the spring starts
    // from 0 naturally.
    var emojiOpenedFromKeyboard by remember { mutableStateOf(false) }

    // Keyboard just finished dismissing → decide the resting state.
    LaunchedEffect(isKeyboardVisible) {
        val justDismissed = wasKeyboardVisible && !isKeyboardVisible
        wasKeyboardVisible = isKeyboardVisible
        if (justDismissed) {
            emojiOpenedFromKeyboard = false
            if (showEmojiKeyboard) {
                emojiAnimActive = true
                emojiHeightAnim.snapTo(savedKeyboardHeight.toFloat())
            } else {
                emojiAnimActive = false
                emojiHeightAnim.snapTo(0f)
            }
        }
    }

    // Animate open/close only when the keyboard is not driving the transition.
    // Mark emojiAnimActive so the height path below knows which value to use.
    LaunchedEffect(showEmojiKeyboard) {
        if (!isKeyboardVisible) {
            emojiAnimActive = true
            emojiHeightAnim.animateTo(
                targetValue = if (showEmojiKeyboard) savedKeyboardHeight.toFloat() else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            // Animation completed — only deactivate after a close animation so the
            // Box collapses cleanly; opening leaves it active.
            if (!showEmojiKeyboard) emojiAnimActive = false
        } else {
            // Keyboard is visible and emoji was auto-dismissed: invalidate the
            // stale Animatable value so it is never rendered after dismiss.
            if (!showEmojiKeyboard) emojiAnimActive = false
        }
    }

    // Height rules (in priority order):
    // 1. Keyboard path: keyboard is animating OR keyboard→emoji transition is in the
    //    one-frame gap (isKeyboardVisible just turned false but emojiAnimActive not set
    //    yet). The gap-bridge only applies when emoji was opened from keyboard, NOT
    //    for a pure emoji open (where the box should start at 0 so the spring animates
    //    from the bottom naturally).
    // 2. Emoji path: emojiAnimActive is true — the Animatable holds the right value.
    // 3. Zero: nothing is open.
    val bottomHeightDp = when {
        isKeyboardVisible || (showEmojiKeyboard && !emojiAnimActive && emojiOpenedFromKeyboard) -> with(
            density
        ) {
            maxOf(imeBottom, if (showEmojiKeyboard) savedKeyboardHeight else 0).toDp()
        }

        emojiAnimActive -> with(density) { emojiHeightAnim.value.toDp() }
        else -> 0.dp
    }

    // Keep emoji content visible while the close animation is running so the
    // panel slides away instead of vanishing instantly.
    val showEmojiContent = showEmojiKeyboard || (emojiAnimActive && emojiHeightAnim.value > 1f)

    HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Attach / gallery icon
//            AppIconButton(
//                icon = IconSpec.Vector(
//                    imageVector = Icons.Outlined.Image,
//                    tint = Color.White.copy(alpha = 0.30f),
//                ),
//                iconSize = 28.dp,
//                onClick = onAttach,
//                containerColor = Color.Transparent,
//                iconDecoration = IconDecoration(padding = 6.dp),
//            )

            // Input pill: text field + emoji icon
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = Typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(ChatSurface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                BaseText(
                                    text = stringResource(R.string.chat_input_placeholder),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    typography = Typography.bodyMedium,
                                )
                            }
                            innerTextField()
                        }
                        Icon(
                            imageVector = Icons.Outlined.EmojiEmotions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    if (showEmojiKeyboard) {
                                        showEmojiKeyboard = false
                                        keyboardController?.show()
                                    } else {
                                        // Track whether we opened emoji while keyboard was
                                        // visible so the one-frame bridge below applies only
                                        // to keyboard→emoji, not to a pure emoji open.
                                        if (isKeyboardVisible) emojiOpenedFromKeyboard = true
                                        // If keyboard is already at full height the panel will be
                                        // revealed as the keyboard slides away; suppress the
                                        // auto-dismiss that would otherwise fire immediately.
                                        if (savedKeyboardHeight > 0 && imeBottom >= savedKeyboardHeight) {
                                            suppressAutoDismiss = true
                                        }
                                        showEmojiKeyboard = true
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                    }
                                },
                        )
                    }
                },
            )

            val isActive = value.isNotBlank()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White else ChatSurface)
                    .pressScaleClickable(
                        enabled = isActive,
                        onClick = { onSendMessage(value) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.background
                    else Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomHeightDp),
        ) {
            if (showEmojiContent) {
                val selectedTabKey = tabs.getOrNull(selectedTab)
                val displayEmojis = selectedTabKey?.let { groupedEmojis[it] }.orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChatSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    EmojiKeyboardTabRow(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                    EmojiKeyboardGrid(
                        emojis = displayEmojis,
                        onEmojiClick = { emoji -> onValueChange(value + emoji) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictedConversationBanner() {
    HorizontalDivider(color = SeparatorColor, thickness = 1.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatSurface)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = stringResource(R.string.chat_restricted_send_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            typography = Typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmojiKeyboardTabRow(
    tabs: List<EmojiTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest
                        else Color.Transparent,
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                BaseText(text = tab.icon, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun EmojiKeyboardGrid(
    emojis: List<EmojiEntry>,
    onEmojiClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(EmojiGridColumns),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(emojis, key = { it.hexcode ?: it.unicode }) { entry ->
            BaseText(
                text = entry.unicode,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { onEmojiClick(entry.unicode) }
                    .padding(6.dp),
            )
        }
    }
}
