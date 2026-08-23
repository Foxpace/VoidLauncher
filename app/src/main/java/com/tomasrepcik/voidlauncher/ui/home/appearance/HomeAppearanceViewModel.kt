package com.tomasrepcik.voidlauncher.ui.home.appearance

import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferences
import com.tomasrepcik.voidlauncher.data.model.LauncherPreferencesMutation
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepository
import com.tomasrepcik.voidlauncher.data.repository.LauncherRepositoryState
import com.tomasrepcik.voidlauncher.data.repository.RepositoryMutationOutcome
import com.tomasrepcik.voidlauncher.ui.LauncherUiEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

private const val MAX_BACKGROUND_DIMENSION = 1_600

data class HomeBackgroundImage(
    val bitmap: ImageBitmap,
    val primary: Color,
    val secondary: Color?,
    val tertiary: Color?,
)

data class HomeAppearanceState(
    val backgroundUri: String? = null,
    val useBackgroundColors: Boolean = false,
    val background: HomeBackgroundImage? = null,
    val isLoadingBackground: Boolean = false,
)

data class HomeAppearanceActions(
    val onBackgroundSelected: (String) -> Unit = {},
    val onRestoreDefault: () -> Unit = {},
    val onUseBackgroundColorsChange: (Boolean) -> Unit = {},
)

internal class HomeAppearanceViewModel private constructor(
    private val repository: LauncherRepository,
    private val permissions: UriPermissionAdapter,
    private val decoder: ImageDecoderAdapter,
) : ViewModel() {
    private val mutationMutex = Mutex()
    private val mutableState = MutableStateFlow(HomeAppearanceState())
    val state: StateFlow<HomeAppearanceState> = mutableState.asStateFlow()

    private val effectChannel = Channel<LauncherUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.state
                .mapNotNull { repositoryState ->
                    (repositoryState as? LauncherRepositoryState.Ready)?.launcher?.preferences
                }
                .distinctUntilChanged()
                .collectLatest(::loadAppearance)
        }
    }

    fun selectBackground(uri: String) = updateBackground(uri)

    fun restoreDefault() = updateBackground(null)

    fun setUseBackgroundColors(enabled: Boolean) {
        viewModelScope.launch {
            effectChannel.sendMutationError(
                repository.mutatePreferences(
                    LauncherPreferencesMutation.SetUseBackgroundColors(enabled),
                ),
            )
        }
    }

    private fun updateBackground(uri: String?) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val previousUri = mutableState.value.backgroundUri
                val permissionTaken = uri?.let(permissions::take)?.isSuccess ?: false
                when (
                    val outcome = repository.mutatePreferences(
                        LauncherPreferencesMutation.SetHomeBackground(uri),
                    )
                ) {
                    RepositoryMutationOutcome.Completed -> {
                        if (previousUri != uri) previousUri?.let(permissions::release)
                    }
                    is RepositoryMutationOutcome.Failed -> {
                        if (permissionTaken && previousUri != uri) permissions.release(uri)
                        effectChannel.send(LauncherUiEffect.Error(outcome.error))
                    }
                }
            }
        }
    }

    private suspend fun loadAppearance(preferences: LauncherPreferences) {
        val current = mutableState.value
        val uriChanged = current.backgroundUri != preferences.homeBackgroundUri
        mutableState.value = current.copy(
            backgroundUri = preferences.homeBackgroundUri,
            useBackgroundColors = preferences.useBackgroundColors,
            background = if (uriChanged) null else current.background,
            isLoadingBackground = uriChanged && preferences.homeBackgroundUri != null,
        )
        if (!uriChanged) return

        val loaded = preferences.homeBackgroundUri?.let { decoder.decode(it) }
        mutableState.update { state ->
            if (state.backgroundUri == preferences.homeBackgroundUri) {
                state.copy(background = loaded, isLoadingBackground = false)
            } else {
                state
            }
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            repository: LauncherRepository,
        ) = viewModelFactory {
            initializer {
                val applicationContext = context.applicationContext
                HomeAppearanceViewModel(
                    repository = repository,
                    permissions = AndroidUriPermissionAdapter(applicationContext),
                    decoder = AndroidImageDecoderAdapter(applicationContext),
                )
            }
        }

        internal fun createForTest(
            repository: LauncherRepository,
            takePermission: (String) -> Boolean = { true },
            releasePermission: (String) -> Unit = {},
            decode: suspend (String) -> HomeBackgroundImage? = { null },
        ) = HomeAppearanceViewModel(
            repository = repository,
            permissions = object : UriPermissionAdapter {
                override fun take(uri: String): Result<Unit> =
                    if (takePermission(uri)) Result.success(Unit) else Result.failure(
                        IllegalStateException("Permission was not persisted"),
                    )

                override fun release(uri: String) = releasePermission(uri)
            },
            decoder = object : ImageDecoderAdapter {
                override suspend fun decode(uri: String) = decode(uri)
            },
        )
    }
}

private suspend fun Channel<LauncherUiEffect>.sendMutationError(
    outcome: RepositoryMutationOutcome,
) {
    if (outcome is RepositoryMutationOutcome.Failed) {
        send(LauncherUiEffect.Error(outcome.error))
    }
}

private interface UriPermissionAdapter {
    fun take(uri: String): Result<Unit>
    fun release(uri: String)
}

private class AndroidUriPermissionAdapter(context: Context) : UriPermissionAdapter {
    private val contentResolver = context.contentResolver

    override fun take(uri: String): Result<Unit> = runCatching {
        contentResolver.takePersistableUriPermission(
            uri.toUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun release(uri: String) {
        runCatching {
            contentResolver.releasePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

private interface ImageDecoderAdapter {
    suspend fun decode(uri: String): HomeBackgroundImage?
}

private class AndroidImageDecoderAdapter(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImageDecoderAdapter {
    private val contentResolver = context.contentResolver

    override suspend fun decode(uri: String): HomeBackgroundImage? = withContext(ioDispatcher) {
        runCatching {
            val source = ImageDecoder.createSource(contentResolver, uri.toUri())
            ImageDecoder.decodeBitmap(source) { imageDecoder, info, _ ->
                imageDecoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largestDimension = maxOf(info.size.width, info.size.height)
                if (largestDimension > MAX_BACKGROUND_DIMENSION) {
                    val scale = MAX_BACKGROUND_DIMENSION.toFloat() / largestDimension
                    imageDecoder.setTargetSize(
                        (info.size.width * scale).roundToInt(),
                        (info.size.height * scale).roundToInt(),
                    )
                }
            }.toHomeBackgroundImage()
        }.getOrNull()
    }
}

private fun Bitmap.toHomeBackgroundImage(): HomeBackgroundImage {
    val colors = WallpaperColors.fromBitmap(this)
    return HomeBackgroundImage(
        bitmap = asImageBitmap(),
        primary = Color(colors.primaryColor.toArgb()),
        secondary = colors.secondaryColor?.let { Color(it.toArgb()) },
        tertiary = colors.tertiaryColor?.let { Color(it.toArgb()) },
    )
}
