package org.vosk.demo.activities

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.vosk.demo.*
import org.vosk.demo.databinding.PlayerActivityBinding
import java.io.File


class PlayerActivity:AppCompatActivity(), VideoPlayerRecyclerViewAdapter.OnClickListener, ConverterListener {
    private lateinit var binding: PlayerActivityBinding
    private lateinit var adapter: VideoPlayerRecyclerViewAdapter
    private var isVideoPlaying = false
    private lateinit var updateTimerThread: Thread



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rcViewContainer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = VideoPlayerRecyclerViewAdapter(this)
        binding.rcViewContainer.adapter = adapter

        if (Data.timings.isEmpty()){
            binding.listEmptyTitle.visibility = View.VISIBLE
            binding.rcViewContainer.visibility = View.GONE
        } else {
            binding.listEmptyTitle.visibility = View.GONE
            binding.rcViewContainer.visibility = View.VISIBLE
        }

        val video = File(File(filesDir, "folder"), "output_video.mp4")
        setupPlayer(video)

        binding.updateVideoButton.setOnClickListener { updateVideo() }
        binding.saveVideoButton.setOnClickListener { saveVideo() }


    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::updateTimerThread.isInitialized){
            updateTimerThread.interrupt()
            updateTimerThread.join()
        }
    }

    private fun setupPlayer(video: File){
        binding.videoView.stopPlayback()
        binding.videoView.setVideoPath(video.path)


        binding.videoView.setOnPreparedListener{
            var time = ""
            val timeMS = binding.videoView.duration

            val mins = timeMS / 1000 / 60
            if (mins < 10){
                time += "0$mins:"
            } else {
                time += "$mins:"
            }

            val sec = timeMS / 1000 % 60
            if (sec < 10){
                time += "0$sec:"
            } else {
                time += "$sec:"
            }

            val ms = timeMS % 1000 / 10
            if (ms < 10){
                time += "0$ms"
            } else {
                time += "$ms"
            }


            binding.videoLengthTimeTitle.text = time
            binding.seekBar.min = 0
            binding.seekBar.max = timeMS

            updateTimerThread = Thread(task)
            updateTimerThread.start()
        }

        binding.playPauseButton.setOnClickListener {
            if (isVideoPlaying){
                binding.videoView.pause()
                binding.playPauseButton.setImageDrawable(resources.getDrawable(R.drawable.baseline_play_arrow_24))
                isVideoPlaying = false
            } else {
                binding.videoView.start()
                binding.playPauseButton.setImageDrawable(resources.getDrawable(R.drawable.baseline_pause_24))
                isVideoPlaying = true
            }
            val cur = binding.videoView.currentPosition
            Log.i("Vosk", "cur pos $cur")
        }

        binding.forward5sButton.setOnClickListener {
            val cur = binding.videoView.currentPosition
            binding.videoView.seekTo(cur + 5000)
        }

        binding.back5sButton.setOnClickListener {
            val cur = binding.videoView.currentPosition
            binding.videoView.seekTo(cur - 5000)
        }

        binding.videoView.setOnCompletionListener {
            binding.playPauseButton.setImageDrawable(resources.getDrawable(R.drawable.baseline_play_arrow_24))
            isVideoPlaying = false
        }

        binding.addTimingButton.setOnClickListener { addTiming() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser){
                    binding.videoView.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // you can probably leave this empty
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // you can probably leave this empty
            }
        })
    }

    private fun addTiming(){
        AddTimingDialog(adapter).show(supportFragmentManager, "AddTimingDialog")
        binding.listEmptyTitle.visibility = View.GONE
        binding.rcViewContainer.visibility = View.VISIBLE
    }

    private fun updateVideo(){
        val editor = VideoEditor(this,this)
        updateTimerThread.interrupt()
        updateTimerThread.join()
        editor.editAudio()
    }

    private fun saveVideo(){
        val video = File(File(filesDir, "folder"), "output_video.mp4").readBytes()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API version >= 29 (Android 10, 11, ...)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "output_video.mp4")
                put(MediaStore.Downloads.MIME_TYPE, "application/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = this.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            Toast.makeText(this, "Сохранено в Downloads", Toast.LENGTH_LONG).show()

            if (uri != null) {
                val fileOutputStream = resolver.openOutputStream(uri)
                fileOutputStream?.write(video)
                fileOutputStream?.close()

            } else {
                Toast.makeText(this, "Can\'t resolve media path", Toast.LENGTH_LONG).show()
            }
        } else {
            // API version < 29 (Android ..., 7,8,9)
            val outputFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "output_video.mp4"
            )
            outputFile.writeBytes(video)
        }
    }

    private val task = Runnable {
        while (!Thread.interrupted()){
            val curTime = binding.videoView.currentPosition

            var time = ""

            val mins = curTime / 1000 / 60
            if (mins < 10){
                time += "0$mins:"
            } else {
                time += "$mins:"
            }

            val sec = curTime / 1000 % 60
            if (sec < 10){
                time += "0$sec:"
            } else {
                time += "$sec:"
            }

            val ms = curTime % 1000 / 10
            if (ms < 10){
                time += "0$ms"
            } else {
                time += "$ms"
            }


            runOnUiThread {
                binding.videoCurrentTimeTitle.text = time
                binding.seekBar.progress = curTime
            }

        }
    }


    override fun onClickPlay(time: Pair<String, String>) {
        val ms = time.first.toFloat()*1000
        binding.videoView.seekTo(ms.toInt())
        val cur = binding.videoView.currentPosition
        Log.i("Vosk", "video jump to $ms, cur pos $cur")

    }

    override fun onClickDelete(time: Pair<String, String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Удалить?")
            .setPositiveButton("Да") { dialog, id ->
                adapter.deleteTiming(time)
                if (Data.timings.isEmpty()){
                    binding.listEmptyTitle.visibility = View.VISIBLE
                    binding.rcViewContainer.visibility = View.GONE
                } else {
                    binding.listEmptyTitle.visibility = View.GONE
                    binding.rcViewContainer.visibility = View.VISIBLE
                }
            }
            .setNeutralButton("Нет") { dialog, id ->
                // User cancelled the dialog
            }
        // Create the AlertDialog object and return it
        builder.create()
        builder.show()
    }

    override fun onAudioExtracted() {
        TODO("Not yet implemented")
    }

    override fun onVideoEdited() {
        Toast.makeText(this, "Video edited", Toast.LENGTH_SHORT).show()
        val video = File(File(filesDir, "folder"), "output_video.mp4")
        setupPlayer(video)
    }
}