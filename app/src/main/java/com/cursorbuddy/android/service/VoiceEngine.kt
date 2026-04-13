package com.cursorbuddy.android.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceEngine(private val context: Context) {

    companion object {
        private const val TAG = "CursorBuddy"
        // If the recognizer errors out within this window after start, treat it
        // as a transient warmup failure and retry once silently.
        private const val WARMUP_RETRY_WINDOW_MS = 1200L
    }

    interface VoiceCallback {
        fun onListeningStarted()
        fun onPartialResult(text: String)
        fun onResult(text: String)
        fun onListeningError(error: String)
        fun onSpeakingStarted()
        fun onSpeakingDone()
    }

    var callback: VoiceCallback? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isListening = false

    // Retry bookkeeping
    private var startTimeMs = 0L
    private var didBeginSpeech = false
    private var retriedOnce = false

    init {
        // Init TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.0f)
                ttsReady = true
                Log.d(TAG, "TTS initialized")

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        callback?.onSpeakingStarted()
                    }
                    override fun onDone(utteranceId: String?) {
                        callback?.onSpeakingDone()
                    }
                    override fun onError(utteranceId: String?) {
                        callback?.onSpeakingDone()
                    }
                })
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }

        // Warm up the speech recognizer once so the first tap doesn't pay init cost.
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-create speech recognizer", e)
            }
        }
    }

    fun startListening() {
        retriedOnce = false
        startInternal()
    }

    private fun startInternal() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback?.onListeningError("Speech recognition not available")
            return
        }

        // Reuse recognizer if we already have one; only recreate if null.
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create speech recognizer", e)
                callback?.onListeningError("Could not start voice input")
                return
            }
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                callback?.onListeningStarted()
                Log.d(TAG, "Listening...")
            }

            override fun onBeginningOfSpeech() {
                didBeginSpeech = true
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                Log.d(TAG, "End of speech")
            }

            override fun onError(error: Int) {
                isListening = false
                val elapsed = System.currentTimeMillis() - startTimeMs
                Log.e(TAG, "Speech error code=$error elapsed=${elapsed}ms didBegin=$didBeginSpeech retried=$retriedOnce")

                // Transient warmup errors: some devices fire NO_MATCH / CLIENT / ERROR (5)
                // the first time the recognizer is used after process start. Retry once
                // silently so the user doesn't see a bogus "Speech error".
                val isTransient = !didBeginSpeech &&
                    elapsed < WARMUP_RETRY_WINDOW_MS &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_CLIENT ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

                if (isTransient && !retriedOnce) {
                    retriedOnce = true
                    Log.d(TAG, "Transient error; retrying voice input silently")
                    // Fully rebuild the recognizer before the retry — some OEM stacks
                    // leave the instance in a bad state after an early error.
                    try { speechRecognizer?.destroy() } catch (_: Exception) {}
                    speechRecognizer = null
                    mainHandler.postDelayed({ startInternal() }, 150)
                    return
                }

                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again?"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic and speak."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Still listening — give me a sec"
                    else -> "Speech error ($error)"
                }
                callback?.onListeningError(msg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Log.d(TAG, "Speech result: $text")
                if (text.isNotBlank()) {
                    callback?.onResult(text)
                } else {
                    callback?.onListeningError("Didn't catch that. Try again?")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    callback?.onPartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        startTimeMs = System.currentTimeMillis()
        didBeginSpeech = false
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            callback?.onListeningError("Could not start voice input")
        }
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
    }

    fun speak(text: String) {
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready")
            return
        }
        // Stop any current speech
        tts?.stop()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.8f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "caption_${System.currentTimeMillis()}")
        Log.d(TAG, "Speaking: $text")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun destroy() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
