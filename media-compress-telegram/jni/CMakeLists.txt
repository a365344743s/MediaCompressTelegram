cmake_minimum_required(VERSION 3.6.0)

set(CMAKE_CXX_FLAGS "-std=c++14 -DANDROID -g")
set(CMAKE_C_FLAGS "-w -std=c11 -DANDROID -D_LARGEFILE_SOURCE=1 -g")
set(CMAKE_ASM_FLAGS "${CFLAGS} -x assembler-with-cpp")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections -fdata-sections")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--exclude-libs,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libswscale.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavformat.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavcodec.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavresample.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavutil.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libswresample.a,
${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libvpx.a")

if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
    enable_language(ASM)
else()
    enable_language(ASM_NASM)
endif()

add_library(avutil STATIC IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavutil.a)

add_library(avformat STATIC IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavformat.a)

add_library(avcodec STATIC IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavcodec.a)

add_library(avresample STATIC IMPORTED)
set_target_properties(avresample PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libavresample.a)

add_library(swresample STATIC IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libswresample.a)

add_library(swscale STATIC IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libswscale.a)

add_library(libvpx STATIC IMPORTED)
set_target_properties(libvpx PROPERTIES IMPORTED_LOCATION ${CMAKE_HOME_DIRECTORY}/ffmpeg/${ANDROID_ABI}/libvpx.a)

set(NATIVE_LIB "tmessages.42")

#tmessages
add_library(${NATIVE_LIB} SHARED
        jni.c
        gifvideo.cpp)

target_compile_options(${NATIVE_LIB} PUBLIC
        -ffast-math -Os -funroll-loops -ffast-math -fno-strict-aliasing -fno-math-errno)

target_compile_definitions(${NATIVE_LIB} PUBLIC
        BSD=1 NULL=0 SOCKLEN_T=socklen_t ANDROID_NDK DISABLE_IMPORTGL AVOID_TABLES ANDROID_TILE_BASED_DECODE __STDC_CONSTANT_MACROS ANDROID_ARMV6_IDCT OPUS_BUILD FIXED_POINT USE_ALLOCA restrict= __EMX__ LOCALE_NOT_USED HAVE_LRINT HAVE_LRINTF)

set_target_properties(${NATIVE_LIB} PROPERTIES
        ANDROID_ARM_NEON FALSE
        ANDROID_ARM_MODE arm)

if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(${NATIVE_LIB} PUBLIC
            LIBYUV_NEON OPUS_HAVE_RTCD OPUS_ARM_ASM)

elseif(${ANDROID_ABI} STREQUAL "x86")
    target_compile_definitions(${NATIVE_LIB} PUBLIC
            x86fix)
endif()

target_include_directories(${NATIVE_LIB} PUBLIC
        ffmpeg/include)

target_link_libraries(${NATIVE_LIB}
        -Wl,--whole-archive -Wl,--no-whole-archive
        swscale
        avformat
        avcodec
        avresample
        swresample
        libvpx
        avutil
        log
        cpufeatures)

#if (${ANDROID_ABI} STREQUAL "x86" OR ${ANDROID_ABI} STREQUAL "x86_64")
#    target_link_libraries(${NATIVE_LIB}
#            -Wl,--whole-archive vpxasm -Wl,--no-whole-archive
#            c)
#endif()

include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()