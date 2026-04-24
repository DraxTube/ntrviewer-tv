cmake_minimum_required(VERSION 3.22.1)
project(ntrviewer C CXX)

# Root of the NTRViewer-HR source tree
# From android/app/src/main/jni/ go up 5 levels to the project root
get_filename_component(SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../.." ABSOLUTE)

# SDL3 source directory (set by the workflow or user)
if(NOT DEFINED SDL3_SRC_DIR)
    set(SDL3_SRC_DIR "${SRC_DIR}/../SDL")
endif()

# Build SDL3 as a shared library
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
add_subdirectory(${SDL3_SRC_DIR} SDL3-build)

# ============================================================
# libjpeg-turbo (embedded, multi-precision)
# ============================================================

set(JT_COMMON_INCLUDES
    ${SRC_DIR}/jpeg_turbo
    ${SRC_DIR}/jpeg_turbo/src
    ${SRC_DIR}/jpeg_turbo/include
)
set(JT_COMMON_FLAGS -Wno-stringop-overflow -Wno-unused-parameter -Wno-sign-compare)

# --- jpeg16 sources at 16-bit precision ---
file(GLOB JT16_SRCS "${SRC_DIR}/jpeg_turbo/jpeg16/*.c")

add_library(jt16 OBJECT ${JT16_SRCS})
target_include_directories(jt16 PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt16 PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt16 PRIVATE BITS_IN_JSAMPLE=16 GIF_SUPPORTED PPM_SUPPORTED)

# --- jpeg12 own sources at 12-bit precision ---
file(GLOB JT12_OWN_SRCS "${SRC_DIR}/jpeg_turbo/jpeg12/*.c")

add_library(jt12_own OBJECT ${JT12_OWN_SRCS})
target_include_directories(jt12_own PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt12_own PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt12_own PRIVATE BITS_IN_JSAMPLE=12 GIF_SUPPORTED PPM_SUPPORTED)

# jpeg16 sources recompiled at 12-bit precision
add_library(jt12_from16 OBJECT ${JT16_SRCS})
target_include_directories(jt12_from16 PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt12_from16 PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt12_from16 PRIVATE BITS_IN_JSAMPLE=12 GIF_SUPPORTED PPM_SUPPORTED)

# --- jpeg8 own sources at 8-bit precision (default) ---
file(GLOB JT8_OWN_SRCS "${SRC_DIR}/jpeg_turbo/jpeg8/*.c")

add_library(jt8_own OBJECT ${JT8_OWN_SRCS})
target_include_directories(jt8_own PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt8_own PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt8_own PRIVATE BMP_SUPPORTED PPM_SUPPORTED)

# jpeg12 sources recompiled at 8-bit precision
add_library(jt8_from12 OBJECT ${JT12_OWN_SRCS})
target_include_directories(jt8_from12 PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt8_from12 PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt8_from12 PRIVATE BMP_SUPPORTED PPM_SUPPORTED)

# jpeg16 sources recompiled at 8-bit precision
add_library(jt8_from16 OBJECT ${JT16_SRCS})
target_include_directories(jt8_from16 PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt8_from16 PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt8_from16 PRIVATE BMP_SUPPORTED PPM_SUPPORTED)

# --- jpeg_turbo base sources ---
file(GLOB JT_BASE_SRCS "${SRC_DIR}/jpeg_turbo/*.c")
# Remove non-source files if any pattern matched headers
list(FILTER JT_BASE_SRCS INCLUDE REGEX ".*\\.c$")

add_library(jt_base OBJECT ${JT_BASE_SRCS})
target_include_directories(jt_base PRIVATE ${JT_COMMON_INCLUDES})
target_compile_options(jt_base PRIVATE ${JT_COMMON_FLAGS} -Wno-clobbered)
target_compile_definitions(jt_base PRIVATE BMP_SUPPORTED GIF_SUPPORTED PPM_SUPPORTED)

# --- ARM NEON SIMD sources ---
file(GLOB JT_ARM_SRCS
    "${SRC_DIR}/jpeg_turbo/simd/arm/*.c"
    "${SRC_DIR}/jpeg_turbo/simd/arm/aarch64/*.c"
)

add_library(jt_arm OBJECT ${JT_ARM_SRCS})
target_include_directories(jt_arm PRIVATE
    ${JT_COMMON_INCLUDES}
    ${SRC_DIR}/jpeg_turbo/simd
    ${SRC_DIR}/jpeg_turbo/simd/aarch64
)
target_compile_options(jt_arm PRIVATE ${JT_COMMON_FLAGS})
target_compile_definitions(jt_arm PRIVATE NEON_INTRINSICS)

# ============================================================
# Nuklear
# ============================================================
file(GLOB NK_SRCS "${SRC_DIR}/nuklear/*.c")

# Separate nuklear_font.c which needs different flags
list(FILTER NK_SRCS EXCLUDE REGEX "nuklear_font\\.c$")
list(FILTER NK_SRCS EXCLUDE REGEX "stb_.*\\.c$")

file(GLOB NK_STB_SRCS "${SRC_DIR}/nuklear/stb_*.c")

add_library(nk_main OBJECT ${NK_SRCS})
target_include_directories(nk_main PRIVATE ${SRC_DIR}/include ${SRC_DIR})
target_compile_options(nk_main PRIVATE -Wno-unused-function -std=c89)
target_compile_definitions(nk_main PRIVATE USE_SDL_RENDERER_ONLY EMBED_JPEG_TURBO NDEBUG PL_STATIC _GNU_SOURCE PTHREAD_SEM_NON_MONOTONIC_CLOCK)

add_library(nk_font OBJECT "${SRC_DIR}/nuklear/nuklear_font.c")
target_include_directories(nk_font PRIVATE ${SRC_DIR}/include ${SRC_DIR})
target_compile_options(nk_font PRIVATE -Wno-unused-function)
target_compile_definitions(nk_font PRIVATE USE_SDL_RENDERER_ONLY EMBED_JPEG_TURBO NDEBUG PL_STATIC _GNU_SOURCE PTHREAD_SEM_NON_MONOTONIC_CLOCK)

add_library(nk_stb OBJECT ${NK_STB_SRCS})
target_include_directories(nk_stb PRIVATE ${SRC_DIR}/include ${SRC_DIR})
target_compile_definitions(nk_stb PRIVATE USE_SDL_RENDERER_ONLY EMBED_JPEG_TURBO NDEBUG PL_STATIC _GNU_SOURCE PTHREAD_SEM_NON_MONOTONIC_CLOCK)

# ============================================================
# FEC (Forward Error Correction)
# ============================================================
file(GLOB FEC_SRCS "${SRC_DIR}/fecal/*.cpp")
# Exclude gf256.cpp from main list since it needs special handling
list(FILTER FEC_SRCS EXCLUDE REGEX "gf256\\.cpp$")

add_library(fec_main OBJECT ${FEC_SRCS})
target_include_directories(fec_main PRIVATE ${SRC_DIR}/include ${SRC_DIR})
target_compile_options(fec_main PRIVATE -Wno-implicit-fallthrough -Wno-restrict)
target_compile_definitions(fec_main PRIVATE GF256_TARGET_MOBILE USE_SDL_RENDERER_ONLY EMBED_JPEG_TURBO NDEBUG PL_STATIC _GNU_SOURCE PTHREAD_SEM_NON_MONOTONIC_CLOCK)

# For ARM64, no SSE/AVX variants, just the base with GF256_TARGET_MOBILE
add_library(fec_gf256 OBJECT "${SRC_DIR}/fecal/gf256.cpp")
target_include_directories(fec_gf256 PRIVATE ${SRC_DIR}/include ${SRC_DIR})
target_compile_options(fec_gf256 PRIVATE -Wno-implicit-fallthrough -Wno-restrict)
target_compile_definitions(fec_gf256 PRIVATE GF256_TARGET_MOBILE USE_SDL_RENDERER_ONLY EMBED_JPEG_TURBO NDEBUG PL_STATIC _GNU_SOURCE PTHREAD_SEM_NON_MONOTONIC_CLOCK)

# ============================================================
# Main application sources (LITE mode)
# ============================================================
set(APP_SRCS
    ${SRC_DIR}/main.c
    ${SRC_DIR}/rp_syn.c
    ${SRC_DIR}/ikcp.c
    ${SRC_DIR}/libSTB_image.c
    ${SRC_DIR}/libNK_SDL_renderer.c
    ${SRC_DIR}/ui_common_sdl.c
    ${SRC_DIR}/ui_renderer_sdl.c
    ${SRC_DIR}/ui_main_nk.c
    ${SRC_DIR}/ui_input_redirection.c
    ${SRC_DIR}/ntr_common.c
    ${SRC_DIR}/ntr_hb.c
    ${SRC_DIR}/ntr_rp.c
    ${SRC_DIR}/ntr_huff.c
    ${SRC_DIR}/ntr_jpeg_delta.c
    ${SRC_DIR}/ntr_stats_overlay.c
)

add_library(main SHARED ${APP_SRCS})

target_include_directories(main PRIVATE
    ${SRC_DIR}/include
    ${SRC_DIR}
    ${JT_COMMON_INCLUDES}
)

target_compile_definitions(main PRIVATE
    USE_SDL_RENDERER_ONLY
    EMBED_JPEG_TURBO
    NDEBUG
    PL_STATIC
    _GNU_SOURCE
    PTHREAD_SEM_NON_MONOTONIC_CLOCK
)

target_compile_options(main PRIVATE
    -Wall -Wextra
    -Wno-missing-field-initializers
    -Wno-unknown-pragmas
    -O2
    -fno-strict-aliasing
)

# Link all object libraries
target_link_libraries(main PRIVATE
    SDL3::SDL3
    jt16 jt12_own jt12_from16 jt8_own jt8_from12 jt8_from16 jt_base jt_arm
    nk_main nk_font nk_stb
    fec_main fec_gf256
    log
    android
)

# Make sure jt object libraries can see the right includes
foreach(tgt jt16 jt12_own jt12_from16 jt8_own jt8_from12 jt8_from16 jt_base jt_arm)
    target_compile_definitions(${tgt} PRIVATE EMBED_JPEG_TURBO)
endforeach()
