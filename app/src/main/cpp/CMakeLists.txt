cmake_minimum_required(VERSION 3.10.2)
project(ndi_jni)

# Força C++17 (ou C++11 se preferir)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Caminho para headers do NDI SDK
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
#include_directories(${CMAKE_SOURCE_DIR}/src/main/jniLibs/include)

# Biblioteca JNI
add_library(
        ndi                # nome da lib final
        SHARED             # gera .so
        NDIlib_jni.cpp     # fonte JNI
)

# Força o compilador a tratar como C++
set_source_files_properties(NDIlib_jni.cpp PROPERTIES LANGUAGE CXX)
set_target_properties(ndi PROPERTIES LINKER_LANGUAGE CXX)

# Biblioteca NDI pré-compilada
add_library(ndilib STATIC IMPORTED)
set_target_properties(ndilib PROPERTIES
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libndi.so
)

# Linka bibliotecas
target_link_libraries(
        ndi
        ndilib             # NDI SDK
        log                # Android logging
        android            # Android system libs
        jnigraphics        # se usar bitmaps ou gráficos JNI
)
