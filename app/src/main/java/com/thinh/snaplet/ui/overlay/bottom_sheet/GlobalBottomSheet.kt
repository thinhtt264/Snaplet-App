package com.thinh.snaplet.ui.overlay.bottom_sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.overlay.BottomSheetContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlobalBottomSheet(
    content: BottomSheetContent,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        sheetGesturesEnabled = false
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 12.dp)
                .navigationBarsPadding()
        ) {
            when (content) {
                is BottomSheetContent.Options -> OptionsSheet(content, onDismiss)

                is BottomSheetContent.Confirm -> {
//            ConfirmSheetContent(
//                content = content,
//                onConfirm = { onAction(SheetAction.Confirm) },
//                onCancel = onDismiss
//            )
                }

                is BottomSheetContent.Info -> {
//            InfoSheetContent(content)
                }
            }
        }

    }
}