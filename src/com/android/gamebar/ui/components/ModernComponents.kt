/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.android.gamebar.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.gamebar.R
import com.android.gamebar.utils.HapticFeedbackHelper
import com.android.gamebar.utils.rememberHapticFeedback
import com.android.settingslib.widget.LottieColorUtils
import kotlinx.coroutines.launch

data class SelectOption(val value: String, val label: String)

fun connectedItemShape(index: Int, count: Int): RoundedCornerShape {
    if (count <= 1) return RoundedCornerShape(20.dp)
    return when (index) {
        0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        count - 1 -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp, topStart = 4.dp, topEnd = 4.dp)
        else -> RoundedCornerShape(4.dp)
    }
}

@Composable
fun GameBarHeroLottieBanner(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme

    val containerColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_container_color",
    )
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

    val controlBarColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_bar_color",
    )
    val controlBarContentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_bar_content_color",
    )

    val lottieAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.40f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_lottie_alpha",
    )

    val emblemMorph = remember {
        Morph(MaterialShapes.Circle, MaterialShapes.Sunny)
    }
    val emblemProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "hero_emblem_morph",
    )
    val emblemRotation by animateFloatAsState(
        targetValue = if (enabled) 0f else -25f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "hero_emblem_rotation",
    )
    val emblemScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.94f,
        animationSpec = motion.fastSpatialSpec(),
        label = "hero_emblem_scale",
    )
    val emblemColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_emblem_color",
    )
    val emblemIconColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hero_emblem_icon_color",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .squishable(enabled = true, scaleDown = 0.985f)
            .clip(MaterialTheme.shapes.extraLarge)
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                    }
                    onEnabledChange(it)
                },
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .graphicsLayer {
                        alpha = lottieAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { context ->
                        LottieAnimationView(context).apply {
                            setAnimation(R.raw.gamebar)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            LottieColorUtils.applyDynamicColors(context, this)
                            repeatCount = LottieDrawable.INFINITE
                            repeatMode = LottieDrawable.RESTART
                            if (enabled) playAnimation()
                        }
                    },
                    update = { view ->
                        if (enabled) {
                            if (!view.isAnimating) view.resumeAnimation()
                        } else {
                            if (view.isAnimating) view.pauseAnimation()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = controlBarColor,
                contentColor = controlBarContentColor,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                rotationZ = emblemRotation
                                scaleX = emblemScale
                                scaleY = emblemScale
                            }
                            .clip(MorphShape(emblemMorph, emblemProgress))
                            .background(emblemColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gamebar),
                            contentDescription = null,
                            tint = emblemIconColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.game_bar_title),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                        Text(
                            text = stringResource(
                                if (enabled) R.string.gamebar_status_active else R.string.gamebar_status_inactive
                            ),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = null,
                        thumbContent = {
                            Icon(
                                imageVector = if (enabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ModernSettingsCard(
    title: String,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    badgePolygon: RoundedPolygon = MaterialShapes.Cookie9Sided,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                val badgeMorph = remember(badgePolygon) {
                    Morph(MaterialShapes.Circle, badgePolygon)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MorphShape(badgeMorph, 1f))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconPainter != null) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
            }

            content()
        }
    }
}

@Composable
fun ModernSettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    shape: Shape = MaterialTheme.shapes.large,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    val containerColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "switch_row_container",
    )
    val titleColor = if (checked)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (checked)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onCheckedChange(it)
                    },
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = titleColor,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = titleColor,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = titleColor,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = {
                    Crossfade(
                        targetState = checked,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                        label = "switch_thumb_icon",
                    ) { isChecked ->
                        Icon(
                            imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun ModernSettingSlider(
    title: String,
    value: Int,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    units: String = "",
    defaultValue: Int? = null,
    onReset: (() -> Unit)? = null,
    valueLabel: (Int) -> String = { if (units.isNotEmpty()) "$it $units" else it.toString() },
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var lastHapticValue by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        lastHapticValue = value
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            val isNonDefault = defaultValue == null || value != defaultValue
            val pillBg by animateColorAsState(
                targetValue = if (isNonDefault) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "slider_pill_bg",
            )
            val pillText by animateColorAsState(
                targetValue = if (isNonDefault) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "slider_pill_text",
            )

            Surface(
                shape = CircleShape,
                color = pillBg,
            ) {
                Text(
                    text = valueLabel(value),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = pillText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            if (onReset != null && isNonDefault) {
                IconButton(
                    onClick = {
                        scope.launch { haptic.click() }
                        onReset()
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = { newValue ->
                val intValue = newValue.toInt()
                if (intValue != lastHapticValue) {
                    scope.launch {
                        haptic.textureTick()
                    }
                    lastHapticValue = intValue
                }
                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )
    }
}

@Composable
fun ModernSettingSelector(
    title: String,
    selectedValue: String,
    options: List<SelectOption>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: selectedValue

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (options.size in 2..4) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = option.value == selectedValue
                    SegmentedButton(
                        selected = isSelected,
                        onClick = {
                            if (enabled && !isSelected) {
                                scope.launch { haptic.click() }
                                onValueSelected(option.value)
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = option.label,
                            style = if (isSelected) MaterialTheme.typography.labelMediumEmphasized
                                else MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            Box {
                Surface(
                    onClick = {
                        if (enabled) {
                            scope.launch { haptic.tick() }
                            expanded = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    enabled = enabled,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            modifier = Modifier.weight(1f),
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    options.forEach { option ->
                        val isSelected = option.value == selectedValue
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    style = if (isSelected) MaterialTheme.typography.bodyMediumEmphasized
                                        else MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                scope.launch { haptic.click() }
                                expanded = false
                                onValueSelected(option.value)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSettingActionRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    valueBadge: String? = null,
    colorPreview: Color? = null,
    shape: Shape = MaterialTheme.shapes.large,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = {
            scope.launch { haptic.click() }
            onClick()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (colorPreview != null || valueBadge != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (colorPreview != null) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colorPreview),
                            )
                        }
                        if (valueBadge != null) {
                            Text(
                                text = valueBadge,
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ModernConfirmDialog(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Info,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            val dialogMorph = remember {
                Morph(MaterialShapes.Circle, MaterialShapes.PuffyDiamond)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MorphShape(dialogMorph, 1f))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}


@Composable
fun HomeMenuCard(
    title: String,
    summary: String,
    iconRes: Int,
    badgePolygon: RoundedPolygon = MaterialShapes.Sunny,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    onClick: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .squishable(enabled = true, scaleDown = 0.95f),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        onClick = {
            scope.launch { haptic.click() }
            onClick()
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val badgeMorph = remember(badgePolygon) {
                Morph(MaterialShapes.Circle, badgePolygon)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MorphShape(badgeMorph, 1f))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ConnectedHomeMenuGrid(
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HomeMenuCard(
                    title = stringResource(R.string.tab_features),
                    summary = "Stats & methods",
                    iconRes = R.drawable.ic_nav_features,
                    badgePolygon = MaterialShapes.Sunny,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                    onClick = { onNavigate(1) },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                HomeMenuCard(
                    title = stringResource(R.string.tab_customization),
                    summary = "Customizations",
                    iconRes = R.drawable.ic_nav_customization,
                    badgePolygon = MaterialShapes.Cookie9Sided,
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                    onClick = { onNavigate(2) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HomeMenuCard(
                    title = stringResource(R.string.tab_records),
                    summary = "FPS tracking logs",
                    iconRes = R.drawable.ic_nav_fps_record,
                    badgePolygon = MaterialShapes.Clover4Leaf,
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 6.dp),
                    onClick = { onNavigate(3) },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                HomeMenuCard(
                    title = stringResource(R.string.tab_presets),
                    summary = "App configs",
                    iconRes = R.drawable.ic_nav_presets,
                    badgePolygon = MaterialShapes.SoftBurst,
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 24.dp),
                    onClick = { onNavigate(4) },
                )
            }
        }
    }
}
