project(termux-api)
cmake_minimum_required(VERSION 3.4.1)
add_library(termux-api-static STATIC src/main/cpp/termux-api.c)
#可执行文件
add_executable(termux-api src/main/cpp/termux-api-broadcast.c)
target_link_libraries(termux-api termux-api-static)


#编译共享so
#project(termux-api)
#cmake_minimum_required(VERSION 3.4.1)
#aux_source_directory(src/main/cpp/ CPP_SRCS)
#file(GLOB_RECURSE  CPP_SRCS "*.cpp" "*.c")
#add_library(termux-api SHARED ${CPP_SRCS})
#target_link_libraries(termux-api)