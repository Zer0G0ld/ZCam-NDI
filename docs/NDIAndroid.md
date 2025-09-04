# **Guia Base NDI para Android**

## **1. Introdução**

NDI® (Network Device Interface) é uma tecnologia da **Vizrt** para transmissão de vídeo e áudio em tempo real sobre redes IP. Permite enviar, receber e enumerar fontes com **baixa latência**.

* Funciona em **Windows, MacOS, Linux e Android** (API 24+ no Android)
* Estrutura principal do SDK: `send`, `recv`, `find`
* Utilitários para conversão de formatos de áudio e vídeo
* Pode usar multithreading e aceleração de hardware

---

## **2. Estrutura do projeto Android**

```bash
ZCam-NDI/
 ├─ app/
 │  ├─ src/
 │  │  ├─ main/
 │  │  │  ├─ java/com/zer0/zcam_ndi/    # Classes Kotlin
 │  │  │  └─ jniLibs/                   # Bibliotecas nativas
 │  │  │     ├─ arm64-v8a/
 │  │  │     │   ├─ libndi.so
 │  │  │     │   ├─ libndi_licenses.txt
 │  │  │     ├─ armeabi-v7a/
 │  │  │     │   └─ ...
 │  └─ libs/
 │      └─ NDIforAndroid.jar             # Biblioteca Java do SDK
```

* `.so` → bibliotecas nativas por arquitetura
* `.jar` → classes Java/Kotlin que interagem com o SDK
* Licenças devem estar incluídas (`libndi_licenses.txt`)

---

## **3. Enviando vídeo (NDI-SEND)**

```kotlin
val sender = NDIlib_send_create(null) ?: throw Exception("Erro ao criar NDI sender")

val frame = NDIlib_video_frame_v2().apply {
    xres = width
    yres = height
    frame_format_type = NDIlib.frame_format_type_e.BGRA
    p_data = pixels
}

sender.sendVideoFrame(frame)

// Para encerrar:
NDIlib_send_destroy(sender)
```

**Notas importantes:**

* Meta-dados podem ser enviados via XML UTF-8
* Timecode padrão: `NDIlib_send_timecode_synthesize` (UTC, 100 ns)
* Cores RGB podem impactar performance; usar YCbCr se possível
* Enviar frames de forma assíncrona aumenta performance

---

## **4. Recebendo vídeo e áudio (NDI-RECV)**

```kotlin
val recv = NDIlib_recv_create_v3(null) ?: throw Exception("Erro ao criar NDI receiver")

val source = NDIlib_source_t().apply { p_ndi_name = "My Computer (MIX 1)" }
NDIlib_recv_connect(recv, source)

while(receiving) {
    val video = NDIlib_video_frame_v2()
    val audio = NDIlib_audio_frame_v2()
    when(NDIlib_recv_capture_v2(recv, video, audio, null, 1500)) {
        NDIlib_frame_type_video -> {
            // processar video
            NDIlib_recv_free_video_v2(recv, video)
        }
        NDIlib_frame_type_audio -> {
            // processar audio
            NDIlib_recv_free_audio_v2(recv, audio)
        }
    }
}

// Para encerrar:
NDIlib_recv_destroy(recv)
```

* Audio recomendado: **floating-point (FLTP)**
* Buffers de áudio \~ metade da duração do frame de vídeo
* Multithread seguro: áudio e vídeo podem ser processados separadamente

---

## **5. Descobrindo fontes (NDI-FIND)**

```kotlin
val finder = NDIlib_find_create_v2(null) ?: throw Exception("Erro ao criar NDI finder")
val sources = NDIlib_find_get_current_sources(finder)

val changed = NDIlib_find_wait_for_sources(finder, 2500) // espera 2,5s

NDIlib_find_destroy(finder)
```

* Permite listar todas as fontes NDI disponíveis na rede
* Pode ser usado para reconexões automáticas

---

## **6. Metadata e Timecode**

* Metadata: XML UTF-8 NULL-terminated
* Timecode: `NDIlib_send_timecode_synthesize`
* Pode ser usado para sincronização entre streams e máquinas (usar NTP)

---

## **7. Dicas de performance**

* Usar YCbCr quando possível
* Aproveitar GPU para conversão de cores
* Envio assíncrono de frames melhora performance
* Multi-core: NDI usa múltiplos núcleos para codificação/decodificação
* Reliable UDP no NDI v5: eficiente mesmo em redes com perda de pacotes
* Multicast: habilitar IGMP snooping nos roteadores

---

## **8. DirectShow (Windows)**

* Filtro “NDI Source” para WPF/DirectShow
* Áudio: floating-point / 16-bit
* Vídeo: UYVY / BGRA
* URL de fonte: `ndi://computer/source?audio=false&video=false&low_quality=true`

---

## **9. Licenciamento**

* Bibliotecas de terceiros incluídas, verificar `Processing.NDI.Lib.Licenses.txt`
* Inclusão obrigatória no redistribuível

