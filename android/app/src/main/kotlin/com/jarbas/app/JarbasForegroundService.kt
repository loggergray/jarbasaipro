package com.jarbas.app

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class JarbasForegroundService : Service(), TextToSpeech.OnInitListener {

    private val TAG = "JarbasForeground"
    private val CHANNEL_ID = "jarbas_channel"
    private val NOTIF_ID = 1
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    var awaitingPassword = false
    var awaitingAppChoice = false
    var awaitingSearchChoice = false
    var awaitingNextResult = false
    var awaitingCallConfirm = false
    var currentSearchIndex = 0
    var pendingApps: List<String> = emptyList()
    var pendingContactNumber: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                startForeground(NOTIF_ID, buildNotification("Jarbas ouvindo..."))
                startListening()
            }
            "STOP" -> {
                speakThenStop("Jarbas encerrado. Ate logo.")
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("pt", "BR")
            tts.setSpeechRate(0.95f)
            handler.postDelayed({ speak("Jarbas iniciado. Pode falar.") }, 1000)
        }
    }

    private fun startListening() {
        isListening = true
        handler.postDelayed({ listenCycle() }, 500)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun listenCycle() {
        if (!isListening) return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase()?.trim() ?: ""
                Log.d(TAG, "Reconheceu: '$text'")
                if (text.isNotEmpty()) processInput(text)
                else handler.postDelayed({ listenCycle() }, 300)
            }
            override fun onError(error: Int) {
                Log.d(TAG, "Erro reconhecimento: $error")
                if (isListening) handler.postDelayed({ listenCycle() }, 800)
            }
            override fun onReadyForSpeech(p: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle) {}
            override fun onEvent(t: Int, p: Bundle) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun processInput(text: String) {
        Log.d(TAG, "Processando: '$text' | senha=$awaitingPassword app=$awaitingAppChoice")

        when {
            awaitingPassword -> {
                awaitingPassword = false
                // extrai só os números ou dígitos falados
                val digits = text
                    .replace("zero", "0").replace("um", "1").replace("dois", "2")
                    .replace("tres", "3").replace("três", "3").replace("quatro", "4")
                    .replace("cinco", "5").replace("seis", "6").replace("sete", "7")
                    .replace("oito", "8").replace("nove", "9")
                    .filter { it.isDigit() }
                if (digits.isNotEmpty()) {
                    speak("Digitando senha.")
                    JarbasAccessibilityService.instance?.typePassword(digits)
                } else {
                    speak("Nao entendi a senha. Pode repetir os numeros?")
                    awaitingPassword = true
                }
                handler.postDelayed({ listenCycle() }, 2000)
            }

            awaitingAppChoice -> {
                awaitingAppChoice = false
                val choice = extractNumber(text)
                if (choice in 0 until pendingApps.size) {
                    openApp(pendingApps[choice])
                } else {
                    speak("Diga um ou dois.")
                    awaitingAppChoice = true
                }
                handler.postDelayed({ listenCycle() }, 1500)
            }

            awaitingCallConfirm -> {
                awaitingCallConfirm = false
                val choice = extractNumber(text)
                if (choice in 0 until pendingApps.size) {
                    speak("Ligando.")
                    JarbasAccessibilityService.instance?.callNumber(pendingApps[choice])
                } else {
                    speak("Diga um ou dois.")
                    awaitingCallConfirm = true
                }
                handler.postDelayed({ listenCycle() }, 1500)
            }

            awaitingSearchChoice -> {
                awaitingSearchChoice = false
                val choice = extractNumber(text)
                if (choice >= 0) {
                    currentSearchIndex = choice
                    JarbasAccessibilityService.instance?.readSearchResult(choice, this)
                    awaitingNextResult = true
                } else {
                    speak("Diga o numero do resultado.")
                    awaitingSearchChoice = true
                }
                handler.postDelayed({ listenCycle() }, 1000)
            }

            awaitingNextResult -> {
                awaitingNextResult = false
                if (text.contains("sim") || text.contains("proximo") || text.contains("mais")) {
                    currentSearchIndex++
                    val results = JarbasAccessibilityService.instance?.searchResults ?: emptyList()
                    if (currentSearchIndex < results.size) {
                        JarbasAccessibilityService.instance?.readSearchResult(currentSearchIndex, this)
                        awaitingNextResult = true
                    } else {
                        speak("Nao ha mais resultados.")
                    }
                } else {
                    speak("Certo.")
                }
                handler.postDelayed({ listenCycle() }, 1000)
            }

            text.contains("jarbas") -> {
                val command = text.substringAfter("jarbas").trim()
                if (command.isEmpty()) {
                    speak("Sim?")
                } else {
                    handleCommand(command)
                }
            }

            else -> handler.postDelayed({ listenCycle() }, 300)
        }
    }

    private fun handleCommand(command: String) {
        Log.d(TAG, "Comando: '$command'")
        when {
            command.contains("acorda") || command.contains("desbloqueia") || command.contains("desbloqueie") -> {
                JarbasAccessibilityService.instance?.wakeAndUnlock()
                speak("Qual a senha?")
                awaitingPassword = true
                handler.postDelayed({ listenCycle() }, 2500)
            }

            command.contains("encerrar") || command.contains("encerra") || command.contains("fechar jarbas") -> {
                speakThenStop("Encerrando. Ate logo.")
            }

            command.contains("obrigado") -> {
                speak("Disponha! E so chamar.")
                handler.postDelayed({ listenCycle() }, 2000)
            }

            command.contains("socorro") || command.contains("emergencia") || command.contains("ajuda") -> {
                speak("Acionando emergencia!")
                JarbasAccessibilityService.instance?.triggerSOS(this)
                handler.postDelayed({ listenCycle() }, 2000)
            }

            command.contains("pesquisa") || command.contains("busca") || command.contains("pesquisar") || command.contains("buscar") -> {
                val query = extractAfter(command, listOf(
                    "pesquisa sobre", "pesquisa", "pesquisar sobre", "pesquisar",
                    "busca sobre", "busca", "buscar sobre", "buscar"
                ))
                if (query.isNotEmpty()) {
                    speak("Pesquisando $query")
                    JarbasAccessibilityService.instance?.searchWeb(query, this)
                } else {
                    speak("O que devo pesquisar?")
                    handler.postDelayed({ listenCycle() }, 2000)
                }
            }

            command.contains("liga") || command.contains("ligar") || command.contains("chamar") || command.contains("chama") -> {
                val name = extractAfter(command, listOf(
                    "liga pra", "liga para", "ligar pra", "ligar para",
                    "chama", "chamar", "liga", "ligar"
                ))
                if (name.isNotEmpty()) {
                    resolveAndCall(name)
                } else {
                    speak("Para quem devo ligar?")
                    handler.postDelayed({ listenCycle() }, 2000)
                }
            }

            command.contains("abre") || command.contains("abrir") || command.contains("abra") || command.contains("abrir") -> {
                val appName = extractAfter(command, listOf(
                    "abre o", "abre a", "abre", "abrir o", "abrir a",
                    "abrir", "abra o", "abra a", "abra"
                ))
                if (appName.isNotEmpty()) {
                    resolveAndOpenApp(appName)
                } else {
                    speak("Qual aplicativo?")
                    handler.postDelayed({ listenCycle() }, 2000)
                }
            }

            command.contains("toca") || command.contains("tocar") || command.contains("musica") || command.contains("música") || command.contains("play") -> {
                val query = extractAfter(command, listOf(
                    "toca musica de", "toca musica do", "toca a musica", "toca musica",
                    "tocar musica", "play", "toca"
                ))
                val searchQuery = if (query.isEmpty()) "musicas populares brasileiras" else query
                speak("Tocando $searchQuery")
                JarbasAccessibilityService.instance?.openYoutubeAndPlay(searchQuery)
                handler.postDelayed({ listenCycle() }, 2000)
            }

            command.contains("sai") || command.contains("sair") || command.contains("fecha") || command.contains("fechar") || command.contains("volta") || command.contains("voltar") -> {
                speak("Ok.")
                JarbasAccessibilityService.instance?.pressBack()
                handler.postDelayed({ listenCycle() }, 1500)
            }

            command.contains("inicio") || command.contains("início") || command.contains("home") -> {
                JarbasAccessibilityService.instance?.pressHome()
                handler.postDelayed({ listenCycle() }, 1500)
            }

            else -> {
                speak("Nao entendi. Pode repetir?")
                handler.postDelayed({ listenCycle() }, 2000)
            }
        }
    }

    private fun resolveAndCall(name: String) {
        val contacts = JarbasAccessibilityService.instance?.findContacts(name, contentResolver) ?: emptyList()
        when {
            contacts.isEmpty() -> {
                speak("Nao encontrei contato com o nome $name")
                handler.postDelayed({ listenCycle() }, 2000)
            }
            contacts.size == 1 -> {
                speak("Ligando para ${contacts[0].first}")
                JarbasAccessibilityService.instance?.callNumber(contacts[0].second)
                handler.postDelayed({ listenCycle() }, 2000)
            }
            else -> {
                val names = contacts.take(2).mapIndexed { i, c -> "${i + 1}: ${c.first}" }
                pendingApps = contacts.take(2).map { it.second }
                speak("Encontrei dois. ${names[0]} ou ${names[1]}. Diga um ou dois.")
                awaitingCallConfirm = true
                handler.postDelayed({ listenCycle() }, 1000)
            }
        }
    }

    private fun resolveAndOpenApp(appName: String) {
        val pm = packageManager
        val allApps = pm.getInstalledApplications(0)
        val matched = allApps.filter {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
        }
        when {
            matched.isEmpty() -> {
                speak("Nao encontrei $appName")
                handler.postDelayed({ listenCycle() }, 2000)
            }
            matched.size == 1 -> {
                val label = pm.getApplicationLabel(matched[0]).toString()
                speak("Abrindo $label")
                openApp(matched[0].packageName)
                handler.postDelayed({ listenCycle() }, 2000)
            }
            else -> {
                val top = matched.take(2)
                val names = top.mapIndexed { i, app -> "${i + 1}: ${pm.getApplicationLabel(app)}" }
                pendingApps = top.map { it.packageName }
                speak("Encontrei dois. ${names[0]} ou ${names[1]}. Diga um ou dois.")
                awaitingAppChoice = true
                handler.postDelayed({ listenCycle() }, 1000)
            }
        }
    }

    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            speak("Nao consegui abrir o aplicativo.")
        }
    }

    private fun extractAfter(text: String, keywords: List<String>): String {
        val sorted = keywords.sortedByDescending { it.length }
        for (keyword in sorted) {
            if (text.contains(keyword)) {
                return text.substringAfter(keyword).trim()
            }
        }
        return ""
    }

    private fun extractNumber(text: String): Int {
        return when {
            text.contains("um") || text.contains("1") || text.contains("primeiro") -> 0
            text.contains("dois") || text.contains("2") || text.contains("segundo") -> 1
            text.contains("tres") || text.contains("três") || text.contains("3") || text.contains("terceiro") -> 2
            text.contains("quatro") || text.contains("4") || text.contains("quarto") -> 3
            else -> -1
        }
    }

    private fun speakThenStop(text: String) {
        speak(text)
        handler.postDelayed({
            stopListening()
            stopForeground(true)
            stopSelf()
        }, 2500)
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Jarbas", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarbas")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
