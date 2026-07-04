cmake_minimum_required(VERSION 3.22.1)
project("hy-mt-stq")

set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# Build llama.cpp as static lib via its own CMake
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)
set(GGML_USE_STQ ON CACHE BOOL "Enable STQ quantization" FORCE)

add_subdirectory(${LLAMA_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama_build)

add_library(hy-mt-stq SHARED hy-mt-jni.cpp)
target_include_directories(hy-mt-stq PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
    ${LLAMA_DIR}/common
)
target_compile_definitions(hy-mt-stq PRIVATE GGML_USE_STQ=1)
find_library(log-lib log)
target_link_libraries(hy-mt-stq llama ${log-lib})
