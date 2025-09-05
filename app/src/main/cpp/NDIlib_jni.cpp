#include <jni.h>
#include "Processing.NDI.Lib.h"
#include <string>
#include <vector>

static std::vector<NDIlib_send_instance_t> senders;

std::string jstringToString(JNIEnv* env, jstring jStr) {
    const char* chars = env->GetStringUTFChars(jStr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return str;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_initialize(JNIEnv* env, jobject thiz) {
    return NDIlib_initialize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_sendCreate(JNIEnv* env, jobject thiz, jstring jName)
{
    std::string name = jstringToString(env, jName);

    if (!NDIlib_initialize()) return 0;

    NDIlib_send_create_t createDesc;
    createDesc.p_ndi_name = name.c_str();

    NDIlib_send_instance_t sender = NDIlib_send_create(&createDesc);
    if (!sender) return 0;

    senders.push_back(sender);
    return reinterpret_cast<jlong>(sender);
}

JNIEXPORT void JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_sendVideo(JNIEnv* env, jobject, jlong senderPtr,
                                         jint width, jint height, jbyteArray data)
{
    if (!senderPtr) return;
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);

    NDIlib_video_frame_v2_t frame;
    frame.xres = width;
    frame.yres = height;
    frame.FourCC = NDIlib_FourCC_type_BGRX;
    frame.p_data = reinterpret_cast<uint8_t*>(bytes);
    frame.line_stride_in_bytes = width * 4;

    NDIlib_send_send_video_v2(reinterpret_cast<NDIlib_send_instance_t>(senderPtr), &frame);

    env->ReleaseByteArrayElements(data, bytes, 0);
}

JNIEXPORT void JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_sendAudio(JNIEnv* env, jobject, jlong senderPtr,
                                         jint channels, jint sampleRate, jshortArray data)
{
    if (!senderPtr) return;
    jshort* samples = env->GetShortArrayElements(data, nullptr);
    jsize len = env->GetArrayLength(data);

    std::vector<float> floatBuffer(len);
    for (int i = 0; i < len; ++i) {
        floatBuffer[i] = samples[i] / 32768.0f; // normaliza para [-1,1]
    }

    NDIlib_audio_frame_v2_t frame;
    frame.no_channels = channels;
    frame.no_samples = len / channels;
    frame.p_data = floatBuffer.data();
    frame.sample_rate = sampleRate;

    NDIlib_send_send_audio_v2(reinterpret_cast<NDIlib_send_instance_t>(senderPtr), &frame);

    env->ReleaseShortArrayElements(data, samples, 0);
}

JNIEXPORT void JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_sendDestroy(JNIEnv* env, jobject, jlong senderPtr)
{
    if (!senderPtr) return;
    NDIlib_send_destroy(reinterpret_cast<NDIlib_send_instance_t>(senderPtr));
}

JNIEXPORT void JNICALL
Java_com_zer0_zcam_1ndi_NDIlib_destroy(JNIEnv* env, jobject thiz) {
    NDIlib_destroy();
}

} // extern "C"
