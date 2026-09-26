// ============================================================
// File: PlaceholderScreen.kt
// Purpose: Shared "not built yet" content for a Home destination
//          whose real screen belongs to another owner. Keeps each
//          actual stub file (in ui/prosumer, ui/operator,
//          ui/dashboard) to a one-line body, so swapping a
//          placeholder for the real screen later is a small, obvious
//          diff in that owner's own file — nothing here needs to
//          change when that happens.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.ui.components.IconTile
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.components.OuterShape

// Renders a centered "not built yet" empty-state card naming the screen and its eventual owner.
@Composable
fun PlaceholderScreen(title: String, owner: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = OuterShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(icon = JouleIcons.Grid, size = 48.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "Coming soon — owned by $owner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
