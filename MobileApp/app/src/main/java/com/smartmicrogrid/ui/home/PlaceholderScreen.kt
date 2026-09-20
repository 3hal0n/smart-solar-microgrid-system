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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Renders a centered "not built yet" placeholder naming the screen and its eventual owner.
@Composable
fun PlaceholderScreen(title: String, owner: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Coming soon — owned by $owner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
