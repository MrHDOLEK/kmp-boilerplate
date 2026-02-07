# KMP Boilerplate

Kotlin Multiplatform application with Compose UI for Android, iOS and Desktop.

---

#### A KMP project using simplified Hexagonal Architecture

## Project setup

### Requirements
- JDK 24 (Temurin recommended)
- Android Studio / IntelliJ IDEA
- Xcode (for iOS)

### Run Desktop

```bash
./gradlew :composeApp:run
```

### Run Android

```bash
./gradlew :composeApp:installDebug
```

### Run iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run.

### Run all tests

```bash
./gradlew allTests
```

### Run linter

```bash
./gradlew detekt
```

### Run code style check

```bash
./gradlew csCheck
```

### Fix code style

```bash
./gradlew csFix
```

### List all available tasks

```bash
./gradlew tasks --group=verification
```

## Architecture

```
shared/                          # Backend (business logic)
├── domain/                      # Entities & Repository interfaces
│   ├── entity/
│   └── repository/
├── application/                 # Actions (use cases)
│   └── action/
└── infrastructure/              # Implementations
    ├── config/                  # DI Container & Bootstrap
    ├── http/                    # HTTP client
    └── repository/              # Repository implementations

composeApp/                      # Frontend (UI)
└── ui/
    ├── layout/                  # Base layouts (like Twig templates)
    ├── component/               # Reusable components
    └── screen/                  # Screens
```

## Some examples

### Creating a new Action

```kotlin
package com.kmpboilerplate.application.action

class GetCatsAction(
    private val repository: CatRepositoryInterface
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<Cat>> {
        return try {
            Result.success(repository.getCats(limit = limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Creating a new Repository

```kotlin
// Domain - interface
package com.kmpboilerplate.domain.repository

interface CatRepositoryInterface {
    suspend fun getRandomCat(): Cat
    suspend fun getCats(limit: Int): List<Cat>
}

// Infrastructure - implementation
package com.kmpboilerplate.infrastructure.repository

class CatRepository(
    private val client: HttpClient
) : CatRepositoryInterface {

    override suspend fun getRandomCat(): Cat {
        return client.get("$BASE_URL/cat?json=true").body()
    }

    override suspend fun getCats(limit: Int): List<Cat> {
        return client.get("$BASE_URL/cats?limit=$limit").body()
    }
}
```

### Registering dependencies in Container

```kotlin
// infrastructure/config/Container.kt

val repositoryModule = module {
    singleOf(::CatRepository) bind CatRepositoryInterface::class
}

val actionModule = module {
    factoryOf(::GetCatsAction)
}

val container = listOf(networkModule, repositoryModule, actionModule)
```

### Creating a new Screen

```kotlin
@Composable
fun CatScreen(
    getCats: GetCatsAction = koinInject()
) {
    var cats by remember { mutableStateOf<List<Cat>>(emptyList()) }

    LaunchedEffect(Unit) {
        getCats(limit = 20).onSuccess { cats = it }
    }

    AppLayout(title = "Cats") { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(cats) { cat ->
                ImageTile(imageUrl = cat.imageUrl)
            }
        }
    }
}
```

### Creating reusable Component

```kotlin
@Composable
fun ImageTile(
    imageUrl: String,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentScale = ContentScale.Crop
            )
            badge()
        }
    }
}
```

## Code quality

This project enforces a strict coding standard using two tools:

### Detekt (static analysis)
- Zero tolerance policy (`maxIssues: 0`) — any issue breaks the build
- Dead code detection: `UnusedPrivateMember`, `UnusedPrivateClass`, `UnusedImports`
- Redundant code: `RedundantVisibilityModifierRule`, `UnnecessaryAbstractClass`
- Design rules: `UseDataClass`, `UtilityClassWithPublicConstructor`
- Compose rules via [compose-rules](https://github.com/mrmans0n/compose-rules) plugin
- Config: [`detekt.yml`](detekt.yml)

### ktlint (code formatter)
- Automatic code formatting to a single standard (`ktlint_official`)
- Run `./gradlew csFix` to auto-fix all formatting issues
- Run `./gradlew csCheck` to verify without modifying files
- Config: [`.editorconfig`](.editorconfig)

## Documentation

Learn more at these links:

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Koin](https://insert-koin.io/)
- [Ktor Client](https://ktor.io/docs/client.html)
- [Kotlinx Serialization](https://kotlinlang.org/docs/serialization.html)
- [Detekt](https://detekt.dev/)
- [ktlint](https://pinterest.github.io/ktlint/)
