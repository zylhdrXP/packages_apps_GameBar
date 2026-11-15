/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.gamebar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.gamebar.R
import com.android.gamebar.utils.rememberHapticFeedback
import kotlinx.coroutines.launch

@Composable
fun FloatingNavToolbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val routes = listOf("home", "features", "customization", "records", "presets")
    val selectedIndex = routes.indexOf(currentRoute).coerceAtLeast(0)

    Surface(
        modifier = modifier.widthIn(max = 440.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            routes.forEachIndexed { index, route ->
                val selected = selectedIndex == index
                NavToolbarItem(
                    icon = when (route) {
                        "home" -> Icons.Rounded.Home
                        else -> null
                    },
                    painter = when (route) {
                        "features" -> painterResource(R.drawable.ic_nav_features)
                        "customization" -> painterResource(R.drawable.ic_nav_customization)
                        "records" -> painterResource(R.drawable.ic_nav_fps_record)
                        "presets" -> painterResource(R.drawable.ic_nav_presets)
                        else -> null
                    },
                    label = stringResource(when (route) {
                        "home" -> R.string.tab_home
                        "features" -> R.string.tab_features
                        "customization" -> R.string.tab_customization
                        "records" -> R.string.tab_records
                        else -> R.string.tab_presets
                    }),
                    selected = selected,
                    onClick = {
                        if (route != currentRoute) {
                            scope.launch {
                                haptic.click()
                            }
                            onNavigate(route)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NavToolbarItem(
    icon: ImageVector?,
    painter: Painter?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "navigation_press",
    )
    val targetRadius = when {
        pressed -> if (selected) 20.dp else 12.dp
        selected -> 26.dp
        else -> 16.dp
    }
    val radius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = motion.fastSpatialSpec(),
        label = "navigation_shape",
    )
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent,
        animationSpec = motion.fastEffectsSpec(),
        label = "navigation_color",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 14.dp else 10.dp,
        animationSpec = if (selected) motion.defaultSpatialSpec() else motion.fastSpatialSpec(),
        label = "navigation_padding",
    )

    Row(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(radius))
            .background(background)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Icon(
                    imageVector = requireNotNull(icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = motion.fastEffectsSpec()) +
                    expandHorizontally(animationSpec = motion.defaultSpatialSpec()),
                exit = fadeOut(animationSpec = motion.fastEffectsSpec()) +
                    shrinkHorizontally(animationSpec = motion.fastSpatialSpec()),
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .widthIn(max = 90.dp)
                        .clearAndSetSemantics {},
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        }
    }
}
