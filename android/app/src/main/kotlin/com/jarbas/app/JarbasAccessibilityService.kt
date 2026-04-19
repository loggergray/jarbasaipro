package com.jarbas.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class JarbasAccessibilityService : AccessibilityService() {

    private val TAG = "JarbasAccessibility"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        var instance: JarbasAccessibilityService? = null
        const val SOS_CONTACT = ""
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
        val skipButtons = findNodesByText(root, listOf("Pular anuncio", "Skip Ad", "PULAR", "SKIP", "Pular"))
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
        val possibleIds = listOf(
            "com.android.systemui:id/pinEntry",
            "com.android.systemui:id/passwordEntry",
            "com.android.systemui:id/lockPassword",
            "com.miui.securitycore:id/password_entry"
        )
        for (id in possibleIds) {
            val fields = root.findAccessibilityNodeInfosByViewId(id)
            if (fields != null && fields.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password)
                fields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                return
            }
        }
        for (digit in password) {
            val digitNodes = findNodesByText(root, listOf(digit.toString()))
            digitNodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(150)
        }
    }

    fun callContact(name: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$name")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ligar: ${e.message}")
        }
    }

    fun openYoutubeAndPlay(query: String) {
        val encodedQuery = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        scope.launch {
            delay(4000)
            withContext(Dispatchers.Main) {
                clickFirstVideoResult()
            }
        }
    }

    private fun clickFirstVideoResult() {
        val root = rootInActiveWindow ?: return
        val videoNodes = root.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/title"
        )
        if (videoNodes != null && videoNodes.isNotEmpty()) {
            videoNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    fun searchWeb(query: String, service: JarbasForegroundService) {
        scope.launch {
            try {
                val encoded = Uri.encode(query)
                val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
                val response = URL(url).readText()
                val json = JSONObject(response)

                val results = mutableListOf<Pair<String, String>>()

                val abstractText = json.optString("AbstractText", "")
                val abstractTitle = json.optString("Heading", "")
                if (abstractText.isNotEmpty()) {
                    results.add(Pair(abstractTitle, abstractText))
                }

                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.optJSONObject(i)
                        val text = topic?.optString("Text", "") ?: ""
                        if (text.isNotEmpty()) {
                            results.add(Pair("Resultado ${results.size + 1}", text))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (results.isEmpty()) {
                        service.speak("Nao encontrei resultados para $query")
                    } else {
                        val titles = results.take(4).mapIndexed { i, r -> "${i + 1}: ${r.first}" }.joinToString(". ")
                        service.speak("Achei ${results.size} resultados. $titles. Qual voce quer ouvir?")
                        searchResults = results.take(4)
                        service.awaitingSearchChoice = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    service.speak("Erro ao pesquisar. Verifique a internet.")
                }
            }
        }
    }

    var searchResults: List<Pair<String, String>> = emptyList()

    fun readSearchResult(index: Int, service: JarbasForegroundService) {
        if (index < searchResults.size) {
            val result = searchResults[index]
            service.speak("${result.first}. ${result.second}. Deseja ouvir o proximo?")
        }
    }

    fun triggerSOS(context: Context) {
        try {
            if (SOS_CONTACT.isNotEmpty()) {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(SOS_CONTACT, null, "EMERGENCIA: Preciso de ajuda!", null, null)
                callContact(SOS_CONTACT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro SOS: ${e.message}")
        }
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
        scope.cancel()
        super.onDestroy()
    }
}
