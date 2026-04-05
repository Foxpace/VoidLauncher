package com.tomasrepcik.voidlauncher.ui.components

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tomasrepcik.voidlauncher.data.model.InstalledApp

@Composable
fun AppIcon(
    app: InstalledApp,
    modifier: Modifier = Modifier,
) {
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
    val drawable by produceState<Drawable?>(initialValue = null, key1 = app.key) {
        value = runCatching {
            context.packageManager.getActivityIcon(
                ComponentName(app.key.packageName, app.key.activityName)
            )
        }.getOrNull()
    }
    return drawable?.let { BitmapPainter(it.toBitmap().asImageBitmap()) }
}
