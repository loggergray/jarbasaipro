
package com.jarbas.app

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class JarbasForegroundService : Service(), TextToSpeech.OnInitListener {

    private val TAG = "JarbasForeground"
    private val CHANNEL_ID = "jarbas_channel"
    private val NOTIF_ID = 1
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isTtsSpeaking = false

    // ESTADOS ROBUSTOS
    var awaitingPassword = false
    var awaitingAppChoice = false
    var awaitingSearchChoice = false
    var awaitingNextResult = false
    var awaitingCallConfirm = false
    var currentSearchIndex = 0
    var pendingApps: List<String> = emptyList()
    var pendingContactNumber: String = ""

    companion object {
        const val FOREGROUND_SERVICE_TYPE_MIC = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(this, this).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { isTtsSpeaking = true }
                override fun onDone(utteranceId: String?) { isTtsSpeaking = false }
                override fun onError(utteranceId: String?) { isTtsSpeaking = false }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (checkMicPermission()) {
                    startForegroundService()
                } else {
                    stopSelf()
                    Log.e(TAG, "Permissão microfone negada")
                }
            }
            "STOP" -> {
                speakThenStop("Jarbas encerrado. Até logo.")
            }
        }
        return START_STICKY
    }

    private fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startForegroundService() {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            buildNotification("Jarbas ouvindo...", FOREGROUND_SERVICE_TYPE_MIC)
        } else {
            buildNotification("Jarbas ouvindo...", 0)
        }
        startForeground(NOTIF_ID, notification)
        startListening()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale("pt", "BR"))
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.05f)
            handler.postDelayed({ speakWelcome() }, 1000)
        } else {
            Log.e(TAG, "TTS init falhou: $status")
        }
    }

    private fun speakWelcome() {
        speak("Jarbas iniciado. Pode falar: Jarbas abre WhatsApp, liga pra mãe, pesquisa sobre Flutter.")
    }

    private fun startListening() {
        if (isTtsSpeaking) {
            handler.postDelayed({ startListening() }, 500)
            return
        }
        isListening = true
        handler.postDelayed({ listenCycle() }, 500)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun listenCycle() {
        if (!isListening || isTtsSpeaking) {
            handler.postDelayed({ listenCycle() }, 500)
            return
        }

        // ANTI-MEMORY LEAK: destroy anterior sempre
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
            putExtra(RecognizerIntent.EXTRA_NOISE_SUPPRESSION, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase()?.trim() ?: ""
                Log.d(TAG, "Reconheceu: '$text' (${matches?.size ?: 0} opções)")
                if (text.isNotEmpty()) {
                    processInput(text)
                } else {
                    handler.postDelayed({ listenCycle() }, 200)
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                // Preview para responsividade
                val partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                Log.v(TAG, "Parcial: $partial")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erro áudio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erro cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão"
                    SpeechRecognizer.ERROR_NETWORK -> "Sem internet"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout rede"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Não entendeu"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Erro servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout fala"
                    else -> "Erro $error"
                }
                Log.d(TAG, "Erro STT: $errorMsg ($error)")
                if (isListening) handler.postDelayed({ listenCycle() }, 1000)
            }

            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Falha startListening: ${e.message}")
            handler.postDelayed({ listenCycle() }, 1500)
        }
    }

    private fun processInput(text: String) {
        Log.d(TAG, "Processando: '$text'")
        when {
            awaitingPassword -> handlePassword(text)
            awaitingAppChoice -> handleAppChoice(text)
            awaitingCallConfirm -> handleCallConfirm(text)
            awaitingSearchChoice -> handleSearchChoice(text)
            awaitingNextResult -> handleNextResult(text)
            text.contains("jarbas") -> {
                val command = text.substringAfter("jarbas").trim()
                if (command.isEmpty()) speak("Sim?") else handleCommand(command)
            }
            else -> handler.postDelayed({ listenCycle() }, 300)
        }
    }

    // HANDLERS OTIMIZADOS
    private fun handlePassword(text: String) {
        awaitingPassword = false
        val digits = text.replaceNumbersToDigits().filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            speak("Digitando senha $digits")
            JarbasAccessibilityService.instance?.typePassword(digits)
        } else {
            speak("Não entendi a senha. Repita os números?")
            awaitingPassword = true
        }
        handler.postDelayed({ listenCycle() }, 2500)
    }

    private fun handleAppChoice(text: String) {
        awaitingAppChoice = false
        val choice = extractNumber(text)
        if (choice in 0 until pendingApps.size) {
            openApp(pendingApps[choice])
        } else {
            speak("Diga um, dois ou três.")
            awaitingAppChoice = true
        }
        handler.postDelayed({ listenCycle() }, 1500)
    }

    private fun handleCallConfirm(text: String) {
        awaitingCallConfirm = false
        val choice = extractNumber(text)
        if (choice in 0 until pendingApps.size) {
            speak("Ligando agora.")
            JarbasAccessibilityService.instance?.callNumber(pendingApps[choice])
        } else {
            speak("Diga um, dois ou três.")
            awaitingCallConfirm = true
        }
        handler.postDelayed({ listenCycle() }, 1500)
    }

    private fun handleSearchChoice(text: String) {
        awaitingSearchChoice = false
        val choice = extractNumber(text)
        if (choice >= 0 && choice < JarbasAccessibilityService.instance?.searchResults?.size ?: 0) {
            currentSearchIndex = choice
            JarbasAccessibilityService.instance?.readSearchResult(choice, this)
            awaitingNextResult = true
        } else {
            speak("Diga o número do resultado: um, dois, três ou quatro.")
            awaitingSearchChoice = true
        }
        handler.postDelayed({ listenCycle() }, 1000)
    }

    private fun handleNextResult(text: String) {
        awaitingNextResult = false
        if (text.containsAny(listOf("sim", "próximo", "mais", "continua", "seginte"))) {
            currentSearchIndex++
            val results = JarbasAccessibilityService.instance?.searchResults ?: emptyList()
            if (currentSearchIndex < results.size) {
                JarbasAccessibilityService.instance?.readSearchResult(currentSearchIndex, this)
                awaitingNextResult = true
            } else {
                speak("Não há mais resultados.")
            }
        } else {
            speak("Ok, voltando ao modo ouvir.")
        }
        handler.postDelayed({ listenCycle() }, 1000)
    }

    private fun String.replaceNumbersToDigits(): String {
        return this
            .replace("zero", "0").replace("zeroo", "0")
            .replace("um", "1").replace("uma", "1")
            .replace("dois", "2").replace("duas", "2")
            .replace("três", "3").replace("tres", "3").replace("treeees", "3")
            .replace("quatro", "4").replace("quatroo", "4")
            .replace("cinco", "5")
            .replace("seis", "6")
            .replace("sete", "7")
            .replace("oito", "8")
            .replace("nove", "9")
    }

    private fun String.containsAny(words: List<String>): Boolean {
        return words.any { this.contains(it) }
    }

    private fun handleCommand(command: String) {
        Log.d(TAG, "Executando comando: '$command'")
        when {
            command.containsAny(listOf("acorda", "desbloqueia", "desbloqueie", "tela")) -> {
                JarbasAccessibilityService.instance?.wakeAndUnlock()
                speak("Diga a senha agora.")
                awaitingPassword = true
            }
            command.containsAny(listOf("encerrar", "encerra", "para", "fechar jarbas", "desliga")) -> {
                speakThenStop("Encerrando Jarbas. Até logo!")
            }
            command.containsAny(listOf("obrigado", "valeu", "obrigada")) -> {
                speak("De nada! Estou aqui quando precisar.")
            }
            command.containsAny(listOf("socorro", "emergência", "ajuda urgente", "sos")) -> {
                speak("🚨 Emergência acionada!")
                JarbasAccessibilityService.instance?.triggerSOS(this)
            }
            command.containsAny(listOf("pesquisa", "busca", "pesquisar", "googla")) -> {
                val query = extractAfter(command, listOf("pesquisa", "sobre", "busca"))
                if (query.isNotBlank()) {
                    speak("Pesquisando: $query")
                    JarbasAccessibilityService.instance?.searchWeb(query, this)
                } else {
                    speak("O que devo pesquisar?")
                }
            }
            command.containsAny(listOf("liga", "ligar", "chama", "telefone")) -> {
                val name = extractAfter(command, listOf("liga", "pra", "para", "chama"))
                if (name.isNotBlank()) resolveAndCall(name) else speak("Quem devo ligar?")
            }
            command.containsAny(listOf("abre", "abrir", "roda", "executa")) -> {
                val app = extractAfter(command, listOf("abre", "abrir"))
                if (app.isNotBlank()) resolveAndOpenApp(app) else speak("Qual app?")
            }
            command.containsAny(listOf("musica", "música", "toca", "tocar", "play")) -> {
                val song = extractAfter(command, listOf("toca", "musica", "play"))
                val query = if (song.isBlank()) "músicas populares brasil" else song
                speak("Tocando $query no YouTube")
                JarbasAccessibilityService.instance?.openYoutubeAndPlay(query)
            }
            command.containsAny(listOf("volta", "voltar", "sai", "fecha", "back")) -> {
                speak("Voltando.")
                JarbasAccessibilityService.instance?.pressBack()
            }
            command.containsAny(listOf("home", "inicio", "tela inicial")) -> {
                JarbasAccessibilityService.instance?.pressHome()
            }
            else -> speak("Não entendi. Tente: Jarbas abre WhatsApp, liga pra pai, pesquisa IA.")
        }
    }

    private fun resolveAndCall(name: String) {
        val contacts = JarbasAccessibilityService.instance?.findContacts(name, contentResolver) ?: emptyList()
        when {
            contacts.isEmpty() -> speak("Contato '$name' não encontrado.")
            contacts.size == 1 -> {
                speak("Ligando para ${contacts[0].first}")
                JarbasAccessibilityService.instance?.callNumber(contacts[0].second)
            }
            else -> {
                val options = contacts.take(3).mapIndexed { i, c -> "${i+1}: ${c.first}" }
                pendingApps = contacts.take(3).map { it.second }
                speak("Encontrei ${options.size}. ${options.joinToString()}. Qual?")
                awaitingCallConfirm = true
            }
        }
    }

    private fun resolveAndOpenApp(appName: String) {
        val pm = packageManager
        val matched = pm.getInstalledApplications(0).filter {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName)
        }
        when {
            matched.isEmpty() -> speak("App '$appName' não encontrado.")
            matched.size == 1 -> {
                val label = pm.getApplicationLabel(matched[0])
                speak("Abrindo $label")
                openApp(matched[0].packageName)
            }
            else -> {
                val top = matched.take(3)
                val options = top.mapIndexed { i, app -> "${i+1}: ${pm.getApplicationLabel(app)}" }
                pendingApps = top.map { it.packageName }
                speak("Encontrei ${options.size}. ${options.joinToString()}. Qual?")
                awaitingAppChoice = true
            }
        }
    }

    private fun openApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } ?: speak("Falha ao abrir app.")
    }

    private fun extractAfter(text: String, keywords: List<String>): String {
        keywords.sortedByDescending { it.length }.forEach { kw ->
            if (text.contains(kw)) return text.substringAfterLast(kw).trim()
        }
        return ""
    }

    private fun extractNumber(text: String): Int {
        return when {
            text.containsAny(listOf("um", "1", "primeiro")) -> 0
            text.containsAny(listOf("dois", "2", "segundo")) -> 1
            text.containsAny(listOf("três", "tres", "3", "terceiro")) -> 2
            text.containsAny(listOf("quatro", "4", "quarto")) -> 3
            else -> -1
        }
    }

    private fun speakThenStop(text: String) {
        speak(text)
        handler.postDelayed({
            stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 3000)
    }

    fun speak(text: String) {
        if (isTtsSpeaking) {
            handler.postDelayed({ speak(text) }, 500)
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarbas_$System.currentTimeMillis()")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Jarbas Assistente", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Assistente de voz sempre ativo"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, serviceType: Int): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Jarbas Ativo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        if (::tts.isInitialized) tts.shutdown()
        Log.d(TAG, "JarbasForegroundService destruído")
        super.onDestroy()
    }
}
