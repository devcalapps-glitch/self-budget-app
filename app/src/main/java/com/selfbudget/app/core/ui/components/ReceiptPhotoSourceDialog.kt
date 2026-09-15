package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard

/**
 * Receipt photo source picker dialog adhering strictly to the design system guide:
 * - Uniform typography scale (SelfBudgetType.title, section, rowTitle, meta, body)
 * - SectionHeaderBand container with hairline-divided rows (never floating disconnected cards)
 * - Semantic GrayIconTile for neutral field-type actions
 * - Standard SecondaryPillButton(ramp = Ramp.Gray) for dismiss action
 */
@Composable
fun ReceiptPhotoSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Title + Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan receipt",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Capture a receipt to automatically read the merchant and total amount.",
                    style = SelfBudgetType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Shared SectionHeaderBand container pattern (spec §7)
                SectionHeaderBand(
                    title = "Photo source",
                    ramp = Ramp.Teal,
                    icon = Icons.Default.CameraAlt
                ) {
                    // Option 1: Live camera photo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onTakePhoto()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GrayIconTile(icon = Icons.Default.CameraAlt)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Take photo",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use camera to photograph receipt",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))

                    // Option 2: Choose from device gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onChooseGallery()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GrayIconTile(icon = Icons.Default.PhotoLibrary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose from gallery",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pick existing photo from device",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                SecondaryPillButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    ramp = Ramp.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
