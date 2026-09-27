package com.kmpboilerplate.app.ui.component.cat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kmpboilerplate.app.resources.Res
import com.kmpboilerplate.app.resources.cat_image_description
import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatTile(
    cat: CatViewModel,
    modifier: Modifier = Modifier,
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
                model = cat.imageUrl,
                contentDescription = stringResource(Res.string.cat_image_description),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            TagBadge(
                tags = cat.tags,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
            )
        }
    }
}
