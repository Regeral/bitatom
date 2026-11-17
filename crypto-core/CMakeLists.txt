cmake_minimum_required(VERSION 3.12)
project(crypto_core C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(SRC_FILES
    encrypt_aes.c
    auth_gen.c
    password_hash.c
    monocypher.c
    monocypher-ed25519.c
    crypto_api.c
)

# Статическая библиотека (для cinterop / Kotlin Native)
add_library(crypto_core STATIC ${SRC_FILES})
target_include_directories(crypto_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

# Динамическая библиотека для desktop (JVM + JNA): crypto_core.dll / libcrypto_core.so / .dylib
add_library(crypto_core_shared SHARED ${SRC_FILES})
set_target_properties(crypto_core_shared PROPERTIES OUTPUT_NAME crypto_core PREFIX "")
if(NOT WIN32)
    set_target_properties(crypto_core_shared PROPERTIES PREFIX "lib")
endif()
target_include_directories(crypto_core_shared PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

if(WIN32)
    target_link_libraries(crypto_core PUBLIC advapi32)
    target_link_libraries(crypto_core_shared PRIVATE advapi32)
endif()

add_executable(crypto_test main.c)
target_link_libraries(crypto_test PRIVATE crypto_core)
