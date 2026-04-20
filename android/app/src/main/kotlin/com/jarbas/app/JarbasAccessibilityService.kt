package com.jarbas.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
    private val handler = Handler(Looper.getMainLooper())

    var searchResults: List<Pair<String, String>> = emptyList()

    companion object {
        var instance: JarbasAccessibilityService? = null
        var sosContact = ""
    }

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "AccessibilityService conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg.contains("youtube")) {
            handler.postDelayed({ checkAndSkipAd() }, 500)
        }
    }

    private fun checkAndSkipAd() {
        val root = rootInActiveWindow ?: return
        val skipTexts = listOf(
            "Pular anúncio", "Pular anuncio", "Skip Ad", "PULAR", "SKIP",
            "Pular", "Skip", "Pular propaganda", "Skip ads"
        )
        for (text in skipTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (!nodes.isNullOrEmpty()) {
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Anuncio pulado: $text")
                return
            }
        }
    }

    fun pressBack() { performGlobalAction(GLOBAL_ACTION_BACK) }
    fun pressHome() { performGlobalAction(GLOBAL_ACTION_HOME) }

    fun wakeAndUnlock() {
        // GLOBAL_ACTION_WAKEUP nao existe no API 26, usando BACK para acender tela
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, 300)
    }

    fun typePassword(password: String) {
        Log.d(TAG, "Digitando senha: $password")
        val root = rootInActiveWindow ?: run {
            Log.d(TAG, "rootInActiveWindow null")
            return
        }

        val possibleIds = listOf(
            "com.android.systemui:id/pinEntry",
            "com.android.systemui:id/passwordEntry",
            "com.android.systemui:id/lockPassword",
            "com.miui.securitycore:id/password_entry",
            "com.miui.home:id/password_entry",
            "com.samsung.android:id/passwordEntry",
            "com.google.android:id/password"
        )

        for (id in possibleIds) {
            val fields = root.findAccessibilityNodeInfosByViewId(id)
            if (!fields.isNullOrEmpty()) {
                val bundle = Bundle()
                bundle.putString(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    password
                )
                fields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                Log.d(TAG, "Senha digitada via campo $id")

                handler.postDelayed({
                    val r = rootInActiveWindow ?: return@postDelayed
                    val confirmTexts = listOf(
                        "OK", "Enter", "Confirmar", "Done", "Concluido",
                        "Desbloquear", "Unlock", "Continuar", "Next"
                    )
                    for (text in confirmTexts) {
                        val confirmNodes = r.findAccessibilityNodeInfosByText(text)
                        if (!confirmNodes.isNullOrEmpty()) {
                            confirmNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.d(TAG, "Confirmou senha com: $text")
                            return@postDelayed
                        }
                    }
                    Log.d(TAG, "Botao confirmacao nao encontrado, usando BACK")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }, 500)
                return
            }
        }

        // FALLBACK ROBUSTO: digito por digito
        Log.d(TAG, "Fallback: clicando digito a digito")
        var delay = 0L
        for (digit in password) {
            handler.postDelayed({
                val r = rootInActiveWindow ?: return@postDelayed
                val nodes = r.findAccessibilityNodeInfosByText(digit.toString())
                nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }, delay)
            delay += 300
        }
        handler.postDelayed({
            val r = rootInActiveWindow ?: return@postDelayed
            val confirmTexts = listOf("OK", "Enter", "Confirmar", "Done")
            for (text in confirmTexts) {
                val nodes = r.findAccessibilityNodeInfosByText(text)
                if (!nodes.isNullOrEmpty()) {
                    nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return@postDelayed
                }
            }
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, delay + 300)
    }

    fun findContacts(name: String, resolver: ContentResolver): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        try {
            val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val contactName = it.getString(0) ?: continue
                    val number = it.getString(1)?.replace("[^0-9+]".toRegex(), "") ?: continue
                    results.add(Pair(contactName, number))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro buscando contato: ${e.message}")
        }
        return results
    }

    fun callNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d(TAG, "Ligando para $number")
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
            delay(5000)
            withContext(Dispatchers.Main) { clickFirstVideoResult() }
        }
    }

    private fun clickFirstVideoResult() {
        val root = rootInActiveWindow ?: return
        val ids = listOf(
            "com.google.android.youtube:id/title",
            "com.google.android.youtube:id/video_title",
            "com.google.android.apps.youtube:id/title"
        )
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicou no primeiro video via ID: $id")
                return
            }
        }
        // FALLBACK: primeiro clicavel com texto longo
        val allText = findAllClickableNodes(root)
            .filter { it.text?.length ?: 0 > 10 }
        allText.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findAllClickableNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (node.isClickable) result.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            result.addAll(findAllClickableNodes(child))
            child.recycle()
        }
        return result
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
                    results.add(Pair(abstractTitle.ifEmpty { "Resultado principal" }, abstractText))
                }

                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.optJSONObject(i) ?: continue
                        val text = topic.optString("Text", "")
                        if (text.isNotEmpty()) {
                            results.add(Pair("Resultado ${results.size + 1}", text))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (results.isEmpty()) {
                        service.speak("Nao encontrei resultados para $query")
                    } else {
                        searchResults = results.take(4)
                        val titles = searchResults.mapIndexed { i, r -> "${i + 1}: ${r.first.take(30)}..." }.joinToString(". ")
                        service.speak("Achei ${searchResults.size} resultados. $titles. Qual quer ouvir?")
                        service.awaitingSearchChoice = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro pesquisa: ${e.message}")
                withContext(Dispatchers.Main) {
                    service.speak("Erro ao pesquisar. Verifique a internet.")
                }
            }
        }
    }

    fun readSearchResult(index: Int, service: JarbasForegroundService) {
        if (index < searchResults.size) {
            val r = searchResults[index]
            val content = r.second.take(200) + if (r.second.length > 200) "..." else ""
            service.speak("${r.first}. $content. Deseja ouvir o proximo?")
            service.awaitingNextResult = true
        } else {
            service.speak("Nao ha mais resultados.")
        }
    }

    fun triggerSOS(context: Context) {
        try {
            if (sosContact.isNotEmpty()) {
                val sms = SmsManager.getDefault()
                sms.sendTextMessage(
                    sosContact, null,
                    "EMERGENCIA: Preciso de ajuda urgente!",
                    null, null
                )
                callNumber(sosContact)
                Log.d(TAG, "SOS enviado para $sosContact")
            } else {
                callNumber("192")
                Log.d(TAG, "SOS para SAMU 192")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro SOS: ${e.message}")
        }
    }

    fun typeText(text: String) {
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val bundle = Bundle()
        bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        focused?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt chamado")
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        Log.d(TAG, "AccessibilityService destruido")
        super.onDestroy()
    }
}
