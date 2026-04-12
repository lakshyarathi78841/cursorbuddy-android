package com.cursorbuddy.android.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isListening = false

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
    }

    fun startListening() {
        if (isListening) return
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback?.onListeningError("Speech recognition not available")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                callback?.onListeningStarted()
                Log.d(TAG, "Listening...")
            }
            
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                isListening = false
                Log.d(TAG, "End of speech")
            }
            
            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again?"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic and speak."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                    else -> "Speech error ($error)"
                }
                Log.e(TAG, "Speech error: $msg")
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
        }

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
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
