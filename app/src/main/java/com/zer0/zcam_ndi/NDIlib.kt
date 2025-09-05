package com.zer0.zcam_ndi

object NDIlib {

    init {
        System.loadLibrary("ndi") // Lib gerada pelo CMake
    }

    // --- Funções JNI ---
    external fun initialize(): Boolean                        // Inicializa a lib NDI
    external fun sendCreate(name: String): Long              // Cria sender NDI
    external fun sendVideo(senderPtr: Long, width: Int, height: Int, data: ByteArray) // Envia vídeo
    external fun sendAudio(senderPtr: Long, channels: Int, sampleRate: Int, data: ShortArray) // Envia áudio
    external fun sendDestroy(senderPtr: Long)                // Libera sender
    external fun destroy()                                   // Finaliza biblioteca NDI

    // --- Wrappers Kotlin ---
    private var senderPtr: Long = 0

    fun createSender(name: String) {
        senderPtr = sendCreate(name)
        if (senderPtr == 0L) {
            throw RuntimeException("Falha ao criar sender NDI")
        }
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int) {
        if (senderPtr == 0L) return
        sendVideo(senderPtr, width, height, data)
    }

    fun sendAudioFrame(channels: Int, sampleRate: Int, data: ShortArray) {
        if (senderPtr == 0L) return
        sendAudio(senderPtr, channels, sampleRate, data)
    }

    fun close() {
        if (senderPtr != 0L) {
            sendDestroy(senderPtr)
            senderPtr = 0
        }
    }
}
