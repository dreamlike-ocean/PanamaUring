cmake_minimum_required(VERSION 3.25)
project(playgroud)

set(CMAKE_CXX_STANDARD 11)
set(EXE_SOURCES src/main.cpp)


SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../resources)


file(GLOB_RECURSE SHARED_LIB_SOURCES
        src/dynamic/*.cpp
        src/dynamic/*.h
)

add_library(person SHARED
        ${SHARED_LIB_SOURCES}
)
set_target_properties(person PROPERTIES OUTPUT_NAME "person")
set_target_properties(person PROPERTIES SUFFIX ".so")

target_include_directories(person
        PUBLIC
        src/dynamic
)
if (CMAKE_BUILD_TYPE)
    if(CMAKE_BUILD_TYPE STREQUAL "Release")
        message (
                STATUS " add_definitions(-Dflag)"
        )
        add_definitions(-Dflag)
    endif ()
    message (
            STATUS "CMAKE_BUILD_TYPE当前设定为 ${CMAKE_BUILD_TYPE}"
    )
endif ()

if (NOT CMAKE_BUILD_TYPE)
    set (CMAKE_BUILD_TYPE "Debug")
    message (
            STATUS "No CMAKE_BUILD_TYPE selected, defaulting to ${CMAKE_BUILD_TYPE}"
    )

endif ()

file(GLOB_RECURSE ALL_SRC
        src/*.cpp
        src/*.h
)
