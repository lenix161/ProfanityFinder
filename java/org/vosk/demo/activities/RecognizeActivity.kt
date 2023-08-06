package org.vosk.demo.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import org.vosk.demo.ConverterListener
import org.vosk.demo.Data
import org.vosk.demo.VideoEditor
import org.vosk.demo.databinding.RecognizeActivityBinding
import java.io.File
import java.io.IOException
import java.util.Scanner

class RecognizeActivity : AppCompatActivity(), RecognitionListener, ConverterListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null
    private lateinit var binding: RecognizeActivityBinding
    private lateinit var editor: VideoEditor
    private val badWords = mutableListOf<String>()


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = RecognizeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val path = intent.getStringExtra("videoUri")
        val uri = Uri.parse(path)

        editor = VideoEditor(this, this)
        editor.convertToAudio(
                    uri = uri,
                    format = "wav",
                    codec = "pcm_s16le",
                    channels = 1)

        binding.nextActivity.visibility = View.INVISIBLE
        binding.nextActivity.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        // Setup layout
        setUiState(STATE_START)

        LibVosk.setLogLevel(LogLevel.INFO)


        val scanner = Scanner(assets.open("bad_words.txt"))
        while (scanner.hasNextLine()){
            badWords.add(scanner.nextLine())
        }

        initModel()
    }

    private fun initModel() {
        StorageService.unpack(this, "vosk_small_model", "model",
            { model: Model? ->
                this.model = model
                setUiState(STATE_READY)
                recognizeFile()
            }
        ) { exception: IOException ->
            setErrorState(
                "Failed to unpack the model" + exception.message
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
        }
        if (speechStreamService != null) {
            speechStreamService!!.stop()
        }
    }

    override fun onResult(hypothesis: String) {
        findBadWords(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        findBadWords(hypothesis)
        setUiState(STATE_DONE)
        if (speechStreamService != null) {
            speechStreamService = null
        }
        editor.editAudio()
    }

    override fun onPartialResult(hypothesis: String) {
    }

    override fun onError(e: Exception) {
        setErrorState(e.message!!)
    }

    override fun onTimeout() {
        setUiState(STATE_DONE)
    }

    private fun setUiState(state: Int) {
        when (state) {
            STATE_START -> with(binding){
                resultText.movementMethod = ScrollingMovementMethod()
            }
            STATE_AUDIO_EXTRACTED -> with(binding){
                progressBar0.visibility = View.INVISIBLE
                done0.visibility = View.VISIBLE
            }
            STATE_READY -> with(binding){
                progressBar1.visibility = View.INVISIBLE
                done1.visibility = View.VISIBLE
            }
            STATE_DONE -> with(binding){
                progressBar2.visibility = View.INVISIBLE
                done2.visibility = View.VISIBLE
            }
            STATE_VIDEO_EDITED -> with(binding){
                progressBar3.visibility = View.INVISIBLE
                done3.visibility = View.VISIBLE
                nextActivity.visibility = View.VISIBLE
            }
            STATE_FILE -> with(binding){

            }

            else -> throw IllegalStateException("Unexpected value: $state")
        }
    }

    private fun setErrorState(message: String) {
        with(binding){
            resultText.text = message
        }

    }

    private fun recognizeFile() {
        if (speechStreamService != null) {
            setUiState(STATE_DONE)
            speechStreamService!!.stop()
            speechStreamService = null
        } else {
            setUiState(STATE_FILE)
            try {
                Data.timings.clear()
                val rec = Recognizer(model, 16000.0f, )
                val ais = File(filesDir, "folder/audio.wav").inputStream()

                speechStreamService = SpeechStreamService(rec, ais, 16000.0f)

                rec.setWords(true)
                rec.setPartialWords(false)
                speechStreamService!!.start(this)
            } catch (e: IOException) {
                setErrorState(e.message!!)
            }
        }
    }

    private fun findBadWords(result: String){
        val json = Json.parseToJsonElement(result)
        val wordsArray = json.jsonObject["result"]?.jsonArray
        if (wordsArray != null) {
            for (item in wordsArray){
                val start = item.jsonObject["start"].toString().dropLast(4)
                val end = item.jsonObject["end"].toString().dropLast(4)
                val word = item.jsonObject["word"].toString().drop(1).dropLast(1)
                Log.i("Vosk", "word - $word, start - $start, end - $end")
                if (badWords.contains(word)) {
                    Data.timings.add(Pair(start, end))
                    binding.resultText.append("- *** ($start - $end сек.)\n")
               } else {
                    binding.resultText.append("- $word ($start - $end сек.)\n")
               }
            }
        }
    }

    companion object {
        private const val STATE_START = 0
        private const val STATE_READY = 1
        private const val STATE_DONE = 2
        private const val STATE_FILE = 3
        private const val STATE_AUDIO_EXTRACTED = 4
        private const val STATE_VIDEO_EDITED = 5

        /* Used to handle permission request */
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1


    }

    override fun onAudioExtracted() {
        setUiState(STATE_AUDIO_EXTRACTED)
    }

    override fun onVideoEdited() {
        setUiState(STATE_VIDEO_EDITED)
    }
}