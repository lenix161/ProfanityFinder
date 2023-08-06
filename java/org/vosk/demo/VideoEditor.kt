package org.vosk.demo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream

class VideoEditor(private val context: Context, private val listener: ConverterListener): ConverterListener by listener{
    private val VIDEO = "video.mp4"
    private val AUDIO = "audio.wav"
    private var directory: File = File(context.filesDir, "folder")

    /**
     * Convert video to audio.
     *
     * @param uri source video;
     * @param format output audio format (wav, mp3, etc.);
     * @param codec audio codec;
     * @param channels count of audio channels (1 or 2).
     * */
    fun convertToAudio(uri: Uri, format: String, codec: String, channels: Int){
        deleteFilesIfExist()
        Log.i("Vosk", "FILES DELETED")
        uriToFileInternalStorage(uri)
        Log.i("Vosk", "FILES COPIED")
        extractAudio(
            format = format,
            codec = codec,
            channels = channels
        )
    }
    /**
     * Delete file video.mp4 and audio.wav if exists
     * */
    private fun deleteFilesIfExist(){
        val dir = File(context.filesDir, "folder")
        val vid = File(dir,VIDEO)
        if (vid.exists()){
            Log.i("Vosk", "$vid deleted")
            vid.deleteRecursively()
        }
        val aud = File(dir, AUDIO)
        if (aud.exists()){
            Log.i("Vosk", "$aud deleted")
            aud.deleteRecursively()
        }
    }

    /**
     * Copy video into Internal Storage
     */
    private fun uriToFileInternalStorage(uri: Uri){
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream.use {
            if (!directory.isDirectory){
                Log.i("Vosk", "make dir: $directory")
                directory.mkdir()
            }
            val file = File(directory, VIDEO)
            val output = FileOutputStream(file)
            output.use {
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int = inputStream?.read(buffer) ?: -1
                while (read != -1) {
                    output.write(buffer, 0, read)
                    read = inputStream?.read(buffer) ?: -1
                }
                output.flush()
            }
        }
        for (i in directory.listFiles()!!){
            Log.i("Vosk", "copied $i")
        }
    }

    /**
     * Extracting audio from video by using FFMPEG library
     * */
    private fun extractAudio(format: String, codec: String, channels: Int): Int{
        val command = "-i $directory/$VIDEO -f $format -ac $channels -acodec $codec -ar 16000 -vn $directory/$AUDIO"
        Log.i("Vosk", "command: $command")
        var result = 0
        FFmpeg.executeAsync(command) { executionId, returnCode ->
            Log.i("Vosk", "FFMPEG: $executionId $returnCode")
            val b = directory.listFiles()
            Log.i("Vosk", "created files")
            for (i in b!!){
                Log.i("Vosk", "$i")
            }
            result = returnCode
            listener.onAudioExtracted()
        }
        return  result
    }


    fun editAudio(){
        val video = File(File(context.filesDir, "folder"), "output_video.mp4")
        if (video.exists()){
            video.deleteRecursively()
        }
        var times = ""
        if (Data.timings.size != 0){
            for (i in Data.timings.indices){
                if (i == 0){
                    times += "between(t, ${Data.timings[i].first}, ${Data.timings[i].second})"
                } else {
                    times += " + between(t, ${Data.timings[i].first}, ${Data.timings[i].second})"
                }

            }
            val command = """-i $directory/$VIDEO -filter_complex "[0:a]volume=0:enable='$times'[silenced];[silenced]amix=inputs=1[a]" -map 0:v -map "[a]" -c:v copy -c:a aac $directory/output_video.mp4""".trimMargin()
            Log.i("Vosk", "command: $command")
            FFmpeg.executeAsync(command){executionId, returnCode ->
                Log.i("Vosk", "FFMPEG: $executionId $returnCode")
                val b = directory.listFiles()
                Log.i("Vosk", "created files")
                for (i in b!!){
                    Log.i("Vosk", "$i")
                }
                listener.onVideoEdited()
            }
        }else{
            val command = "-i $directory/$VIDEO -c copy $directory/output_video.mp4"
            Log.i("Vosk", "command: $command")
            FFmpeg.executeAsync(command){executionId, returnCode ->
                Log.i("Vosk", "FFMPEG: $executionId $returnCode")
                val b = directory.listFiles()
                Log.i("Vosk", "created files")
                for (i in b!!){
                    Log.i("Vosk", "$i")
                }
                listener.onVideoEdited()
            }
        }
    }
}