package com.selfbudget.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.AppLogoBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill

@Composable
fun AppLockScreen(
    onUnlockClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    isSetupPrompt: Boolean = false
) {
    val isDark = isAppInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Hero Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Custom Self Budget App Logo Badge
                AppLogoBadge(
                    size = 110.dp,
                    showLockBadge = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                // App Brand Title & Subtitle
                Text(
                    text = "Self Budget",
                    style = SelfBudgetType.title.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = ShapePill,
                    color = Ramp.Teal.tintFill(isDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Ramp.Teal.secondaryText(isDark),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSetupPrompt) "Enable biometric security" else "Private & encrypted financial vault",
                            style = SelfBudgetType.badge,
                            color = Ramp.Teal.secondaryText(isDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Modern Action Unlock Button
                PrimaryPillButton(
                    text = if (isSetupPrompt) "Enable biometrics" else "Unlock with biometrics",
                    onClick = onUnlockClick,
                    ramp = Ramp.Teal,
                    icon = Icons.Default.Fingerprint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                if (onSkipClick != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onSkipClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Not now",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Privacy Note Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% offline & private — data stays on your device",
                    style = SelfBudgetType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
