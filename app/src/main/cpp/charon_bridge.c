#include <jni.h>
#include <android/log.h>
#include <stdio.h>

#define LOG_TAG "JNI-Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define CERT_PATH "/data/data/com.allinsafevpn/files/ca-cert.pem"


JNIEXPORT jboolean JNICALL
Java_com_allinsafevpn_NativeVpnBridge_startVpn(JNIEnv *env, jobject thiz,
                                                jstring server,
                                                jstring username,
                                                jstring password,
                                                jbyteArray certBytes,
                                                jstring certPath) {
    // 1. 인증서 파일 저장
    // certBytes = pem 인증서 파일 원본
    // certPath = 그걸 이 프로그램에서 저장해서 쓸 경로
    jsize len = (*env)->GetArrayLength(env, certBytes);
    jbyte *buf = (*env)->GetByteArrayElements(env, certBytes, NULL);

    const char *c_certPath = (*env)->GetStringUTFChars(env, certPath, 0);
    FILE *fp = fopen(c_certPath, "wb");
    if (fp == NULL) {
        LOGE("❌ Failed to open file for CA cert");
        return JNI_FALSE;
    }

    fwrite(buf, sizeof(jbyte), len, fp);
    fclose(fp);
    (*env)->ReleaseStringUTFChars(env, certPath, c_certPath);

    LOGI("✅ CA cert written is at %s", CERT_PATH);

    // 2. 문자열 파라미터 변환
    const char *c_server = (*env)->GetStringUTFChars(env, server, 0);
    const char *c_user = (*env)->GetStringUTFChars(env, username, 0);
    const char *c_pw = (*env)->GetStringUTFChars(env, password, 0);

    LOGI("🔐 server=%s, id=%s", c_server, c_user);

    // 3. strongSwan 연결 로직 호출 (여기에 구현 추가할 것)
    // TODO: cert 로드 → IKE_SA → EAP-MSCHAPv2 인증 → 연결

    // 4. 해제
    (*env)->ReleaseStringUTFChars(env, server, c_server);
    (*env)->ReleaseStringUTFChars(env, username, c_user);
    (*env)->ReleaseStringUTFChars(env, password, c_pw);

    return JNI_TRUE;
}
