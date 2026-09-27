package com.kmpboilerplate.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmpboilerplate.app.resources.Res
import com.kmpboilerplate.app.resources.cats_load_failed
import com.kmpboilerplate.app.resources.cats_title
import com.kmpboilerplate.app.resources.random_cat
import com.kmpboilerplate.app.ui.component.cat.CatTile
import com.kmpboilerplate.app.ui.component.common.ChipRow
import com.kmpboilerplate.app.ui.component.common.ErrorState
import com.kmpboilerplate.app.ui.component.common.LoadingState
import com.kmpboilerplate.app.ui.layout.AppLayout
import com.kmpboilerplate.application.action.cat.GetCatTagsAction
import com.kmpboilerplate.application.action.cat.GetCatsAction
import com.kmpboilerplate.application.action.cat.GetRandomCatAction
import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun CatScreen(
    getRandomCat: GetRandomCatAction = koinInject(),
    getCats: GetCatsAction = koinInject(),
    getCatTags: GetCatTagsAction = koinInject(),
) {
    var cats by remember { mutableStateOf<List<CatViewModel>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showsRandomCat by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var catsFailed by remember { mutableStateOf(false) }
    var tagsFailed by remember { mutableStateOf(false) }

    LaunchedEffect(attempt) {
        if (tags.isEmpty()) {
            tagsFailed = false
            getCatTags()
                .onSuccess { loaded -> tags = loaded }
                .onFailure { tagsFailed = true }
        }
    }

    LaunchedEffect(selectedTag, showsRandomCat, attempt) {
        isLoading = true
        catsFailed = false
        val loaded = if (showsRandomCat) getRandomCat().map { cat -> listOf(cat) } else getCats(selectedTag)
        loaded
            .onSuccess { found -> cats = found }
            .onFailure { catsFailed = true }
        isLoading = false
    }

    AppLayout(
        title = stringResource(Res.string.cats_title),
        actions = {
            TextButton(
                onClick = {
                    showsRandomCat = true
                    attempt++
                },
            ) {
                Text(stringResource(Res.string.random_cat))
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            if (tags.isNotEmpty()) {
                ChipRow(
                    items = tags,
                    selected = selectedTag,
                    onSelect = { tag ->
                        selectedTag = tag
                        showsRandomCat = false
                    },
                    labelSelector = { tag -> tag },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (catsFailed || tagsFailed) {
                ErrorState(
                    message = stringResource(Res.string.cats_load_failed),
                    onRetry = { attempt++ },
                )
            }
            if (isLoading) {
                LoadingState()
            }
            CatGrid(cats = cats)
        }
    }
}

@Composable
private fun CatGrid(
    cats: List<CatViewModel>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(cats, key = { cat -> cat.id }) { cat ->
            CatTile(cat = cat)
        }
    }
}
