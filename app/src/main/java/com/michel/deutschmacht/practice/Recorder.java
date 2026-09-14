package com.michel.deutschmacht.practice;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.IOException;

/** Minimal mic recorder/player for the Practice tab. All local, no permissions beyond RECORD_AUDIO. */
public final class Recorder {

    public interface Callback { void onState(boolean recording); }

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String currentFile;

    public String start(Context c) throws IOException {
        stopAll();
        currentFile = c.getExternalFilesDir(null) + "/practice_" + System.currentTimeMillis() + ".m4a";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(96000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(currentFile);
        recorder.prepare();
        recorder.start();
        return currentFile;
    }

    public void stopRecording() {
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
            recorder.release();
            recorder = null;
        }
    }

    public boolean isRecording() { return recorder != null; }

    public void play(String path) throws IOException {
        stopAll();
        player = new MediaPlayer();
        player.setDataSource(path);
        player.prepare();
        player.start();
    }

    public boolean isPlaying() {
        try { return player != null && player.isPlaying(); } catch (Exception e) { return false; }
    }

    public String lastFile() { return currentFile; }

    public void stopAll() {
        stopRecording();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
    }
}
