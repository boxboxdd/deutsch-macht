package com.michel.deutschmacht.practice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.IOException

/** Mic recorder / player for the Practice tab. Everything stays on the device. */
class Recorder {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: String? = null

    val isRecording: Boolean get() = recorder != null

    val isPlaying: Boolean
        get() = try {
            player?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    fun lastFile(): String? = currentFile

    @Throws(IOException::class)
    fun start(context: Context): String {
        stopAll()
        val file = "${context.getExternalFilesDir(null)}/practice_${System.currentTimeMillis()}.m4a"
        currentFile = file
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file)
            prepare()
            start()
        }
        return file
    }

    fun stopRecording() {
        recorder?.let {
            try { it.stop() } catch (ignored: Exception) {}
            it.release()
        }
        recorder = null
    }

    @Throws(IOException::class)
    fun play(path: String) {
        stopAll()
        player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
        }
    }

    fun stopAll() {
        stopRecording()
        player?.let {
            try { it.stop() } catch (ignored: Exception) {}
            it.release()
        }
        player = null
    }
}
