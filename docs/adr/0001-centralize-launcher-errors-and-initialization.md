# Centralize launcher errors and repository initialization

VoidLauncher uses `AppError` as the only model for application failures. Blank searches, missing matches, and app hints remain ordinary UI feedback. The launcher-action module performs known recovery before it returns a recovered or failed outcome, and it lets unexpected exceptions escape.

`LauncherRepository` is a concrete module that owns default creation and exposes `Loading`, `Ready`, or `InitializationError` state. Callers cannot read or write storage directly and cannot initialize defaults themselves. Production uses Room storage, while local tests use the in-memory storage adapter through the same repository interface.

This makes startup failure explicit and retryable. A failed later mutation keeps the last `Ready` launcher state and attaches a non-blocking `STORAGE_WRITE_FAILED` error. The cost is that storage and PackageManager changes must pass through the repository, but that constraint prevents partially initialized launcher state from reaching screens.
