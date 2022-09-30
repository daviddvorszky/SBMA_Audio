package com.example.sbma_audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.sbma_audio.ui.theme.SBMA_AudioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    lateinit var inputStream: InputStream
    lateinit var lastRecord: File

    private val recRunning = mutableStateOf(false)
    private val lastExists = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasPermission()

        setContent {
            val buttonText = if(recRunning.value) "Stop" else "Record"

            SBMA_AudioTheme {
                // A surface container using the 'background' color from the theme
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Audio Lab")

                    Button(onClick = {
                        recRunning.value = !recRunning.value
                        if(recRunning.value){
                            GlobalScope.launch(Dispatchers.Main) {
                                async(Dispatchers.Default) {
                                    lastRecord = recordAudio()
                                    lastExists.value = true
                                }
                            }
                        }
                    }){
                        Text(buttonText)
                    }

                    Button(onClick = {
                        GlobalScope.launch(Dispatchers.Main) {
                            inputStream = lastRecord.inputStream()
                            async(Dispatchers.Default) { playAudio(inputStream) }
                        }
                    },
                        enabled = lastExists.value
                    ) {
                        Text("Play Last Record")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordAudio(): File{
        val recFileName = "recording${LocalTime.now().toString()}.raw"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        var recFile: File? = null
        try{
            recFile = File(storageDir.toString() + "/" + recFileName)
        }catch (e: IOException){
            Log.e("pengb", "Can't create audio file $e")
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            44100, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val aFormat: AudioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(aFormat)
            .setBufferSizeInBytes(minBufferSize)
            .build()
        val audioData = ByteArray(minBufferSize)

        try{
            val outputStream = FileOutputStream(recFile)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)

            recorder.startRecording()
            while(recRunning.value){
                val numofBytes = recorder.read(audioData, 0, minBufferSize)
                if(numofBytes > 0){
                    dataOutputStream.write(audioData)
                }
            }
            recorder.stop()
            dataOutputStream.close()
        }catch (e: IOException){
            Log.d("pengb", e.toString())
        }
        return recFile!!
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
        track.setVolume(2f)

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