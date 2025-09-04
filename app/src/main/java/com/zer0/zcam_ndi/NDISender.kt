package com.zer0.zcam_ndi

import android.graphics.Bitmap
import android.util.Log
import com.newtek.ndi.NDIlib
import java.nio.ByteBuffer

class NDISender(private val streamName: String) {

    private var sender: NDIlib.SND? = null

    init {
        if (!NDIlib.initialize()) {
            Log.e("NDISender", "NDI não pôde ser inicializado")
            return
        }

        // Cria o sender
        val sendDesc = NDIlib.send_create_t().apply {
            p_ndi_name = streamName
        }

        sender = NDIlib.send_create(sendDesc)
        if (sender == null) {
            Log.e("NDISender", "Falha ao criar NDI Sender")
        } else {
            Log.d("NDISender", "NDI Sender criado: $streamName")
        }
    }

    fun sendFrame(bitmap: Bitmap) {
        sender?.let { snd ->
            val width = bitmap.width
            val height = bitmap.height

            // Garantir que o bitmap esteja em ARGB_8888
            val bmp = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else bitmap

            val frame = NDIlib.video_frame_v2().apply {
                xres = width
                yres = height
                frame_rate_N = 30000
                frame_rate_D = 1000
                frame_format_type = NDIlib.FOURCC_BGRA
                p_data = convertBitmapToBGRA(bmp)
            }

            NDIlib.send_send_video_v2(snd, frame)
        }
    }

    fun release() {
        sender?.let { NDIlib.send_destroy(it) }
        NDIlib.destroy()
    }

    private fun convertBitmapToBGRA(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val buffer = ByteBuffer.allocate(pixels.size * 4)
        for (pixel in pixels) {
            val a = (pixel shr 24 and 0xFF).toByte()
            val r = (pixel shr 16 and 0xFF).toByte()
            val g = (pixel shr 8 and 0xFF).toByte()
            val b = (pixel and 0xFF).toByte()

            buffer.put(b)
            buffer.put(g)
            buffer.put(r)
            buffer.put(a)
        }
        return buffer.array()
    }
}
