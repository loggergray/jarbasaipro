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

    var searchResults: List<Pair<String, String>> = emptyList()

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
            Log.d(TAG, "Anuncio pulado")
        }
    }

    fun pressBack() { performGlobalAction(GLOBAL_ACTION_BACK) }
    fun pressHome() { performGlobalAction(GLOBAL_ACTION_HOME) }

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
            if (!fields.isNullOrEmpty()) {
                val bundle = Bundle()
                bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password)
                fields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                return
            }
        }
        for (digit in password) {
            findNodesByText(root, listOf(digit.toString())).firstOrNull()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
            withContext(Dispatchers.Main) { clickFirstVideoResult() }
        }
    }

    private fun clickFirstVideoResult() {
        val root = rootInActiveWindow ?: return
        val videoNodes = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/title")
        if (!videoNodes.isNullOrEmpty()) {
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
                        service.awaitingSearchChoice = false
                    } else {
                        searchResults = results.take(4)
                        val titles = searchResults.mapIndexed { i, r -> "${i + 1}: ${r.first}" }.joinToString(". ")
                        service.speak("Achei ${searchResults.size} resultados sobre $query. $titles. Qual voce quer ouvir?")
                        service.awaitingSearchChoice = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    service.speak("Erro ao pesquisar. Verifique a internet.")
                    service.awaitingSearchChoice = false
                }
            }
        }
    }

    fun readSearchResult(index: Int, service: JarbasForegroundService) {
        if (index < searchResults.size) {
            val result = searchResults[index]
            service.speak("${result.first}. ${result.second}. Deseja ouvir o proximo?")
        } else {
            service.speak("Nao ha mais resultados.")
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }
}
