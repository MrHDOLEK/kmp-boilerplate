# KMP Boilerplate

A Kotlin Multiplatform starting point with a Compose Multiplatform UI for **Android, iOS and desktop**
(JVM). It ships one small feature — a gallery of cats from [cataas.com](https://cataas.com) — built the
way every feature in a project started from it is meant to be built.

This README is the project's standard. The rules below are written down for the people who work on the
code; the quality gates (ktlint, detekt and the Konsist rules in `:architecture`) enforce most of them,
and [What no gate checks](#what-no-gate-checks) lists the rest.

## Contents

- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Architecture](#architecture)
- [Dependency injection](#dependency-injection)
- [User interface](#user-interface)
- [Platform abstraction](#platform-abstraction)
- [Key conventions](#key-conventions)
- [Compose practices](#compose-practices)
- [Quality gates](#quality-gates)
- [What :architecture checks](#what-architecture-checks)
- [What no gate checks](#what-no-gate-checks)

## Getting started

### Requirements

- JDK 24 (Temurin recommended) — every module compiles with `jvmToolchain(24)`
- Android SDK with platform-tools, the emulator and at least one AVD
- Xcode with an iOS simulator (macOS only)
- [`just`](https://github.com/casey/just), the task runner

The `justfile` exports `ANDROID_HOME`, defaulting to `~/Library/Android/sdk`, so Android tasks work
without a `local.properties`.

### Everyday commands

`just` is the entry point for every local command. Run `just` on its own to list every recipe.

| Need | Recipe |
|---|---|
| Check the toolchain | `just doctor` |
| Download dependencies | `just deps` |
| Run the app | `just run-android` / `just run-ios` / `just run-desktop` |
| Test one platform | `just test-android` / `just test-ios` / `just test-desktop` |
| Run every test target | `just test` |
| Compile fastest (common metadata only) | `just precompile-common` |
| Compile one platform | `just precompile-android` / `just precompile-ios` / `just precompile-desktop` |
| Compile everything | `just precompile` |
| Run the three quality gates | `just check` |
| Format, then run the gates | `just fix` |
| Everything CI runs | `just ci` |

Pick another device per invocation: `just ios_sim="iPhone 16" run-ios`, `just avd=Pixel_7 run-android`.

### Enable the pre-commit hook

Once per clone:

```bash
git config core.hooksPath .githooks
```

From then on, a commit that touches Kotlin, a Gradle script or the version catalog runs the gates first
(see [Quality gates](#quality-gates)).

## Project structure

Three modules: `shared` holds everything but the UI, `composeApp` holds the UI and the platform entry
points, and `architecture` holds only the tests that guard the other two. The iOS app shell lives in
`iosApp/`. The root package is `com.kmpboilerplate`.

```
shared/src/
├── commonMain/kotlin/com/kmpboilerplate/
│   ├── domain/                   # split by area — one feature is one folder
│   │   ├── cat/
│   │   │   ├── Cat.kt                        # the entity
│   │   │   ├── CatService.kt                 # the area's business logic
│   │   │   └── CatRepositoryInterface.kt     # its persistence contract
│   │   └── shared/
│   │       └── ResultOf.kt                   # resultOf — runCatching that rethrows cancellation
│   ├── application/
│   │   ├── action/cat/           # one use case per file: GetCatsAction, GetRandomCatAction, GetCatTagsAction
│   │   └── viewmodel/
│   │       ├── cat/CatViewModel.kt
│   │       └── mapper/cat/CatViewModelMapper.kt
│   └── infrastructure/
│       ├── cataas/               # the cataas.com adapter
│       │   ├── CatRepository.kt
│       │   ├── CataasBaseUrl.kt
│       │   ├── dto/CatDto.kt
│       │   └── mapper/CatDtoMapper.kt
│       ├── config/               # Bootstrap.kt, Container.kt (Koin)
│       └── http/                 # CreateHttpClient.kt (Ktor)
└── commonTest/kotlin/com/kmpboilerplate/   # mirrors the packages above; FakeCatRepository sits in domain/cat

composeApp/src/
├── commonMain/
│   ├── composeResources/
│   │   ├── values/strings.xml       # English
│   │   └── values-pl/strings.xml    # Polish
│   └── kotlin/com/kmpboilerplate/app/
│       ├── App.kt                   # root composable with KoinContext
│       └── ui/
│           ├── layout/AppLayout.kt
│           ├── component/
│           │   ├── cat/             # CatTile, TagBadge
│           │   └── common/          # ChipRow, ErrorState, LoadingState
│           └── screen/CatScreen.kt
├── androidMain/   # KmpBoilerplateApplication (starts Koin), MainActivity, AndroidManifest.xml, res/
├── iosMain/       # MainViewController, StartDependencyInjection
└── desktopMain/   # Main.kt (starts Koin, opens the window)

architecture/src/test/kotlin/com/kmpboilerplate/architecture/   # the Konsist rules and their helpers

iosApp/iosApp/     # iOSApp.swift (starts Koin in init), ContentView.swift
```

The domain, `application/action`, `application/viewmodel` and `application/viewmodel/mapper` are split
into the **same** areas, so a feature is one folder name across all four layers. A new file goes into
the area it belongs to, never loose at the root of a layer. `ui/component/` follows the areas of
`application/viewmodel`, plus `common/` for what every area draws with.

An area may be missing from a layer only when that layer has nothing to put there. Such an absence is
declared in `ABSENT` in `AreaStructureTest`, in the same change that adds the area elsewhere. Today
every area is in all four layers.

## Architecture

Simplified hexagonal architecture: dependencies point inwards, towards the domain.

```
composeApp (UI)  →  application (actions, view models)  →  domain  ←  infrastructure (adapters, config)
```

### Domain

Pure Kotlin: it may import only `kotlin`, `kotlinx.coroutines`, `kotlinx.datetime` (once added) and itself — no
Ktor, no serialization, no Compose. It is packaged **by feature**, so one area folder holds these kinds
of file side by side:

- **Entities** — data classes such as `Cat`: the facts the feature is about.
- **Collections and read models** — a `CatCollection` when a list carries operations of its own, a
  read model when a service assembles several facts into the one thing a screen reads. A plain
  `List<Cat>` is fine until there is a real operation to put on it:

  ```kotlin
  class CatCollection(private val cats: List<Cat>) : List<Cat> by cats {
      fun filterByTag(tag: String): CatCollection = CatCollection(cats.filter { tag in it.tags })
  }
  ```

- **Values** — what those facts are made of, in one of two folders: `<area>/enum/` for enumerations and
  `<area>/valueObject/` for every other value (identifiers, dates, windows, sealed variants). No other
  kind folder exists below a domain area. A comparator is a rule, not a value, so it sits beside the
  facts.
- **Services** — the business logic. Anything that filters, sorts, aggregates, resolves a fallback or
  applies a rule lives here, never in an action, a repository or a screen. Policy that looks like
  presentation is still policy: `CatService` owns the page size, the tag cap and the removal of blank
  tags from the third-party payload.
- **Contracts** — the persistence contract, named `{Noun}RepositoryInterface`, and any **port**: what
  the domain needs from outside that is not persistence (a clock, a time zone, a location source),
  named `{Noun}Interface`. A contract sits in the area that needs it, or in `domain/shared/` when more
  than one area does.

The folder does not separate these kinds, so **the name does**, and the gates read it:

| Kind | Name |
|---|---|
| Service | ends in `Service`, `Factory`, `Resolver`, `Converter` or `Mapper` |
| Persistence contract | ends in `RepositoryInterface` |
| Port | ends in `Interface` |
| Entity, collection, value | none of the above |

A domain class that takes a contract, a port or a service in its constructor is a service and carries
one of those suffixes. An entity names none of them — not by import and not as a neighbour in its
package.

`domain/shared/` holds what every area speaks and no area owns. Today that is `ResultOf.kt` alone.

### Application

An **action** is one use case in one file, named `{Verb}{Noun}Action`, with an
`operator fun invoke` that returns `Result<T>`. An action contains no business logic. It may only:

1. validate its input,
2. delegate to a domain service,
3. map the service's answer to a ViewModel.

It never injects a repository or a port, and it never returns a domain entity: the UI never sees the
domain. ViewModels live in `application/viewmodel/<area>/`, their mappers in
`application/viewmodel/mapper/<area>/`.

```kotlin
class GetCatsAction(
    private val catService: CatService,
    private val mapper: CatViewModelMapper,
) {
    suspend operator fun invoke(tag: String? = null): Result<List<CatViewModel>> =
        resultOf { mapper.mapCollection(catService.getCats(tag)) }
}
```

Every `invoke` builds its result with `resultOf` (`domain/shared/ResultOf.kt`) — never `runCatching`
and never a `try`/`catch`. `runCatching` catches `Throwable`, including the `CancellationException` of
a cancelled coroutine: swallowed, leaving a screen becomes a success carrying nothing. `resultOf`
rethrows the cancellation and packs everything else into the `Result`.

A ViewModel mapper maps and nothing more: it names no service or repository, suspends nothing, and
imports only Kotlin, `kotlinx.datetime` (once added), the domain and the application.

### Infrastructure

- **Adapters.** A wire format lives in its own folder with its client, its DTOs and its mapper:
  `infrastructure/cataas/`. A new adapter is added to `InfrastructureRole.ADAPTERS` in `:architecture`
  together with its folder.
- **Repositories** implement the domain contracts. One that speaks a wire format sits beside its
  adapter; one over a local store sits under `infrastructure/repository/<area of its contract>/`. A
  repository reads and writes; it names no domain service and no clock — the service that runs it
  passes the moment in. Its name carries no technology: `CatRepository`, never `KtorCatRepository` or
  `InMemoryCatRepository`.
- **DTOs** carry data across a boundary and nothing more. They are named `*Dto` (or are
  `@Serializable`) and sit in the adapter's `dto/` folder.
- **Mappers** convert between DTOs and entities, one per direction pair, in the adapter's `mapper/`
  folder. There is no top-level `infrastructure/dto/` or `infrastructure/mapper/`.
- **HTTP** is Ktor. `createHttpClient()` is one common function; the engine comes from each source
  set's dependency (OkHttp on Android and desktop, Darwin on iOS).

## Dependency injection

Koin. Every module is defined in `infrastructure/config/Container.kt` and listed in `container`, which
`bootstrap()` installs:

```kotlin
val container = listOf(httpModule, repositoryModule, serviceModule, mapperModule, actionModule)
```

| Definition | Used for |
|---|---|
| `single` / `singleOf` | the HTTP client, repositories, and **anything that keeps state** — a `var`, or a mutable flow, state or collection |
| `factory` / `factoryOf` | actions (always), services, mappers |
| `bind Interface::class` | every implementation of a domain contract |

Every new action, service, mapper and repository **must** be registered: an unregistered dependency
fails at runtime, not at compile time. A stateful class registered as a `factory` compiles too, and
then hands every screen its own empty copy.

A module that needs a platform is an `expect fun x(): Module` in commonMain with an `actual` on every
platform, called from `container`.

Koin starts **once per process, before any UI exists**: `KmpBoilerplateApplication.onCreate` on
Android, `main()` before `application { }` on desktop, and `iOSApp.init()` on iOS through
`startDependencyInjection()`. `MainActivity`, the desktop window and `MainViewController` only render
`App()`. `bootstrap()` is idempotent as a second line of defence.

## User interface

A three-tier model inspired by template inheritance: **layout → component → screen**.

- **Layouts** (`ui/layout/`) are the scaffolds a screen fills. `AppLayout` provides the top bar, its
  actions, the floating action button slot and the content padding.
- **Components** (`ui/component/<area>/`) are state-hoisted composables: they take values and
  callbacks. A component never injects a dependency, never names Koin and never calls a use case.
- **Screens** (`ui/screen/`) are pages. A screen injects its actions as `koinInject()` default
  parameters, which keeps it testable, loads data in a `LaunchedEffect`, and composes a layout with
  components. `ui/screen/` holds `*Screen` files and nothing else.

```kotlin
@Composable
fun CatScreen(getCats: GetCatsAction = koinInject()) {
    var cats by remember { mutableStateOf<List<CatViewModel>>(emptyList()) }
    LaunchedEffect(Unit) {
        getCats().onSuccess { loaded -> cats = loaded }
    }
    AppLayout(title = stringResource(Res.string.cats_title)) { padding -> CatGrid(cats, Modifier.padding(padding)) }
}
```

A screen holds UI state and reacts to results; it never filters, sorts or transforms domain data. The
UI reaches infrastructure only through `config.bootstrap`, and never names the domain.

**Text a user reads comes from composeResources**: `values/strings.xml` in English and
`values-pl/strings.xml` in Polish, with the same keys in both. Production Kotlin is English and plain
ASCII, and the domain never assembles a sentence — it carries the facts, and the screen writes the
words in the device's language. A product name that is not translated, such as the desktop window
title, may stay a constant.

## Platform abstraction

Three actual source sets: `androidMain`, `iosMain` and `desktopMain`. Every `expect` declaration needs
an `actual` in **all three**. A missing one breaks only the build of that target, which a local build of
one platform never notices; `PlatformActualTest` catches it locally. The boilerplate declares no
`expect` today.

## Key conventions

### Files

- **One public type per file**, and the file is named after what it declares — `createHttpClient()` in
  `CreateHttpClient.kt`, `resultOf` in `ResultOf.kt`, `CATAAS_BASE_URL` in `CataasBaseUrl.kt`.
  Containment either way counts, and a platform suffix is not part of the name. `private` and
  `internal` helpers, SCREAMING_SNAKE constants and `Container.kt` are outside the rule.
- **The package matches the folder.** `shared` holds `domain`, `application` and `infrastructure`;
  `composeApp` holds `com.kmpboilerplate.app`.

### Functions

**Expression body when the whole body is one expression.** A function whose body would be nothing but
`return <expression>` is written with `=`; everything else keeps its braces. ktlint's
`function-expression-body` rule enforces it, and `just fmt` rewrites it.

```kotlin
suspend fun getRandomCat(): Cat = catRepository.getRandomCat()
```

### Comments

**No `//` or `/* */` comment in production code.** If a line needs a comment to be understood, the
code is not readable yet: put the meaning in a name, extract a function or a named value, and delete
the rest. A KDoc paragraph is prose like any other comment.

Two comments stay:

- a KDoc made only of block tags that documentation tools read (`@param`, `@property`, `@return`,
  `@throws`, `@see`) on a declaration;
- the reason next to a `@Suppress`, on the annotation's line or in one `//` line directly above it.

Tests and Gradle scripts are outside this rule.

### Language

Code, identifiers and string literals are English. Use full words: shorten only format and protocol
names (`Dto`, `Http`, `Json`), never the project's own concepts — `CatSyncService` and `catCfg` are out.

### Naming

| What | Rule |
|---|---|
| Action | `{Verb}{Noun}Action` — `GetCatsAction` |
| Persistence contract | `{Noun}RepositoryInterface` |
| Port | `{Noun}Interface` |
| Repository | no `Sql`, `InMemory`, `Room`, `Ktor` or `Http` in the name |
| Callback parameter | present tense — `onSelect`, `onRetry`, not `onSelected` |
| Composable that emits UI | PascalCase, one root emitter |

### Testing

Tests live in `shared/src/commonTest`, which runs on every target — desktop, Android and iOS. A test
may sit in `desktopTest` or `androidUnitTest` only if it needs something only the JVM has; one that
could run in commonTest and does not silently drops the iOS coverage. `androidInstrumentedTest`, which
needs a device, is not judged by the gate.

- Test names are backticked sentences starting with `should`.
- Use hand-written fakes (`FakeCatRepository`), never a mocking library. A fake of a domain contract
  lives in commonTest under the package of the interface it implements, so every layer can reuse it.
- Use `runTest` from `kotlinx-coroutines-test` and `kotlin.test` assertions.
- Follow Arrange-Act-Assert:

```kotlin
@Test
fun `should return only the cats of the selected tag`() =
    runTest {
        // Arrange
        val action = GetCatsAction(CatService(FakeCatRepository(cats = cats)), CatViewModelMapper())

        // Act
        val result = action(tag = "funny")

        // Assert
        assertEquals(listOf("2"), result.getOrThrow().map { cat -> cat.id })
    }
```

`composeApp` has no test source set today.

## Compose practices

These are advice, not gates, except where a rule names its checker.

### Responsibilities

`composeApp` is UI only: composables, presentation state and the platform entry points. Everything
else — entities, services, actions, ViewModels, repositories, mappers, configuration — is in `shared`.

### Recomposition phases

Composition (what) → layout (where) → drawing (how). A state read during composition recomposes the
whole scope; read fast-changing state in the layout or draw phase instead:

```kotlin
Modifier.offset(y = -scroll.value.toDp())      // recomposes on every scrolled pixel
Modifier.offset { IntOffset(0, -scroll.value) } // reads it during layout
```

### Stability

Compose skips a composable whose parameters did not change. Stable parameters (`String`, `Int`,
lambdas, `val`-only data classes of a module with the Compose compiler) are compared with `equals`.
`List`, `Map`, interfaces and classes from a module without the Compose compiler are unstable.

`shared` does not apply the Compose compiler, so every ViewModel is unstable to the UI. With strong
skipping — on by default since Kotlin 2.0.20 — a composable with unstable parameters is still
skippable; they are compared by instance (`===`). What defeats it is rebuilding the ViewModel on every
recomposition: hold the instance the action returned, because a fresh `copy()` of an identical value
recomposes every row. Keep ViewModels `val`-only data classes. If recomposition ever shows up as a real
cost, apply the Compose compiler to `shared` and annotate them `@Immutable`; do not move logic into
`composeApp`.

### State

```kotlin
remember(key) { expensive() }                        // recomputes when key changes
remember { derivedStateOf { list.isNotEmpty() } }    // derived from other state
people[0].tagged = true                              // invisible to Compose
people[0] = people[0].copy(tagged = true)            // replaces the element: observed
```

Key the state of a screen on its arguments (`remember(catId)`) when the same composable can be shown
again with different ones.

### Side effects

- `LaunchedEffect(key)` — one-shot, restarts when the key changes.
- `DisposableEffect(key)` — with cleanup in `onDispose { }`.
- `rememberCoroutineScope()` — for callbacks such as `onClick`.
- A lambda passed into a `LaunchedEffect` goes through `rememberUpdatedState` (compose-rules'
  `LambdaParameterInRestartableEffect`).

Never run a side effect in a composable's body; it runs again on every recomposition.

### Modifier order

Modifiers apply outside-in, and order changes behaviour:

```kotlin
Modifier.padding(16.dp).background(Red)   // transparent padding
Modifier.background(Red).padding(16.dp)   // red padding
```

A composable takes `modifier: Modifier = Modifier` and applies it to its root only.

### Performance checklist

1. Measure on a release build; debug has a large overhead.
2. Use the Layout Inspector to find needless recompositions.
3. Give every `items()` call a `key`.
4. Hold the ViewModel instance the action returned.
5. Prefer the lambda `Modifier.offset { }` over the value `.offset()` for animations.
6. Use `remember(key)` for expensive computations and `derivedStateOf` for derived state.

### Multiplatform checklist

1. Every `expect` has an `actual` in `androidMain`, `iosMain` and `desktopMain`.
2. No JVM-only API (`java.*`) in `commonMain`.
3. iOS has no system back button; do not rely on one for navigation.
4. `just precompile` compiles every target.

## Quality gates

Nothing is done until these pass.

| Gate | Recipe | Runs |
|---|---|---|
| Code style (ktlint, every module and script) | `just fmt-check` (`./gradlew csCheck`) | locally, pre-commit hook, CI |
| Static analysis (detekt with compose-rules) | `just lint` (`./gradlew detekt`) | locally, pre-commit hook, CI |
| Architecture (Konsist) | `just arch` (`./gradlew :architecture:test`) | locally, pre-commit hook, CI |
| The three gates at once | `just check` | locally |
| Auto-format | `just fmt` (`./gradlew csFix`) | locally, by hand |
| Format, then the three gates | `just fix` | locally |
| Tests on desktop, Android and iOS | `just test` | locally, CI |
| The gates and every test target | `just ci` | locally |
| Pull request title | — | CI |

- **Pre-commit hook** (`.githooks/pre-commit`) runs `csCheck`, `detekt` and `:architecture:test` when a
  commit adds, modifies, renames or deletes a `.kt`, `.kts` or `.toml` file. It checks the working
  tree, not the staged snapshot.
- **CI** (`.github/workflows/kotlin.yml`) runs on every push to `main` and on every pull request to it
  that is not a draft: one job runs the three gates, and a matrix runs the tests on desktop, Android
  and iOS. `.github/workflows/check-pr-title.yml` checks the pull request title.
- The hook and CI call `./gradlew` directly, so they work on a machine without `just`.

Rules:

- **detekt `maxIssues: 0`.** Any finding fails the build. Fix the code; do not suppress the rule. A
  `@Suppress` says why, and no suppression may switch off an architecture rule, `LongMethod` in
  composeApp or ktlint as a whole.
- **Never edit `detekt.yml` or `.editorconfig` to silence a finding.** Changing the standard is a
  deliberate decision of its own.
- **compose-rules is pinned** to 0.4.28, the last line built for detekt 1.x. `detektComposeCanary`
  lints a probe before every `detekt` run and fails unless `MultipleEmitters`,
  `LambdaParameterInRestartableEffect` and `ParameterNaming` report on it.
- **detekt analyses the main source sets only**, from the list written out in the root
  `build.gradle.kts`; a new source set is not analysed until it is added there. ktlint covers every
  source set and script, tests included. Test code is formatted and held to the test conventions, but
  not statically analysed.
- **Lines are at most 120 characters, functions at most 80 lines** (detekt's `LongMethod`).
- **A new architecture rule lands in the same change as the code that satisfies it** — never as a
  baseline of known violations, and never silenced with a `@Suppress`.

## What :architecture checks

Every rule reads the code as well as the imports, so a fully qualified name, an import alias or a
typealias meets the same prohibition as an import.

| Test | What it holds |
|---|---|
| `LayerDependencyTest` | Dependencies point inwards. The domain uses only `kotlin`, `kotlinx.coroutines`, `kotlinx.datetime` (once added) and itself; the application reaches neither infrastructure nor the UI; infrastructure never reaches the UI. |
| `UserInterfaceBoundaryTest` | composeApp reaches infrastructure only through `config.bootstrap` and never names the domain. The application aliases no domain type. A ViewModel names nothing from the domain. |
| `EntityRuleTest` | An entity names no service, repository contract or port. |
| `NamingTest` | Contracts end in `Interface`, persistence ones in `RepositoryInterface`; a domain class taking a collaborator carries a service suffix; no technology in a repository's name; every use case is an `*Action` with `operator fun invoke`. |
| `ActionRuleTest` | No repository or port in an action. `invoke` returns a `Result` with no domain type in it and builds it with `resultOf`. No `runCatching`, no `catch`. Nothing but `*Action` classes in the action package. |
| `ViewModelMapperTest` | A ViewModel mapper names no service or repository, suspends nothing, and imports only Kotlin, `kotlinx.datetime` (once added), the domain and the application. |
| `RepositoryRuleTest` | A repository, chosen by role, names no domain service or clock, and sits under `repository/<area of its contract>/` or beside its adapter. |
| `InfrastructureStructureTest` | DTOs in an adapter's `dto/`, mappers in its `mapper/`, no `dto` or `mapper` folder anywhere else. |
| `DependencyInjectionTest` | Every module is started from `container`; every action, service, mapper and repository is registered; a class registered on one platform is registered on the others unless only that platform declares it; every implemented domain contract is bound; every action is a `factory`; every class that keeps state is a `single`. |
| `AreaStructureTest` | One set of areas across domain, action, viewmodel and mapper, with absences declared in `ABSENT`. No file at the root of a layer. `domain/shared/` holds only its declared vocabulary. |
| `KindPackageTest` | Every enumeration sits in an `enum/` folder and an `enum/` folder holds only enumerations; `valueObject/` holds no enumeration, collaborator or comparator. Below a domain area only `enum/` and `valueObject/`, below a ViewModel area only `enum/`. |
| `FileOrganizationTest` | At most one public type per file, and every file named after what it declares. |
| `SourceLayoutTest` | The package matches the folder. Each module keeps to its package roots. No source hides from Konsist under `build/`, `target/` or a `buildsrc` name. |
| `UserInterfaceStructureTest` | No Koin and no use case in a component; every component under an area of `application/viewmodel` or `common/`; only `*Screen` files in `ui/screen/`. |
| `PlatformActualTest` | Every `expect` has an `actual` in `androidMain`, `iosMain` and `desktopMain`. |
| `CommentTest` | No `//` or `/* */` comment and no prose KDoc in production code; only a block-tag KDoc on a declaration and a suppression's reason. |
| `SuppressionTest` | No suppression switches off an architecture rule, `LongMethod` in composeApp or ktlint as a whole, under every spelling Konsist, detekt and ktlint honour; every `@Suppress` says why. |
| `DisplayTextTest` | No non-ASCII letter in production code or its literals, and no phrase in a string literal of the domain. |
| `TestConventionTest` | Test names are `should` sentences; a test stays in a JVM test source set only if it needs the JVM; no mocking library and nothing named as a mock. |

## What no gate checks

Written down here and left to review. A green gate says nothing about these:

- full words over abbreviations, and the verb of `{Verb}{Noun}Action`;
- present tense in the properties of a callback bundle (compose-rules checks a composable's parameters
  only);
- that the English and Polish strings files hold the same keys, that no English sentence is hard-coded
  in a screen, and that a translation keeps its accents;
- technology in a repository name spelled otherwise (`SQLite`), or on a class not named `*Repository`;
- where Koin starts: `bootstrap` may be imported from any composeApp file;
- the Koin constructor graph beyond the roles above — nothing runs Koin's `verify()`;
- `runCatching` in suspending code outside `application/action`;
- an overloaded `expect` — the compile of each target in CI catches it;
- where a fake lives, the Arrange-Act-Assert layout, `runTest` and `kotlin.test` assertions;
- a block-tag KDoc on a local declaration inside a function body;
- the stability advice under [Compose practices](#compose-practices) — detekt's `UnstableCollections`
  rule is off;
- detekt's written-out source list in `build.gradle.kts`, and the Swift in `iosApp`;
- that the test names this README cites exist in `:architecture`.

## License

See [`LICENSE`](LICENSE).
