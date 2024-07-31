package com.app.lockcompose


import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.app.lockcompose.ui.theme.LockComposeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LockComposeTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val hasUsageStatsPermission = remember { mutableStateOf(hasUsageStatsPermission(context)) }
        val hasOverlayPermission = remember { mutableStateOf(hasOverlayPermission(context)) }
        val hasNotificationPermission = remember { mutableStateOf(isNotificationPermissionGranted(context)) }
        val isServiceRunning = remember { mutableStateOf(false) }

        fun updatePermissionStatus() {
            hasUsageStatsPermission.value = hasUsageStatsPermission(context)
            hasOverlayPermission.value = hasOverlayPermission(context)
            hasNotificationPermission.value = isNotificationPermissionGranted(context)
        }

        fun startAppLockService() {
            val intent = Intent(context, AppLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopAppLockService() {
            val intent = Intent(context, AppLockService::class.java)
            context.stopService(intent)
        }

        fun hasAllPermissions(): Boolean {
            return hasUsageStatsPermission.value && hasOverlayPermission.value && hasNotificationPermission.value
        }

        val requestOverlayPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updatePermissionStatus()
        }

        val requestUsageStatsPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updatePermissionStatus()
        }

        val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updatePermissionStatus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ToggleButton(
                isChecked = isServiceRunning.value,
                onCheckedChange = {
                    if (it) {
                        if (hasAllPermissions()) {
                            startAppLockService()
                            isServiceRunning.value = true
                        } else {
                            Toast.makeText(context, "Please grant all permissions.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        stopAppLockService()
                        isServiceRunning.value = false
                    }
                },
                enabled = hasAllPermissions()
            )
            PermissionRow(
                label = "Overlay Permission",
                isGranted = hasOverlayPermission.value,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    requestOverlayPermissionLauncher.launch(intent)
                }
            )
            PermissionRow(
                label = "Usage Access Permission",
                isGranted = hasUsageStatsPermission.value,
                onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    requestUsageStatsPermissionLauncher.launch(intent)
                }
            )
            PermissionRow(
                label = "Notification Permission",
                isGranted = hasNotificationPermission.value,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        requestNotificationPermissionLauncher.launch(intent)
                    }
                }
            )
        }
    }

    @Composable
    fun ToggleButton(
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        enabled: Boolean
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lock Service")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isChecked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled
            )
        }
    }

    @Composable
    fun PermissionRow(
        label: String,
        isGranted: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(
                    id = if (isGranted) R.drawable.baseline_check_24 else R.drawable.uncheck
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

