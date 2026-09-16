package com.personal.ai.shivai.core.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShivAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: ""
            if (pkg.isNotBlank() && pkg != _currentPackage.value) {
                _currentPackage.value = pkg
                _lastChangeTime.value = System.currentTimeMillis()
            }
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    fun captureDeviceContext(): DeviceContext {
        val root = rootInActiveWindow
        val pkg = _currentPackage.value
        if (root == null) {
            return DeviceContext(pkg, "Unavailable/Restricted Window", emptyList())
        }
        val elements = mutableListOf<AccessibleNode>()
        traverseNodeTree(root, elements)
        return DeviceContext(
            currentPackage = root.packageName?.toString() ?: pkg,
            windowTitle = root.windowTitle?.toString() ?: "",
            accessibleNodes = elements
        )
    }

    private fun traverseNodeTree(node: AccessibilityNodeInfo?, outList: MutableList<AccessibleNode>) {
        if (node == null) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""

        if (text.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isEditable) {
            outList.add(
                AccessibleNode(
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    boundsInScreen = bounds
                )
            )
        }

        for (i in 0 until node.childCount) {
            traverseNodeTree(node.getChild(i), outList)
        }
    }

    fun clickElementByText(text: String, exact: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(text)
        if (matches.isNullOrEmpty()) return false

        for (node in matches) {
            val matchesRule = if (exact) {
                node.text?.toString().equals(text, ignoreCase = true) ||
                        node.contentDescription?.toString().equals(text, ignoreCase = true)
            } else {
                node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                        node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
            }

            if (matchesRule) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    if (current.isClickable) {
                        return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    current = current.parent
                }
            }
        }
        return false
    }

    fun setTextInput(text: String, fieldIndex: Int = 0): Boolean {
        val root = rootInActiveWindow ?: return false
        val editables = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodes(root, editables)

        if (editables.isNotEmpty() && fieldIndex < editables.size) {
            val target = editables[fieldIndex]
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        }
        return false
    }

    private fun findEditableNodes(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isEditable) out.add(node)
        for (i in 0 until node.childCount) {
            findEditableNodes(node.getChild(i), out)
        }
    }

    fun scroll(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollable(root) ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val res = findFirstScrollable(node.getChild(i))
            if (res != null) return res
        }
        return null
    }

    fun dispatchCoordinateTap(x: Float, y: Float, durationMs: Long = 100): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    companion object {
        var instance: ShivAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentPackage = MutableStateFlow("com.personal.ai.shivai")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        private val _lastChangeTime = MutableStateFlow(System.currentTimeMillis())
        val lastChangeTime: StateFlow<Long> = _lastChangeTime.asStateFlow()
    }
}
