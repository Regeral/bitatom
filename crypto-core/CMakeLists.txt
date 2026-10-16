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
)

add_library(crypto_core STATIC ${SRC_FILES})

target_include_directories(crypto_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_executable(crypto_test main.c)

target_link_libraries(crypto_test PRIVATE crypto_core)
