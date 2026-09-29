// ============================================================
// File: JouleBottomBar.kt
// Purpose: The app's bottom navigation bar (2026-09 premium revamp),
//          replacing Material3's stock NavigationBar: white surface
//          with a hairline + soft upward shadow, and the selected tab
//          shown as a violet icon inside an animated soft-violet pill
//          with its label darkened - the unselected tabs stay quiet.
//          Pads itself for the system navigation bar (edge-to-edge).
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.ui.components.Hairline
import com.smartmicrogrid.ui.theme.StripeBrandVioletSoft
import com.smartmicrogrid.ui.theme.StripeInk
import com.smartmicrogrid.ui.theme.StripeMuted
import com.smartmicrogrid.ui.theme.StripePrimary
import com.smartmicrogrid.ui.theme.StripeSurface

data class BottomBarItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun JouleBottomBar(items: List<BottomBarItem>, currentRoute: String?, onSelect: (BottomBarItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, ambientColor = Color(0xFF31315D).copy(alpha = 0.10f), spotColor = Color(0xFF31315D).copy(alpha = 0.10f))
            .background(StripeSurface),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { item ->
                BarTab(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BarTab(item: BottomBarItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pillColor by animateColorAsState(if (selected) StripeBrandVioletSoft else Color.Transparent, tween(220), label = "tab-pill")
    val iconColor by animateColorAsState(if (selected) StripePrimary else StripeMuted, tween(220), label = "tab-icon")
    val labelColor by animateColorAsState(if (selected) StripeInk else StripeMuted, tween(220), label = "tab-label")
    val pillWidth by animateDpAsState(if (selected) 56.dp else 40.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "tab-width")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(pillWidth)
                .clip(RoundedCornerShape(50))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = labelColor, maxLines = 1)
    }
}
