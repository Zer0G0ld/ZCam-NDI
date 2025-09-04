package com.ndi.android;

import com.ndi.android.NDIlib;

/**
 * Classe que representa um sender NDI.
 * Gerencia o ciclo de vida do sender e envia frames de vídeo.
 */
public class NDISender implements AutoCloseable {

    private long senderPtr = 0;
    private final String name;

    /**
     * Cria um novo sender NDI.
     *
     * @param name Nome do sender
     * @throws RuntimeException se a biblioteca NDI não puder ser inicializada ou o sender não for criado
     */
    public NDISender(String name) {
        this.name = name;

        // Não inicializar aqui, assume que MainActivity já fez
        senderPtr = NDIlib.sendCreate(name);
        if (senderPtr == 0) {
            throw new RuntimeException("Falha ao criar o sender NDI para: " + name);
        }
    }

    /**
     * Envia um frame de vídeo pelo sender.
     *
     * @param frame  dados do frame em bytes
     * @param width  largura do frame
     * @param height altura do frame
     * @throws IllegalStateException se o sender não estiver inicializado
     */
    public void sendFrame(byte[] frame, int width, int height) {
        if (senderPtr == 0) {
            throw new IllegalStateException("Sender NDI não inicializado.");
        }
        if (frame == null || frame.length == 0) return;

        NDIlib.sendVideo(senderPtr, frame, width, height);
    }

    /**
     * Libera recursos do sender.
     * Pode ser chamado manualmente ou automaticamente em try-with-resources.
     */
    @Override
    public void close() {
        if (senderPtr != 0) {
            NDIlib.sendDestroy(senderPtr);
            senderPtr = 0;
        }
    }

    /**
     * Retorna o nome do sender.
     */
    public String getName() {
        return name;
    }

    /**
     * Verifica se o sender ainda está ativo.
     */
    public boolean isActive() {
        return senderPtr != 0;
    }
}
