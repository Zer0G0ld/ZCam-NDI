package com.ndi.android;

/**
 * Classe de interface Java para a biblioteca nativa NDI.
 */
public class NDIlib {

    static {
        try {
            System.loadLibrary("ndi"); // Carrega libndi.so
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Não foi possível carregar a biblioteca NDI: " + e.getMessage());
        }
    }

    /**
     * Inicializa a biblioteca NDI.
     *
     * @return true se inicializou corretamente, false caso contrário
     */
    public static native boolean initialize();

    /**
     * Finaliza a biblioteca NDI.
     */
    public static native void destroy();

    /**
     * Cria um sender NDI.
     *
     * @param name Nome do sender
     * @return ponteiro nativo do sender como long
     */
    public static native long sendCreate(String name);

    /**
     * Destroi um sender NDI.
     *
     * @param sender ponteiro nativo do sender
     */
    public static native void sendDestroy(long sender);

    /**
     * Envia um frame de vídeo pelo sender NDI.
     *
     * @param sender ponteiro nativo do sender
     * @param frame  dados do frame em bytes
     * @param width  largura do frame
     * @param height altura do frame
     */
    public static native void sendVideo(long sender, byte[] frame, int width, int height);
}
