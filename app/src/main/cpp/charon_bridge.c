#include <jni.h>
#include <android/log.h>

JNIEXPORT jboolean JNICALL
Java_com_example_allinsafevpn_CharonBridge_startVpn(JNIEnv* env, jobject thiz, jstring juser, jstring jpass) {
    const char* user = (*env)->GetStringUTFChars(env, juser, 0);
    const char* pass = (*env)->GetStringUTFChars(env, jpass, 0);

    __android_log_print(ANDROID_LOG_INFO, "CharonBridge", "Start VPN for user: %s", user);

    // TODO: 여기에서 libcharon을 초기화하고 IKE 세션 시작하는 로직 작성

    (*env)->ReleaseStringUTFChars(env, juser, user);
    (*env)->ReleaseStringUTFChars(env, jpass, pass);

    return JNI_TRUE;
}
