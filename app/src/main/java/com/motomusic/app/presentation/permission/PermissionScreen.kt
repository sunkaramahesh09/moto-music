package com.motomusic.app.presentation.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.motomusic.app.core.MediaPermission

/**
 * First-launch welcome and the one permission request the app makes.
 *
 * The rationale is shown before the system dialog, and if the user has permanently denied the
 * permission the button switches to opening system settings, which is the only way back.
 */
@Composable
fun PermissionScreen(
    onPermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var requested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        requested = true
        onPermissionResult(results[MediaPermission.audioPermission] == true)
    }

    // Once the system marks the permission as permanently denied, no dialog will appear again.
    val permanentlyDenied = requested && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, MediaPermission.audioPermission)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(88.dp),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Moto Music", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Simple. Offline. Yours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Moto Music plays the music already stored on your phone.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        PermissionPoint(
            icon = Icons.Rounded.LibraryMusic,
            title = "Access to your audio files",
            body = "Needed to find the songs on this device. Moto Music reads nothing else — " +
                "no contacts, no location, no camera.",
        )
        Spacer(Modifier.height(16.dp))
        PermissionPoint(
            icon = Icons.Rounded.CloudOff,
            title = "Works completely offline",
            body = "There is no internet permission, no account and no backend. Your music and " +
                "listening history never leave the device.",
        )
        Spacer(Modifier.height(16.dp))
        PermissionPoint(
            icon = Icons.Rounded.Lock,
            title = "No ads, no tracking",
            body = "Nothing is collected, uploaded or shared.",
        )

        Spacer(Modifier.height(36.dp))

        if (permanentlyDenied) {
            Text(
                text = "Access to audio files is turned off. You can grant it in system settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { context.startActivity(appSettingsIntent(context.packageName)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open settings") }
        } else {
            Button(
                onClick = { launcher.launch(requestedPermissions()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Allow access to my music") }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onPermissionResult(MediaPermission.hasAudioAccess(context)) }) {
            Text("Not now")
        }
    }
}

/**
 * Audio access is the real request. On Android 13+ the notification permission is asked for at
 * the same time because without it the platform will not display media controls at all.
 */
private fun requestedPermissions(): Array<String> = buildList {
    add(MediaPermission.audioPermission)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        MediaPermission.notificationPermission?.let { add(it) }
    }
}.toTypedArray()

private fun appSettingsIntent(packageName: String) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

@Composable
private fun PermissionPoint(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
