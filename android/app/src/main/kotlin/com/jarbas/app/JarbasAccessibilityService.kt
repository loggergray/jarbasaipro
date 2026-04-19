package com.jarbas.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarbasAccessibilityService : AccessibilityService() {

    private val TAG = "JarbasAccessibility"

    companion object {
        var instance: JarbasAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "AccessibilityService conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName.contains("youtube")) {
            checkAndSkipAd()
        }
    }

    private fun checkAndSkipAd() {
        val root = rootInActiveWindow ?: return
        val skipButtons = findNodesByText(root, listOf("Pular anuncio", "Skip Ad", "PULAR", "SKIP"))
        if (skipButtons.isNotEmpty()) {
            skipButtons[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Anuncio pulado automaticamente")
        }
    }

    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun typePassword(password: String) {
        val root = rootInActiveWindow ?: return
        val fields = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/pinEntry")
            ?: root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/passwordEntry")

        if (fields != null && fields.isNotEmpty()) {
            val bundle = Bundle()
            bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password)
            fields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        } else {
            for (digit in password) {
                val digitNodes = findNodesByText(root, listOf(digit.toString()))
                digitNodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(150)
            }
        }
    }

    fun callContact(name: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val searchIntent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val callUri = Uri.parse("tel:$name")
            val directCall = Intent(Intent.ACTION_CALL, callUri)
            directCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(directCall)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ligar: ${e.message}")
        }
    }

    fun openYoutubeAndPlay(query: String) {
        val encodedQuery = query.replace(" ", "+")
        val youtubeIntent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery"))
        youtubeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(youtubeIntent)
    }

    fun clickOnScreen(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun findAndClick(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = findNodesByText(root, listOf(text))
        return if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } else false
    }

    fun typeText(text: String) {
        val root = rootInActiveWindow ?: return
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val bundle = Bundle()
        bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    private fun findNodesByText(root: AccessibilityNodeInfo, texts: List<String>): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes != null) result.addAll(nodes)
        }
        return result
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrompido")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
