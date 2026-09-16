package com.personal.ai.shivai.core.automation

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class LaunchableApp(
    val label: String,
    val packageName: String
)

class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = packageManager.queryIntentActivities(intent, 0)
        return resolved.map {
            LaunchableApp(
                label = it.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun launchAppByName(query: String): Pair<Boolean, String> {
        val clean = query.trim().lowercase()
        val apps = getInstalledLaunchableApps()

        val matched = apps.firstOrNull {
            it.packageName.equals(clean, ignoreCase = true) ||
                    it.label.lowercase() == clean ||
                    it.label.lowercase().contains(clean)
        }

        if (matched == null) {
            return Pair(false, "App '$query' not found among installed launchable applications.")
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(matched.packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Pair(true, "Successfully launched ${matched.label} (${matched.packageName})")
        } else {
            Pair(false, "Could not obtain launcher intent for ${matched.label}")
        }
    }

    fun safeExitForegroundApp(): String {
        val service = ShivAccessibilityService.instance
        return if (service != null) {
            service.performHome()
            "Returned to Home Screen. Android security restricts non-system apps from force-stopping third-party applications."
        } else {
            "Accessibility Service is required to trigger navigation."
        }
    }

    fun queryUsageStatsForegroundApp(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - (1000 * 60 * 2), now)
            if (!stats.isNullOrEmpty()) {
                val top = stats.maxByOrNull { it.lastTimeUsed }
                return top?.packageName ?: "Unknown"
            }
        }
        return "Unknown"
    }
}
