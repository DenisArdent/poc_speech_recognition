package com.denisardent.pocspeechrecognition

enum class RecordState{
    RECORD_START,
    RECORD_STOPPED,
    RECORD_ON_PAUSE
}

const val DYNAMIC_MODEL = "dynamicmodel"

data class UiState(
    val isLoadingModel: Boolean,
    val isModuleDelivered: Boolean?,
    val isRecordingRunning: Boolean
)
