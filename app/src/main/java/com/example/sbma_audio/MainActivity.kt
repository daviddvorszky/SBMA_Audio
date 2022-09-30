package com.example.sbma_audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.example.sbma_audio.ui.theme.SBMA_AudioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    lateinit var inputStream: InputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasPermission()

        setContent {
            SBMA_AudioTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Text("Audio Lab")
                    Button(onClick = {
                        GlobalScope.launch(Dispatchers.Main) {
                            inputStream = resources.openRawResource(R.raw.when_you_try_your_best)
                            async(Dispatchers.Default) { playAudio(inputStream) }
                        }
                    }) {
                        Text("Play Audio")
                    }
                }
            }
        }


    }

    private fun playAudio(istream: InputStream) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            44100, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val aBuilder = AudioTrack.Builder()
        val aAttr: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val aFormat: AudioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val track = aBuilder.setAudioAttributes(aAttr)
            .setAudioFormat(aFormat)
            .setBufferSizeInBytes(minBufferSize)
            .build()
        track.setVolume(1f)

        track.play()
        var i = 0
        val buffer = ByteArray(minBufferSize)
        try {
            i = istream.read(buffer, 0, minBufferSize)
            while(i != -1){
                track.write(buffer, 0, i)
                i = istream.read(buffer, 0, minBufferSize)
            }
        }catch (e: IOException){
            Log.e("pengb", "Stream read error $e")
        }
        try {
            istream.close()
        }catch (e: IOException){
            Log.e("pengb", "Close error $e")
        }

        track.stop()
        track.release()
    }

    private fun hasPermission(): Boolean {
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            Log.d("pengb", "No audio access")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return true // assuming that the user grants permission
        }
        return true
    }
}