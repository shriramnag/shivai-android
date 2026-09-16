package com.personal.ai.shivai.core.automation

import android.graphics.Rect

data class AccessibleNode(
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val boundsInScreen: Rect
)

data class DeviceContext(
    val currentPackage: String,
    val windowTitle: String,
    val accessibleNodes: List<AccessibleNode>,
    val lastDetectedChangeTimestamp: Long = System.currentTimeMillis()
) {
    fun toSemanticSummary(): String {
        val builder = StringBuilder()
        builder.append("Active Package: ").append(currentPackage).append("\n")
        builder.append("Window: ").append(windowTitle).append("\n")
        builder.append("Interactive Elements:\n")
        accessibleNodes
            .filter { it.text.isNotBlank() || it.contentDescription.isNotBlank() || it.isEditable }
            .take(40)
            .forEachIndexed { i, n ->
                val label = if (n.text.isNotBlank()) n.text else n.contentDescription
                val tag = when {
                    n.isEditable -> "[Input]"
                    n.isClickable -> "[Button]"
                    else -> "[Label]"
                }
                builder.append("  $i. $tag \"$label\" (id: ${n.viewId.ifBlank { "none" }})\n")
            }
        return builder.toString()
    }
}
