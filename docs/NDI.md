## **1. Introdução ao NDI**

* NDI (Network Device Interface) é uma tecnologia da **Vizrt** para transmissão de vídeo e áudio sobre rede IP.
* Permite **enviar, receber e enumerar fontes** de vídeo/áudio em tempo real com baixa latência.
* Funciona em Windows, MacOS, Linux e Android (API 24+ no Android).
* Estrutura do SDK: **send**, **recv**, **find**, além de utilitários para conversão de formatos.

---

## **2. Estrutura do SDK e pastas (Android)**

Exemplo típico de projeto Android:

```bash
ZCam-NDI/
 ├─ app/
 │  ├─ src/
 │  │  ├─ main/
 │  │  │  ├─ java/com/zer0/zcam_ndi/   # Classes Kotlin
 │  │  │  └─ jniLibs/                  # Bibliotecas nativas
 │  │  │     ├─ arm64-v8a/
 │  │  │     │   ├─ libndi.so
 │  │  │     │   ├─ libndi_licenses.txt
 │  │  │     ├─ armeabi-v7a/
 │  │  │     │   └─ ... mesmo formato
 │  │  │     └─ ... outras arquiteturas
 │  └─ libs/
 │      └─ NDIforAndroid.jar            # Biblioteca Java do SDK (NDI classes)
```

* `.so` → código nativo do NDI (necessário para cada arquitetura)
* `.jar` → classes Java/Kotlin para interface com o SDK
* Licenças (`libndi_licenses.txt`) devem ser incluídas para conformidade

---

## **3. Envio de vídeo (NDI-SEND)**

* Criar instância do **sender**:

```kotlin
val sender = NDIlib_send_create(null)
```

* Criar frame de vídeo:

```kotlin
val frame = NDIlib_video_frame_v2().apply {
    xres = width
    yres = height
    frame_format_type = NDIlib.frame_format_type_e.BGRA
    p_data = pixels
}
sender.sendVideoFrame(frame)
```

* Recursos adicionais:

  * Meta-dados por frame (XML UTF-8)
  * Controle de timecode (`NDIlib_send_timecode_synthesize`)
  * Conversão de cores: RGB→YCbCr (interno) ou GPU

---

## **4. Recepção de vídeo e áudio (NDI-RECV)**

* Criar instância do **receiver**:

```kotlin
val recv = NDIlib_recv_create_v3(null)
```

* Conectar a uma fonte:

```kotlin
val source = NDIlib_source_t().apply { p_ndi_name = "My Computer (MIX 1)" }
NDIlib_recv_connect(recv, source)
```

* Loop de captura:

```kotlin
while(receiving) {
    val video = NDIlib_video_frame_v2()
    val audio = NDIlib_audio_frame_v2()
    when(NDIlib_recv_capture_v2(recv, video, audio, null, 1500)) {
        NDIlib_frame_type_video -> { /* processar video */ }
        NDIlib_frame_type_audio -> { /* processar audio */ }
    }
}
```

* Receber áudio: ponto flutuante (FLTP) recomendado; buffers \~ metade da duração do frame de vídeo

---

## **5. Descoberta de fontes (NDI-FIND)**

* Criar instância:

```kotlin
val finder = NDIlib_find_create_v2(null)
```

* Consultar fontes atuais:

```kotlin
val sources = NDIlib_find_get_current_sources(finder)
```

* Esperar por mudanças:

```kotlin
val changed = NDIlib_find_wait_for_sources(finder, 2500)
```

---

## **6. Metadata e timecode**

* Metadata é XML UTF-8 NULL-terminated.
* Timecode padrão: `NDIlib_send_timecode_synthesize` (UTC, 100 ns).
* Pode ser usado para sincronização entre streams e máquinas (usar NTP para multi-device).

---

## **7. Dicas de performance**

* Preferir YCbCr para vídeo (melhor performance/qualidade).
* Usar cores no GPU quando possível.
* Multithreading: separar captura de áudio e vídeo.
* Assíncrono: enviar frames de forma não-bloqueante aumenta performance.
* NDI v5 usa **Reliable UDP**, eficiente mesmo em altas perdas de pacotes.

---

## **8. Multicast (opcional)**

* Suportado, mas deve haver **IGMP snooping** habilitado no roteador.
* Má configuração de multicast pode sobrecarregar a rede.

---

## **9. DirectShow (Windows)**

* Filtro “NDI Source” para integrar em WPF/DirectShow.
* Áudio: floating-point / 16-bit, Vídeo: UYVY/BGRA
* URL de fonte: `ndi://computer/source`
* Opções: `audio=false`, `video=false`, `low_quality=true`, `rgb=true`, etc.

---

## **10. Licenciamento**

* Bibliotecas usam algumas bibliotecas de terceiros.
* Incluir `Processing.NDI.Lib.Licenses.txt` no redistribuível.

