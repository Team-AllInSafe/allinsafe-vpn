#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <credentials/certificates/certificate.h> // lib->credmgr
#include <credentials/certificates/x509.h>
#include <credentials/credential_manager.h>  // add_cert()
#include <credentials/certificates/x509.h>
#include <bio/bio_reader.h>
#include <bio/bio_writer.h>
#include <library.h>



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

    // 3. 인증서 로드 → x509 파싱 → credentials 등록
    bio_reader_t *reader = (bio_reader_t *) bio_reader_create_file(c_certPath);
    if (!reader) {
        LOGE("❌ Failed to read CA cert file");
        return JNI_FALSE;
    }

    chunk_t cert_chunk = reader->read_all(reader);
    reader->destroy(reader);

    if (!cert_chunk.ptr || !cert_chunk.len) {
        LOGE("❌ CA cert is empty or unreadable");
        return JNI_FALSE;
    }

    x509_t *x509 = x509_create_from_der(cert_chunk);
    chunk_free(&cert_chunk);

    if (!x509) {
        LOGE("❌ x509 파싱 실패");
        return JNI_FALSE;
    }

    lib->credmgr->add_static_set(lib->credmgr, CRED_CERTIFICATE, &x509->interface);
    LOGI("✅ 인증서 로딩 및 등록 완료");

    // TODO: 이후 strongSwan 연결 (IKE_SA, EAP-MSCHAPv2 설정 및 연결 요청)

    // 4. 해제
    (*env)->ReleaseStringUTFChars(env, server, c_server);
    (*env)->ReleaseStringUTFChars(env, username, c_user);
    (*env)->ReleaseStringUTFChars(env, password, c_pw);

    return JNI_TRUE;
}
}
