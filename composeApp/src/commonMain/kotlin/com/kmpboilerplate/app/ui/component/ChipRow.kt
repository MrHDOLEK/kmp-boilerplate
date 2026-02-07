package com.kmpboilerplate.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ChipRow(
    items: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    labelSelector: (T) -> String,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(bottom = 16.dp),
    ) {
        items(items) { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelect(if (selected == item) null else item) },
                label = { Text(labelSelector(item)) },
            )
        }
    }
}
