LOCAL_PATH := $(call my-dir)

# charon_bridge 모듈 정의
include $(CLEAR_VARS)

LOCAL_MODULE    := charon-bridge#myvpncore
LOCAL_SRC_FILES := $(LOCAL_PATH)/../app/src/main/cpp/charon_bridge.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../external/strongswan/src/include \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan \
    $(LOCAL_PATH)/../external/strongswan/src/libstrongswan/asn1 \
    $(LOCAL_PATH)/../external/strongswan/src/libcharon \
    $(LOCAL_PATH)/../external/strongswan/src/charon

LOCAL_LDLIBS := -llog -latomic -lm
# LOCAL_SHARED_LIBRARIES := libatomic

include $(BUILD_SHARED_LIBRARY)

