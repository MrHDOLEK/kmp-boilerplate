package com.kmpboilerplate.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImageTile(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {},
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
            ) {
                badge()
            }
        }
    }
}

@Composable
fun TagBadge(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isNotEmpty()) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ) {
            Text(
                text = tags.take(2).joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
