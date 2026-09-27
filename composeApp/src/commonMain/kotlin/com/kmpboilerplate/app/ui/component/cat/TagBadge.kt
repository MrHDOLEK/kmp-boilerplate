package com.kmpboilerplate.app.ui.component.cat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val VISIBLE_TAG_COUNT = 2
private const val BADGE_OPACITY = 0.85f

@Composable
fun TagBadge(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isNotEmpty()) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = BADGE_OPACITY),
        ) {
            Text(
                text = tags.take(VISIBLE_TAG_COUNT).joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
