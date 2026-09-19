package com.personal.ai.shivai.core.health

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.personal.ai.shivai.core.automation.ShivAccessibilityService

data class PermissionItem(
    val id: String,
    val title: String,
    val isGranted: Boolean,
    val isEssential: Boolean,
    val rationaleEn: String,
    val rationaleHi: String
)

data class PermissionAuditReport(
    val items: List<PermissionItem>,
    val allEssentialGranted: Boolean,
    val missingEssentialCount: Int
)

class PermissionIntelligence(private val context: Context) {

    fun auditPermissions(): PermissionAuditReport {
        val list = mutableListOf<PermissionItem>()

        val isAccessGranted = ShivAccessibilityService.instance != null
        list.add(
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                isGranted = isAccessGranted,
                isEssential = true,
                rationaleEn = "Required for UI inspection and safe automation actions.",
                rationaleHi = "UI निरीक्षण और सुरक्षित ऑटोमेशन के लिए आवश्यक।"
            )
        )

        val isUsageStatsGranted = checkUsageStatsPermission()
        list.add(
            PermissionItem(
                id = "usage_stats",
                title = "Usage Access",
                isGranted = isUsageStatsGranted,
                isEssential = true,
                rationaleEn = "Required to detect foreground applications for Privacy Mode.",
                rationaleHi = "प्राइवेसी मोड के लिए एक्टिव ऐप की पहचान हेतु आवश्यक।"
            )
        )

        val isOverlayGranted = Settings.canDrawOverlays(context)
        list.add(
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps",
                isGranted = isOverlayGranted,
                isEssential = false,
                rationaleEn = "Allows showing floating emergency stop button.",
                rationaleHi = "स्क्रीन पर फ्लोटिंग इमरजेंसी स्टॉप बटन दिखाने के लिए।"
            )
        )

        val isAudioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "record_audio",
                title = "Microphone / Voice Input",
                isGranted = isAudioGranted,
                isEssential = false,
                rationaleEn = "Required for voice commands in Hindi and English.",
                rationaleHi = "हिंदी और अंग्रेजी में वॉयस कमांड्स के लिए।"
            )
        )

        val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                isGranted = isNotificationGranted,
                isEssential = false,
                rationaleEn = "Required for security alert notices and execution progress.",
                rationaleHi = "सुरक्षा चेतावनियों और प्रोग्रेस नोटिफिकेशन के लिए।"
            )
        )

        val missingEssential = list.count { it.isEssential && !it.isGranted }

        return PermissionAuditReport(
            items = list,
            allEssentialGranted = missingEssential == 0,
            missingEssentialCount = missingEssential
        )
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
