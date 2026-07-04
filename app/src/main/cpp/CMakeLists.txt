cmake_minimum_required(VERSION 3.22.1)
project("hy-mt-stq")

set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

file(GLOB LLAMA_SRCS
    ${LLAMA_DIR}/ggml.c
    ${LLAMA_DIR}/ggml-alloc.c
    ${LLAMA_DIR}/ggml-backend.c
    ${LLAMA_DIR}/ggml-quants.c
    ${LLAMA_DIR}/ggml-quants-stq.c
    ${LLAMA_DIR}/llama.cpp
    ${LLAMA_DIR}/unicode.cpp
    ${LLAMA_DIR}/unicode-data.cpp
)

add_library(hy-mt-stq SHARED
    hy-mt-jni.cpp
    ${LLAMA_SRCS}
)

target_include_directories(hy-mt-stq PRIVATE ${LLAMA_DIR})
target_compile_definitions(hy-mt-stq PRIVATE GGML_USE_STQ=1)
find_library(log-lib log)
target_link_libraries(hy-mt-stq ${log-lib})
