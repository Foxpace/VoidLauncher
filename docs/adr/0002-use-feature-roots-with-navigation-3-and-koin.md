# Use feature roots with Navigation 3 and Koin

Status: Accepted

## Context

VoidLauncher previously assembled application dependencies through a manual `AppContainer`. The app-level composable also mixed navigation, ViewModel creation, state collection, effect handling, and screen rendering. This made lifecycle ownership unclear and required production factory helpers that tests did not need.

The launcher needs one level of application navigation while keeping each feature independently testable. Repositories should provide domain data and named mutations. ViewModels should derive screen state without knowing about routes, back stacks, Room, or Android intent handling.

## Decision

VoidLauncher uses one application-level Navigation 3 back stack. The app navigation module declares every serializable route and every `entry<Route>` registration explicitly. Each entry delegates directly to a feature root such as `HomeRoot`, `DrawerRoot`, `CustomizationRoot`, `ScheduleListRoot`, or `ScheduleEditorRoot`.

A feature root is the integration boundary for one navigation entry. It resolves its ViewModels with `koinViewModel()`, passes route arguments through Koin parameters, collects lifecycle-aware state, collects effects through a local `LauncherEffectBoundary`, and renders a stateless screen. Feature roots emit typed navigation events. Only the app navigation module translates those events into back-stack operations. ViewModels do not depend on Navigation 3 types.

Koin replaces the manual container. All ViewModels use Koin `viewModel` definitions, so the Navigation 3 entry's `ViewModelStoreOwner` controls their lifetime. ViewModels are never Koin singletons. The Room database, installed-app data source, repository, search module, action executor, effect handler, and error reporter may be application singletons because they either own shared data or have no entry-specific state.

Repositories expose resolved domain data and named operations such as `removeHomeApp`, `saveShortcut`, and `mutateSchedule`. ViewModels turn repository data into UI state. The private storage interface accepts one sealed `LauncherStorageMutation` model for launcher writes. Room and in-memory storage implement the same exhaustive mutation dispatch while keeping their adapter-specific behavior.

Each root delegates effects to the shared `LauncherEffectHandler`. The handler runs `LauncherActionExecutor`, reports unexpected errors with their original cause and `AppOperation`, maps failures to safe user messages, and sends those messages to the root's snackbar boundary. `LauncherActionExecutor` owns intent construction, destination checks, fallback policy, package inspection, recovery, and failure translation.

## Consequences

Navigation entry lifetime now determines ViewModel lifetime. Returning to an entry reuses its ViewModels while a separate entry gets separate instances. Shared repository state remains available to every entry.

Routes remain easy to find because registration stays in one app navigation module. Adding a route requires an explicit route type, entry registration, root, and typed event translation. VoidLauncher does not use Koin's aggregated Navigation 3 entry provider.

Feature screens remain stateless and can be previewed or tested without Koin. Domain and ViewModel tests keep using direct constructors. One small Koin verification test checks the production dependency graph, and an entry ownership test checks that `koinViewModel()` follows the navigation entry owner.

Adding a storage write requires a new `LauncherStorageMutation` case and an implementation in both storage adapters. The exhaustive dispatch is intentionally centralized even though it is larger than the project's normal method-complexity threshold.

## Rejected alternatives

A separate back stack per feature would add navigation state the launcher does not need. Koin-managed route aggregation would hide the complete route table. Keeping `AppContainer` would preserve manual factory wiring and unclear lifecycle ownership. Exposing generic storage commands through repositories or ViewModels would leak persistence concerns into UI code.
