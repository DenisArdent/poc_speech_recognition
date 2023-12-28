package com.denisardent.pocspeechrecognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.denisardent.pocspeechrecognition.ui.theme.PocSpeechRecognitionTheme
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class MainActivity : ComponentActivity(), RecognitionListener {
    lateinit var model: Model
    lateinit var recognizer: Recognizer
    private var speechService: SpeechService? = null
    val uiState = MutableStateFlow(UiState(true, null, false))
    val textState = MutableStateFlow("")
    lateinit var splitInstallManager: SplitInstallManager

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionCheck =
            this.checkSelfPermission(Manifest.permission.RECORD_AUDIO)

        splitInstallManager = SplitInstallManagerFactory.create(this)
        setContent {
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1
                )
            } else {
                initModel()
            }
            val state = uiState.collectAsStateWithLifecycle(this)
            val tvState = textState.collectAsStateWithLifecycle(this)
            PocSpeechRecognitionTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    TranscriptionBox(state.value.isLoadingModel, state.value.isModuleDelivered, state.value.isRecordingRunning, tvState.value, Modifier.align(Alignment.Center))
                }
            }

        }
    }

    fun initModel(){
        val installedModules: Set<String> = splitInstallManager.installedModules
        if (installedModules.contains(DYNAMIC_MODEL)){
            Log.d("PFD", "Module is installed")
        } else{
            Log.d("PFD", "Module is not installed")
            val request =
                SplitInstallRequest
                    .newBuilder()
                    .addModule("@string/title_dynamicmodel")
                    .build()
            lifecycleScope.launch {
                splitInstallManager
                    .startInstall(request)
                    .addOnSuccessListener { sessionId ->
                        uiState.update { state ->
                            UiState(state.isLoadingModel, true, state.isRecordingRunning)
                        }
                        Log.d("PFD", "SUCCESS")
                    }
                    .addOnFailureListener { exception ->
                        uiState.update { state ->
                            UiState(state.isLoadingModel, false, state.isRecordingRunning)
                        }
                        Log.e("PFD","EXCEPTION ${exception.message}")
                    }
            }
        }


        StorageService.unpack(this, "model-en-us", "model",
            { model ->
                this.model = model
                lifecycleScope.launch {
                    uiState.update { state ->
                        UiState(isLoadingModel = false, isModuleDelivered = state.isModuleDelivered, state.isRecordingRunning)
                    }
                }
                Log.d("VOSK","FINISH")
            }
        ) { exception: IOException ->
            Log.e("VOSK","EXCEPTION ${exception.message}")
        }
    }



    override fun onPartialResult(hypothesis: String?) {
    }

    override fun onResult(hypothesis: String?) {
        lifecycleScope.launch {
            if (hypothesis !=null){
                textState.emit(hypothesis)
            }
        }

    }

    override fun onFinalResult(hypothesis: String?) {
        lifecycleScope.launch {
            if (hypothesis !=null){
                textState.emit(hypothesis)
            }
        }
    }

    override fun onError(exception: Exception?) {

    }

    override fun onTimeout() {

    }

    fun startRecording(){
        if (speechService != null) {
            lifecycleScope.launch {
                uiState.update { state ->
                    UiState(state.isLoadingModel, state.isModuleDelivered, false)
                }
            }
            speechService!!.stop()
            speechService = null
        } else {
            lifecycleScope.launch {
                uiState.update { state ->
                    UiState(state.isLoadingModel, state.isModuleDelivered, true)
                }
            }
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
            } catch (e: IOException) {
            }
        }
    }

    @Composable
    fun TranscriptionBox(loading: Boolean, moduleDelivered: Boolean?, isRecordingRunning: Boolean, text: String, modifier: Modifier = Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            if (moduleDelivered != null){
                if (moduleDelivered){
                    Text(modifier = modifier, color = Color.Green, text = "Module Delivered")
                } else{
                    Text(modifier = modifier, color = Color.Red, text = "exception")
                }

            } else{
                Text(modifier = modifier, text = "Wait")
            }

            Text(text = text)

            Button(
                modifier = modifier,
                onClick = {startRecording()},
                enabled = !loading
            ){
                Text(text = "Начать транскрибацию", modifier = modifier)
            }
            if (isRecordingRunning)
                Button(
                    modifier = modifier,
                    onClick = {

                    },
                    enabled = !loading
                ){
                    Text(text = "Остановить запись", modifier = modifier)
                }
            if (loading){
                CircularProgressIndicator(modifier = modifier)
            }
        }
    }
}




