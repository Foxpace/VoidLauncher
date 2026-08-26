# Use narrow feature boundaries

Status: Accepted

## Context

[ADR 0001](0001-centralize-launcher-errors-and-initialization.md) put startup state, storage failures, and default initialization behind `LauncherRepository`. [ADR 0002](0002-use-feature-roots-with-navigation-3-and-koin.md) replaced the manual app container with Koin and introduced Navigation 3 feature roots.

Those decisions fixed ownership at the application boundary, but the first implementation still concentrated too much work in a few types. Feature ViewModels depended on the full launcher repository. One storage contract exposed every write. The app navigation composable translated feature navigation events and also coordinated native effects. Large screen and database files mixed several independent components.

The result was technically layered but hard to read in the order of a user action. A change to home apps, shortcuts, schedules, or preferences often crossed contracts that also exposed unrelated launcher data.

## Decision

VoidLauncher keeps `LauncherRepository` as the internal owner of initialization and the complete storage snapshot. Feature ViewModels do not use it directly. They depend on narrow repositories named after the data or user intent they need:

- `InstalledAppsRepository` owns the installed app catalogue.
- `HomeAppsRepository` owns pinned home apps and their ordering and labels.
- `ShortcutRepository` owns launcher shortcuts.
- `PreferencesRepository` owns launcher preferences.
- `ScheduleRepository` owns app schedules.
- `LauncherStatusRepository` exposes startup status only to the app-level startup UI.

Storage follows the same split. `InstalledAppsStorage`, `HomeAppsStorage`, `ShortcutStorage`, `PreferencesStorage`, and `ScheduleStorage` are separate capabilities. `RoomLauncherStorage` implements them against one Room database, while tests can provide small fakes or the in-memory implementation. Room entities and DAOs live in separate files so each persistence contract can be inspected without reading the whole database implementation.

Each feature keeps a root, immutable contract, ViewModel, screen, and supporting UI parts. The screen renders state and sends actions to its ViewModel. The ViewModel coordinates repositories and emits state, root actions, and typed navigation events. The root is the boundary where Compose meets the application. It collects state, maps feature navigation events to `LauncherNavigator`, and delegates native work to `LauncherRootActionHandler`.

`LauncherNavigation` owns the back stack, route registration, and shared transition defaults. A route can override those defaults through its Navigation 3 entry metadata. The app drawer uses this route-owned metadata for its slower parallax and fade transition without changing motion on unrelated screens.

Native interactions use `LauncherRootAction`. `LauncherRootActionHandler` invokes `LauncherActionExecutor`, reports unexpected failures, maps failures and recovery to user messages, and returns a handled result to the root. ViewModels do not construct Android intents or call Android APIs.

Every `try` or `catch` block contains one statement. A named helper owns multi-step work inside that boundary. This makes the protected operation and its failure policy visible without mixing normal control flow into exception handling.

Tests use the same feature boundaries as production. ViewModel and repository tests use fakes, while Android implementation tests cover Room and platform behavior. Every test name states `given`, `when`, and `then`, and every test body separates those phases with `// GIVEN`, `// WHEN`, and `// THEN` comments. Shared fixtures create reusable conditions rather than hiding behavior behind mocks.

## Consequences

A ViewModel dependency list now describes the feature instead of the complete launcher. Repository and storage changes remain local to one capability unless the launcher snapshot itself changes. The central launcher repository still guarantees one initialization sequence and one coherent state source.

Navigation decisions sit next to the feature event that caused them. The route table remains centralized and easy to scan, while a feature root owns the translation from user intent to destination. Route-specific animation can change independently of the global navigation motion.

Native effects have one policy and one Android boundary, but feature roots remain responsible for deciding how handled results affect navigation or snackbars. Tests can exercise repository contracts without constructing unrelated dependencies.

The split adds more files and Koin definitions. That cost is deliberate. Each main file has one reason to change, and the application can be read by following UI, ViewModel, repository, storage, and platform boundaries in order.

This ADR refines ADR 0002. Navigation 3, Koin, and entry-scoped ViewModels remain unchanged. Feature roots now translate their own navigation events instead of sending that responsibility back to the app navigation composable. It also refines ADR 0001 by keeping initialization centralized while moving feature-facing reads and writes behind narrow repositories.

## Rejected alternatives

Keeping one feature-facing `LauncherRepository` would preserve fewer constructor parameters, but every ViewModel would still see unrelated data and mutations. Giving each feature its own Room database would separate storage physically at the cost of duplicated initialization, transactions, and migration work. Letting ViewModels navigate or call Android APIs directly would shorten root code but tie business state to Compose, Navigation 3, and the Android framework.
