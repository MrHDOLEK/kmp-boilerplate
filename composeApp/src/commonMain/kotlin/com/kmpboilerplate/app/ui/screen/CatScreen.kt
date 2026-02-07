package com.kmpboilerplate.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmpboilerplate.app.ui.component.*
import com.kmpboilerplate.app.ui.layout.AppLayout
import com.kmpboilerplate.application.action.GetCatTagsAction
import com.kmpboilerplate.application.action.GetCatsAction
import com.kmpboilerplate.application.action.GetRandomCatAction
import com.kmpboilerplate.domain.entity.Cat
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CatScreen(
    getRandomCat: GetRandomCatAction = koinInject(),
    getCats: GetCatsAction = koinInject(),
    getCatTags: GetCatTagsAction = koinInject(),
) {
    var cats by remember { mutableStateOf<List<Cat>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadCats() {
        scope.launch {
            isLoading = true
            error = null
            getCats(tags = selectedTag?.let { listOf(it) }, limit = 20)
                .onSuccess { cats = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadTags() {
        scope.launch {
            getCatTags()
                .onSuccess { tags = it.take(20) }
        }
    }

    fun loadRandomCat() {
        scope.launch {
            isLoading = true
            error = null
            getRandomCat()
                .onSuccess { cats = listOf(it) }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadTags()
        loadCats()
    }

    LaunchedEffect(selectedTag) {
        loadCats()
    }

    AppLayout(
        title = "Cats",
        actions = {
            TextButton(onClick = { loadRandomCat() }) {
                Text("Random")
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
                    onSelect = { selectedTag = it },
                    labelSelector = { it },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            error?.let {
                ErrorState(
                    message = it,
                    onRetry = { loadCats() },
                )
            }

            if (isLoading) {
                LoadingState()
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(cats) { cat ->
                    ImageTile(
                        imageUrl = cat.imageUrl,
                        contentDescription = "Cat",
                        badge = { TagBadge(cat.tags) },
                    )
                }
            }
        }
    }
}
