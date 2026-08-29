# Package structure

VoidLauncher uses feature-first packages inside the single `app` Gradle module. The first package below `com.tomasrepcik.voidlauncher` names a user capability or an application-wide responsibility.

```text
voidlauncher/
├── appearance/
├── appcatalog/
├── customization/
├── design/
├── drawer/
├── home/
├── launcher/
├── onboarding/
├── schedule/
├── shortcuts/
└── storage/
```

The largest capabilities split further when a cohesive group reaches at least three files:

- `launcher/root` owns application composition, startup state, and root actions.
- `launcher/navigation` owns routes, back-stack behavior, and navigation transitions.
- `schedule/editor/content` owns editor fields and picker content. The editor contract, root, screen, and ViewModel remain together in `schedule/editor`.

## Ownership

- A screen keeps its root, screen, contract, ViewModel, actions, and navigation event in its capability package.
- Feature-owned persistence stays with the feature under `data`.
- `appcatalog` owns installed-app discovery and search used by multiple features.
- `design` owns reusable Compose controls and the application theme. It does not depend on product features.
- `storage` owns database-wide implementation and launcher-wide persistence. Features access it through feature-owned repositories.
- `launcher` composes features, handles Android side effects, and owns root navigation and launcher-wide contracts.

## Dependency rules

- Feature UI talks to its own ViewModel and contract.
- A feature may depend on its own data package, `design`, launcher-wide contracts, and narrow shared capabilities such as `appcatalog`.
- Features do not depend on launcher navigation implementations.
- Code moves to a shared package only after at least two features use it.
- Do not add generic `common`, `model`, `repository`, or `util` packages.
- Create a technical subpackage only for at least three related files. Stable user capabilities may remain small.
- Consider a split when a package grows beyond seven files. Keep a larger package intact when all files describe one screen and no group of at least three has a distinct responsibility.

Tests mirror production package paths. Shared fixtures stay in `testing`.
