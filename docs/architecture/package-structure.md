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

Tests mirror production package paths. Shared fixtures stay in `testing`.
