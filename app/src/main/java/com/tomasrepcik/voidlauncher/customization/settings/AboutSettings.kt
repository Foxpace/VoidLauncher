package com.tomasrepcik.voidlauncher.customization.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.voidlauncher.R

private const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
private const val MIT_LICENSE_URL = "https://opensource.org/license/mit"

@Composable
internal fun AboutSettings(
    onOpenLicenses: () -> Unit,
) {
    val appVersion = rememberAppVersion()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionTitle(stringResource(R.string.about))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_version"),
        ) {
            AboutRow(
                icon = {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                },
                title = stringResource(R.string.version),
                summary = stringResource(
                    R.string.app_version_value,
                    appVersion.name,
                    appVersion.code,
                ),
            )
        }
        ElevatedCard(
            onClick = onOpenLicenses,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_source_licenses_button"),
        ) {
            AboutRow(
                icon = {
                    Icon(imageVector = Icons.Outlined.Description, contentDescription = null)
                },
                title = stringResource(R.string.open_source_licenses),
                summary = stringResource(R.string.open_source_licenses_summary),
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun rememberAppVersion(): AppVersion {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    return remember(context, isInspectionMode) {
        if (isInspectionMode) return@remember AppVersion(name = "1.0", code = 1)
        val packageInfo = context.installedPackageInfo()
        AppVersion(
            name = packageInfo.versionName.orEmpty(),
            code = packageInfo.longVersionCode,
        )
    }
}

private fun Context.installedPackageInfo(): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }

private data class AppVersion(
    val name: String,
    val code: Long,
)

@Composable
private fun AboutRow(
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        trailing()
    }
}

@Composable
internal fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("open_source_licenses_dialog"),
        title = { Text(stringResource(R.string.open_source_licenses)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.open_source_licenses_intro),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    LicenseGroup(
                        name = stringResource(R.string.license_voidlauncher),
                        components = stringResource(R.string.license_voidlauncher_components),
                        license = stringResource(R.string.mit_license),
                    )
                }
                item {
                    LicenseGroup(
                        name = stringResource(R.string.license_androidx),
                        components = stringResource(R.string.license_androidx_components),
                        license = stringResource(R.string.apache_license_2_0),
                    )
                }
                item {
                    LicenseGroup(
                        name = stringResource(R.string.license_kotlin),
                        components = stringResource(R.string.license_kotlin_components),
                        license = stringResource(R.string.apache_license_2_0),
                    )
                }
                item {
                    LicenseGroup(
                        name = stringResource(R.string.license_material_icons),
                        components = stringResource(R.string.license_material_icons_components),
                        license = stringResource(R.string.apache_license_2_0),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { uriHandler.openUri(MIT_LICENSE_URL) }) {
                    Text(stringResource(R.string.view_mit_license))
                }
                TextButton(onClick = { uriHandler.openUri(APACHE_LICENSE_URL) }) {
                    Text(stringResource(R.string.view_apache_license))
                }
            }
        },
    )
}

@Composable
private fun LicenseGroup(
    name: String,
    components: String,
    license: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleSmall)
        Text(
            text = components,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = license,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
