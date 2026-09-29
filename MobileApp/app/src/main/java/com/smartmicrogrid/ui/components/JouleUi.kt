// ============================================================
// File: JouleUi.kt
// Purpose: The mobile app's shared design-system components (2026-09
//          premium revamp): cards, section headers, metric tiles,
//          status pills, date blocks, buttons, text fields, segmented
//          control, empty states and banners. Every screen composes
//          these instead of hand-styling Material defaults, so spacing,
//          radii, borders and elevation stay consistent app-wide.
//          Colors come only from ui/theme/Color.kt (Stripe tokens).
//          JouleCards.kt's older StatTray/SectionCard/IconTile remain
//          for any screen not yet migrated.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmicrogrid.ui.theme.NumericStyle
import com.smartmicrogrid.ui.theme.StripeBody
import com.smartmicrogrid.ui.theme.StripeBorder
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeError
import com.smartmicrogrid.ui.theme.StripeErrorContainer
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripeMuted
import com.smartmicrogrid.ui.theme.StripeOnErrorContainer
import com.smartmicrogrid.ui.theme.StripeOnPrimary
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeSuccess
import com.smartmicrogrid.ui.theme.StripeSuccessContainer
import com.smartmicrogrid.ui.theme.StripeSurface
import com.smartmicrogrid.ui.theme.StripeTray
import com.smartmicrogrid.ui.theme.StripeWarning
import com.smartmicrogrid.ui.theme.StripeWarningContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val CardShape = RoundedCornerShape(14.dp)
val ControlShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(50)

// Blue-tinted, low elevation - docs/stripe.design.md: "keep shadows blue-tinted and subtle".
private val ShadowTint = Color(0xFF31315D)

fun Modifier.softShadow(shape: Shape = CardShape, elevation: Dp = 6.dp): Modifier =
    this.shadow(elevation = elevation, shape = shape, ambientColor = ShadowTint.copy(alpha = 0.10f), spotColor = ShadowTint.copy(alpha = 0.10f))

// White surface, 1dp hairline, soft shadow. Pass onClick to make the whole card tappable.
@Composable
fun JouleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .softShadow()
        .clip(CardShape)
        .background(StripeSurface)
        .border(1.dp, StripeBorder, CardShape)
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(contentPadding),
        content = content,
    )
}

// Section title with an optional trailing text action ("See all").
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = StripeInk)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StripeMuted)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = StripePrimary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

// Compact metric: small tinted icon chip, tabular number, full-width label (wraps, never truncates).
@Composable
fun MetricTile(
    label: String,
    value: Int?,
    icon: ImageVector,
    accent: Color,
    accentSoft: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .softShadow()
            .clip(CardShape)
            .background(StripeSurface)
            .border(1.dp, StripeBorder, CardShape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = value?.toString() ?: "0",
                style = NumericStyle.copy(fontSize = 26.sp, lineHeight = 30.sp),
                color = StripeInk,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = StripeBody,
            maxLines = 2,
            minLines = 2,
        )
    }
}

// Status → (text, fg, bg). Covers reservation and account statuses used across the app.
private fun statusColors(status: String): Triple<String, Color, Color> = when (status) {
    "Confirmed", "Active", "Available" -> Triple(status, StripePrimary, StripeBrandVioletSoft)
    "Completed" -> Triple(status, StripeSuccess, StripeSuccessContainer)
    "PendingActivation" -> Triple("Pending", StripeWarning, StripeWarningContainer)
    "Pending" -> Triple(status, StripeWarning, StripeWarningContainer)
    "Cancelled", "Deactivated", "Inactive" -> Triple(status, StripeError, StripeErrorContainer)
    else -> Triple(status, StripeBody, StripeTray)
}

@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val (text, fg, bg) = statusColors(status)
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = fg)
    }
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("dd", Locale.ENGLISH).withZone(ZoneId.systemDefault())
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH).withZone(ZoneId.systemDefault())

// Calendar-style leader for list rows: day number over a short month.
@Composable
fun DateBlock(iso: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val instant = runCatching { Instant.parse(iso) }.getOrNull()
    Column(
        modifier = modifier
            .size(width = 48.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) StripePrimary else StripeBrandVioletSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = instant?.let { DAY_FORMAT.format(it) } ?: "--",
            style = NumericStyle.copy(fontSize = 18.sp, lineHeight = 20.sp),
            color = if (highlight) StripeOnPrimary else StripeInk,
        )
        Text(
            text = instant?.let { MONTH_FORMAT.format(it).uppercase() } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) StripeOnPrimary.copy(alpha = 0.85f) else StripePrimary,
        )
    }
}

@Composable
fun JoulePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = StripePrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = StripeOnPrimary,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = StripeOnPrimary.copy(alpha = 0.9f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = StripeOnPrimary, strokeWidth = 2.dp)
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun JouleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = StripeInk,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, StripeBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = StripeSurface, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.height(52.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// Labeled field (label above, never placeholder-only) with an optional error line below.
@Composable
fun JouleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = StripeInk)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = StripeMuted) } },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = StripeMuted, modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailing,
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            isError = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = StripeInk),
            shape = ControlShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = StripeSurface,
                focusedContainerColor = StripeSurface,
                disabledContainerColor = StripeTray,
                unfocusedBorderColor = StripeBorder,
                focusedBorderColor = StripePrimary,
                errorBorderColor = StripeError,
                cursorColor = StripePrimary,
            ),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = StripeError)
        }
    }
}

// Pill segmented control - docs/stripe.design.md's segmented-control token.
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(StripeTray)
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(if (isSelected) StripeSurface else Color.Transparent, tween(200), label = "seg-bg")
            val fg by animateColorAsState(if (isSelected) StripeInk else StripeBody, tween(200), label = "seg-fg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.softShadow(PillShape, 2.dp) else Modifier)
                    .clip(PillShape)
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
            }
        }
    }
}

// Selectable chip for filter rows.
@Composable
fun JouleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) StripeInk else StripeSurface, tween(180), label = "chip-bg")
    val fg by animateColorAsState(if (selected) StripeOnPrimary else StripeBody, tween(180), label = "chip-fg")
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, if (selected) StripeInk else StripeBorder, PillShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

// Circular 44dp icon button on a soft surface (header actions).
@Composable
fun JouleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = StripeInk,
    background: Color = StripeSurface,
    bordered: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, StripeBorder, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(StripeBrandVioletSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = StripePrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = StripeInk)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = StripeBody,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            JoulePrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.height(44.dp))
        }
    }
}

// Compact inline error with an optional dismiss "×".
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(StripeErrorContainer)
            .border(1.dp, StripeError.copy(alpha = 0.2f), ControlShape)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(StripeError))
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = StripeOnErrorContainer,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Text(
                "×",
                style = MaterialTheme.typography.titleMedium,
                color = StripeOnErrorContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

// Key/value row used inside cards (profile details, booking details).
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(StripeTray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = StripeBody, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StripeMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = StripeInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(StripeBorder))
}

// Row container for header trailing actions.
@Composable
fun HeaderActions(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

// Kept for API parity with Surface-based callers that want the card shape without the shadow.
@Composable
fun FlatCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(shape = CardShape, color = StripeSurface, border = BorderStroke(1.dp, StripeBorder), modifier = modifier.fillMaxWidth(), content = content)
}
