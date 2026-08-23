package com.tomasrepcik.voidlauncher.ui.components

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tomasrepcik.voidlauncher.data.model.InstalledApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ICON_BITMAP_SIZE_PX = 144
private const val ICON_CACHE_BYTES = 12 * 1024 * 1024
private val appIconLoader = AppIconLoader()

internal typealias AppIconContent = @Composable (InstalledApp, Modifier) -> Unit

internal val LocalAppIconContent = staticCompositionLocalOf<AppIconContent?> { null }

@Composable
fun AppIcon(
    app: InstalledApp,
    modifier: Modifier = Modifier,
) {
    LocalAppIconContent.current?.let { content ->
        content(app, modifier)
        return
    }

    val painter = rememberAppIconPainter(app = app, context = LocalContext.current)
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = app.label,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun rememberAppIconPainter(
    app: InstalledApp,
    context: Context,
): Painter? {
    val bitmap by produceState<ImageBitmap?>(
        initialValue = AppIconBitmapCache.get(app.cacheKey())?.asImageBitmap(),
        key1 = app.key,
    ) {
        if (value != null) {
            return@produceState
        }
        value = appIconLoader.load(context, app)?.asImageBitmap()
    }
    return bitmap?.let(::BitmapPainter)
}

private class AppIconLoader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun load(context: Context, app: InstalledApp): Bitmap? = withContext(ioDispatcher) {
        val cacheKey = app.cacheKey()
        val cachedBitmap = AppIconBitmapCache.get(cacheKey)
        cachedBitmap ?: loadActivityIcon(context, app)?.toCachedBitmap(cacheKey)
    }

    private fun loadActivityIcon(context: Context, app: InstalledApp): Drawable? = runCatching {
        context.packageManager.getActivityIcon(
            ComponentName(app.key.packageName, app.key.activityName)
        )
    }.getOrNull()
}

private fun Drawable.toCachedBitmap(cacheKey: String): Bitmap {
    val bitmap = toBitmap(
        width = ICON_BITMAP_SIZE_PX,
        height = ICON_BITMAP_SIZE_PX,
    )
    AppIconBitmapCache.put(cacheKey, bitmap)
    return bitmap
}

private fun InstalledApp.cacheKey(): String = "${key.packageName}/${key.activityName}"

private object AppIconBitmapCache : LruCache<String, Bitmap>(ICON_CACHE_BYTES) {
    override fun sizeOf(
        key: String,
        value: Bitmap,
    ): Int = value.allocationByteCount
}
