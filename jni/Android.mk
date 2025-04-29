LOCAL_PATH := $(call my-dir)

strongswan_DIR=../external/strongswan

include $(LOCAL_PATH)/../external/strongswan/Android.mk

# libcrypto.a (openssl) 파일 불러오기
include $(CLEAR_VARS)
LOCAL_CFLAGS += -include $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/config.h
LOCAL_MODULE := libcrypto_static
LOCAL_SRC_FILES := C:/Users/User/AndroidStudioProjects/AllinSafe_Vpn/app/src/main/openssl/$(TARGET_ARCH_ABI)/libcrypto.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/openssl/include
include $(PREBUILT_STATIC_LIBRARY)



# charon-bridge 파일 불러오기
include $(CLEAR_VARS)
LOCAL_CFLAGS += -include $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/config.h
LOCAL_MODULE    := charon-bridge
LOCAL_SRC_FILES := ../app/src/main/cpp/charon-bridge.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../external/strongswan/src/include \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/sets \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/keys \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/asn1 \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/utils \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/crypto/hashers \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/collections \
    $(LOCAL_PATH)/../external/strongswan/src/libcharon \
    $(LOCAL_PATH)/../external/strongswan/src/charon \
    $(LOCAL_PATH)/openssl/include

LOCAL_STATIC_LIBRARIES := libcrypto_static libstrongswan libcharon
LOCAL_LDLIBS := -llog -latomic -lm

include $(BUILD_SHARED_LIBRARY)

# charon-bridge에 필요한 c 파일들 컴파일하기(charon-bridge 불러오기 전에 넣을것)
# include $(CLEAR_VARS)
# LOCAL_CFLAGS += -include $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/config.h
# LOCAL_MODULE := strongswan_partial
# LOCAL_SRC_FILES := \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/sets/mem_cred.c \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/utils/identification.c \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/keys/shared_key.c \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/utils/chunk.c \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/library.c
#
# LOCAL_C_INCLUDES := \
#     $(LOCAL_PATH)/../external/strongswan/src/include \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/sets \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/credentials/keys \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/utils \
#     $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/threading
#
# LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)
# include $(BUILD_SHARED_LIBRARY)