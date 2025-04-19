cmake_minimum_required(VERSION 3.10)
project("myvpn")

# strongSwan 외부 경로 설정
set(STRONGSWAN_PATH "${CMAKE_SOURCE_DIR}/../../../../external/strongswan")

# include 경로
include_directories(
        ${STRONGSWAN_PATH}/src/include
        ${STRONGSWAN_PATH}/src/libstrongswan
        ${STRONGSWAN_PATH}/src/charon
)

# strongSwan 관련 .c 파일들 수동 추가 or glob
file(GLOB STRONGSWAN_SOURCES
        ${STRONGSWAN_PATH}/src/libstrongswan/*.c
        ${STRONGSWAN_PATH}/src/charon/*.c
)

# JNI 브릿지 C 파일
set(JNI_SOURCES
        ${CMAKE_SOURCE_DIR}/charon_bridge.c
)

# 라이브러리 생성
add_library(
        charon-bridge
        SHARED
        ${JNI_SOURCES}
        ${STRONGSWAN_SOURCES}
)

# 안드로이드 로그 라이브러리 링크
target_link_libraries(
        charon-bridge
        log
)
