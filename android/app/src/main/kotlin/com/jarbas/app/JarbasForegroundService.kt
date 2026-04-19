package com.jarbas.app

import android.app.*
import android.content.Intent
import android.os.IBinder
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

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var awaitingCommand = false
    private var awaitingPassword = false
    private var awaitingAppChoice = false
    private var pendingApps: List<String> = emptyList()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                startForeground(NOTIF_ID, buildNotification("Jarbas ativo e ouvindo..."))
                startListening()
            }
            "STOP" -> {
                speak("Jarbas encerrado. Ate logo.")
                stopListening()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("pt", "BR")
            speak("Jarbas iniciado. Pode falar.")
        }
    }

    private fun startListening() {
        isListening = true
        listenForWakeWord()
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun listenForWakeWord() {
        if (!isListening) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase() ?: ""
                Log.d(TAG, "Ouviu: $text")
                processInput(text)
            }
            override fun onError(error: Int) {
                if (isListening) listenForWakeWord()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun processInput(text: String) {
        when {
            awaitingPassword -> {
                awaitingPassword = false
                val password = text.replace(" ", "")
                JarbasAccessibilityService.instance?.typePassword(password)
                speak("Digitando senha.")
                listenForWakeWord()
            }
            awaitingAppChoice -> {
                awaitingAppChoice = false
                val choice = when {
                    text.contains("1") || text.contains("um") || text.contains("primeiro") -> 0
                    text.contains("2") || text.contains("dois") || text.contains("segundo") -> 1
                    else -> -1
                }
                if (choice >= 0 && choice < pendingApps.size) {
                    openApp(pendingApps[choice])
                } else {
                    speak("Nao entendi. Diga um ou dois.")
                    awaitingAppChoice = true
                }
                listenForWakeWord()
            }
            text.contains("jarbas") -> {
                val command = text.substringAfter("jarbas").trim()
                handleCommand(command)
            }
            else -> listenForWakeWord()
        }
    }

    private fun handleCommand(command: String) {
        when {
            command.contains("acorda") -> {
                speak("Qual a senha do celular?")
                awaitingPassword = true
                listenForWakeWord()
            }
            command.contains("encerrar") || command.contains("encerra") -> {
                speak("Encerrando Jarbas. Ate logo.")
                stopListening()
                stopForeground(true)
                stopSelf()
            }
            command.contains("obrigado") -> {
                speak("Disponha! E so chamar.")
                listenForWakeWord()
            }
            command.contains("liga") || command.contains("ligar") -> {
                val name = extractAfter(command, listOf("liga pra", "ligar pra", "liga para", "ligar para"))
                if (name.isNotEmpty()) {
                    speak("Ligando para $name")
                    JarbasAccessibilityService.instance?.callContact(name)
                } else {
                    speak("Para quem devo ligar?")
                }
                listenForWakeWord()
            }
            command.contains("abre") || command.contains("abrir") || command.contains("abra") -> {
                val appName = extractAfter(command, listOf("abre o", "abre a", "abre", "abrir o", "abrir a", "abrir", "abra o", "abra a", "abra"))
                if (appName.isNotEmpty()) {
                    resolveAndOpenApp(appName)
                } else {
                    speak("Qual aplicativo devo abrir?")
                    listenForWakeWord()
                }
            }
            command.contains("toca") || command.contains("tocar") || command.contains("musica") -> {
                val query = extractAfter(command, listOf("toca musica de", "toca musica do", "toca musica", "tocar musica", "toca"))
                val searchQuery = if (query.isEmpty()) "musicas populares" else query
                speak("Abrindo YouTube e tocando $searchQuery")
                JarbasAccessibilityService.instance?.openYoutubeAndPlay(searchQuery)
                listenForWakeWord()
            }
            command.contains("sai") || command.contains("sair") || command.contains("fechar") || command.contains("fecha") -> {
                speak("Fechando.")
                JarbasAccessibilityService.instance?.pressBack()
                listenForWakeWord()
            }
            command.contains("voltar") || command.contains("volta") -> {
                JarbasAccessibilityService.instance?.pressBack()
                listenForWakeWord()
            }
            command.isEmpty() -> {
                speak("Sim? O que deseja?")
                listenForWakeWord()
            }
            else -> {
                speak("Nao entendi o comando. Pode repetir?")
                listenForWakeWord()
            }
        }
    }

    private fun resolveAndOpenApp(appName: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val matched = apps.filter {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
        }

        when {
            matched.isEmpty() -> {
                speak("Nao encontrei o aplicativo $appName")
                listenForWakeWord()
            }
            matched.size == 1 -> {
                speak("Abrindo ${pm.getApplicationLabel(matched[0])}")
                openApp(matched[0].packageName)
                listenForWakeWord()
            }
            else -> {
                val names = matched.take(2).mapIndexed { i, app ->
                    "${i + 1}: ${pm.getApplicationLabel(app)}"
                }
                pendingApps = matched.take(2).map { it.packageName }
                speak("Encontrei dois. ${names[0]} ou ${names[1]}. Diga um ou dois.")
                awaitingAppChoice = true
                listenForWakeWord()
            }
        }
    }

    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun extractAfter(text: String, keywords: List<String>): String {
        for (keyword in keywords) {
            if (text.contains(keyword)) {
                return text.substringAfter(keyword).trim()
            }
        }
        return ""
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Jarbas", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Jarbas assistente de voz"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarbas")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
