#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <credentials/certificates/certificate.h> // lib->credmgr
#include <credentials/credential_manager.h>  // add_cert()
#include <credentials/credential_set.h>
#include <credentials/cred_encoding.h>
#include <credentials/credential_factory.h>
#include <>
#include <config/peer_cfg.h>
#include <config/ike_cfg.h>
#include <daemon.h>


#define LOG_TAG "JNI-Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


typedef struct cert_t cert_t;

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

    // 2. 문자열 파라미터 변환
    const char *c_server = (*env)->GetStringUTFChars(env, server, 0);
    const char *c_user = (*env)->GetStringUTFChars(env, username, 0);
    const char *c_pw = (*env)->GetStringUTFChars(env, password, 0);

    LOGI("🔐 server=%s, id=%s", c_server, c_user);

    // 3. strongSwan 연결 로직 호출 (여기에 구현 추가할 것)
    // TODO: cert 로드 → IKE_SA → EAP-MSCHAPv2 인증 → 연결
    // 3-1. 인증서 로드
    cert_t *cert = lib->credmgr->add_cert(lib->credmgr, CERT_X509, ca_cert_path);
    if (!cert) {
        LOGE("❌ 인증서 로딩 실패: %s", c_certPath);
        return JNI_FALSE;
    }
    LOGI("✅ 인증서 로딩 성공");


    // 4. 해제
    (*env)->ReleaseStringUTFChars(env, server, c_server);
    (*env)->ReleaseStringUTFChars(env, username, c_user);
    (*env)->ReleaseStringUTFChars(env, password, c_pw);

    return JNI_TRUE;
}
