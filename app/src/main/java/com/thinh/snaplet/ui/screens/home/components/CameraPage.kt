import androidx.camera.core.ImageCapture
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AnimatedButton
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.home.HomeUiState
import com.thinh.snaplet.ui.screens.home.HomeViewModel
import com.thinh.snaplet.utils.Logger

/** Camera section with preview or permission denied overlay */
@Composable
fun CameraPage(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val cameraModifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(top = 100.dp)
            .clip(RoundedCornerShape(15.dp))

        if (uiState.hasCameraPermission) {
            CameraPreview(
                modifier = cameraModifier, onImageCaptureReady = onImageCaptureReady
            )
        } else {
            CameraPermissionDenied(
                modifier = cameraModifier.background(MaterialTheme.colorScheme.surface),
                onRequestPermission = {
                    Logger.d("🔐 Request permission from overlay")
                    viewModel.onScreenInitialized()
                })
        }

        CaptureButton(
            onClick = {
                Logger.d("📸 Capture button clicked")
                viewModel.onCapturePhoto(context)
            },
            modifier = Modifier.padding(top = 56.dp)
        )
    }
}

/** Overlay shown when camera permission is denied */
@Composable
private fun CameraPermissionDenied(
    modifier: Modifier = Modifier, onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            AppText(
                text = stringResource(R.string.camera_unavailable),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            AppText(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.approve_camera),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                onClick = onRequestPermission,
                title = stringResource(R.string.approve),
                titleColor = Color.Black,
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color.White
        }, animationSpec = tween(durationMillis = 150), label = "capture_button_color"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .border(
                width = 6.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape
            ), contentAlignment = Alignment.Center
    ) {
        AnimatedButton(
            modifier = Modifier.size(60.dp),
            onClick = onClick,
            scaleOnPress = 0.85f,
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = backgroundColor, shape = CircleShape
                    )
            )
        }
    }
}
