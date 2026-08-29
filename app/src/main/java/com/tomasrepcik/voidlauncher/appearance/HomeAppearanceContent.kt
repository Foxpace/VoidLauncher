package com.tomasrepcik.voidlauncher.appearance

import android.app.WallpaperColors
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val MAX_BACKGROUND_DIMENSION = 1_600

data class HomeBackgroundImage(
    val bitmap: ImageBitmap,
    val primary: Color,
    val secondary: Color?,
    val tertiary: Color?,
)

internal class AndroidContentPermissionManager(
    context: Context,
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    fun keepReadAccess(uri: String): Result<Unit> = runCatching {
        contentResolver.takePersistableUriPermission(
            uri.toUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    fun releaseReadAccess(uri: String) {
        runCatching {
            contentResolver.releasePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

internal class AndroidBackgroundImageReader(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    suspend fun read(uri: String): HomeBackgroundImage? = withContext(ioDispatcher) {
        runCatching {
            val source = ImageDecoder.createSource(contentResolver, uri.toUri())
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largestDimension = maxOf(info.size.width, info.size.height)
                if (largestDimension > MAX_BACKGROUND_DIMENSION) {
                    val scale = MAX_BACKGROUND_DIMENSION.toFloat() / largestDimension
                    decoder.setTargetSize(
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
