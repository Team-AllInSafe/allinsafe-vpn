#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <unistd.h>

#include <credentials/keys/shared_key.h>
#include <credentials/sets/mem_cred.h>
#include <credentials/credential_manager.h>
#include <library.h>
//#include <credentials/certificates/x509.h>
#include <threading/rwlock.h>
#include <utils/chunk.h>
#include <utils/debug.h>


#define LOG_TAG "JNI-Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)




JNIEXPORT jboolean JNICALL
Java_com_allinsafevpn_NativeVpnBridge_registerCredentials(JNIEnv *env, jobject thiz,
                                                          jstring j_server,
                                                          jstring j_username,
                                                          jstring j_password,
                                                          jstring j_certPath
                                                          ) {


    const char *username = (*env)->GetStringUTFChars(env, j_username, 0);
    const char *password = (*env)->GetStringUTFChars(env, j_password, 0);
    const char *certPath = (*env)->GetStringUTFChars(env, j_certPath, 0);

    library_init(NULL,"libstrongswan");

    // 3️⃣ 파일 경로를 BUILD_FROM_FILE로 넘긴다
    certificate_t *cert = lib->creds->create(lib->creds,
                                             CRED_CERTIFICATE, CERT_X509,
                                             BUILD_FROM_FILE, certPath,
                                             BUILD_END);

    if (!cert) {
    LOGE("❌ 인증서 로딩 실패: %s", certPath);
        (*env)->ReleaseStringUTFChars(env, j_username, username);
        (*env)->ReleaseStringUTFChars(env, j_password, password);
        (*env)->ReleaseStringUTFChars(env, j_certPath, certPath);
    return JNI_FALSE;
    }

    // mem_cred 생성 및 등록
    mem_cred_t *creds = mem_cred_create();

    // username/password 기반 shared key 등록 (EAP-MSCHAPv2)
    identification_t *id = identification_create_from_string(username);
    shared_key_t *key = shared_key_create(SHARED_EAP, chunk_from_str(password));
    creds->add_shared(creds, key, id, NULL);

    // 인증서 등록
    creds->add_cert(creds, TRUE, cert);

    // 시스템 credential manager 에 등록
    lib->credmgr->add_set(lib->credmgr ,&creds->set);
//    lib->credmgr->add_set(lib->credmgr,&creds->set); CRED_CERTIFICATE 넣는거 내가 뺌.
//      파라미터가 두개짜리 함수인데 3개 넣는걸로 줘서 내가 수정


    (*env)->ReleaseStringUTFChars(env, j_username, username);
    (*env)->ReleaseStringUTFChars(env, j_password, password);
    (*env)->ReleaseStringUTFChars(env, j_certPath, certPath);

    return JNI_TRUE;
}


//전버전2 (안돌아감)
/*
Java_com_allinsafevpn_NativeVpnBridge_registerCredentials(JNIEnv *env, jobject thiz,
                                                          jstring j_username,
                                                          jstring j_password,
                                                          jstring j_certPath) {
    const char *username = (*env)->GetStringUTFChars(env, j_username, 0);
    const char *password = (*env)->GetStringUTFChars(env, j_password, 0);
    const char *certPath = (*env)->GetStringUTFChars(env, j_certPath, 0);

    // 1. cert 파일 경로로부터 x509 certificate 생성
    certificate_t *cert = lib->creds->create(lib->creds,
                                             CRED_CERTIFICATE, CERT_X509,
                                             BUILD_FROM_FILE, certPath,
                                             BUILD_END);

    mem_cred_t* creds=mem_cred_create();
    creds->add_cert(creds,TRUE,cert);
    lib->credmgr->get_cert
    if (!cert) {
        LOGE("❌ 인증서 로딩 실패: %s", certPath);
        return JNI_FALSE;
    }

    // 2. mem_cred_t 생성
    mem_cred_t *creds = mem_cred_create();

    // 3. username (ID) + password (EAP key) 등록
    identification_t *id = identification_create_from_string(username);
    shared_key_t *key = shared_key_create(SHARED_EAP, chunk_from_str(password));



    creds->add(creds, CRED_SHARED, id, key);
    creds->add(creds, CRED_CERTIFICATE, NULL, &cert->get_ref(cert)->interface);

    // 4. 시스템 Credential Manager에 등록
    lib->credmgr->add_set(lib->credmgr, CRED_SHARED, &creds->set);
    lib->credmgr->add_set(lib->credmgr, CRED_CERTIFICATE, &creds->set);

    LOGI("✅ 인증 정보 등록 완료: ID=%s", username);

    // 해제
    (*env)->ReleaseStringUTFChars(env, j_username, username);
    (*env)->ReleaseStringUTFChars(env, j_password, password);
    (*env)->ReleaseStringUTFChars(env, j_certPath, certPath);


    return JNI_TRUE;
}
 */

//startVpn 전버전 (안돌아감)
/*
 #include <credentials/auth_cfg.h> //add_cert
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
    */








