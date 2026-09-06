cmake_minimum_required(VERSION 2.6)
project(game_text C)

include(CTest)
enable_testing()

## find SDL2
include(sdl2.cmake)

## compilation rules
include_directories(${SDL2_ALL_INC})
add_executable(game_sdl main.c game_sdl.c)
target_link_libraries(game_sdl ${SDL2_ALL_LIBS} m game)

#set(CMAKE_C_FLAGS "-std=c99 -g -Wall -O3")
set(CMAKE_C_FLAGS "-Wall -std=c99")            # options de compilation génériques
set(CMAKE_C_FLAGS_DEBUG "-g -O0 --coverage")   # options de compilation en mode Debug
set(CMAKE_C_FLAGS_RELEASE "-O3")
link_directories(${CMAKE_SOURCE_DIR})
add_executable(game_text game_text.c)
add_executable(game_test_lsoetens game_test_lsoetens.c)
add_executable(game_test_mtheodoredel game_test_mtheodoredel.c)
add_executable(game_test_cevasseur game_test_cevasseur.c)
add_executable(game_solve game_solve.c)
add_library(game game.c game_aux.c game_ext.c game_annexe.c game_tools.c queue.c game_solve.c)
# Fin ajout des add_library

target_link_libraries(game_text game)
target_link_libraries(game_test_mtheodoredel game)
target_link_libraries(game_test_lsoetens game)
target_link_libraries(game_test_cevasseur game)
target_link_libraries(game_solve game)
add_test(test_mtheodoredel_game_new_empty ./game_test_mtheodoredel game_new_empty)
add_test(test_mtheodoredel_game_delete ./game_test_mtheodoredel game_delete game_annexe.c)
add_test(test_mtheodoredel_game_get_constraint ./game_test_mtheodoredel game_get_constraint)
add_test(test_mtheodoredel_game_get_status ./game_test_mtheodoredel game_get_status)
add_test(test_mtheodoredel_game_won ./game_test_mtheodoredel game_won)
add_test(test_mtheodoredel_game_default ./game_test_mtheodoredel game_default)
add_test(test_mtheodoredel_game_save ./game_test_mtheodoredel game_save)
add_test(test_mtheodoredel_game_load ./game_test_mtheodoredel game_load)
add_test(test_mtheodoredel_game_solve ./game_test_mtheodoredel game_solve)
add_test(test_mtheodoredel_game_nb_solutions ./game_test_mtheodoredel game_nb_solutions)

add_test(test_lsoetens_test_game_new ./game_test_lsoetens test_game_new)
add_test(test_lsoetens_test_game_equal ./game_test_lsoetens test_game_equal)
add_test(test_lsoetens_test_game_set_color ./game_test_lsoetens test_game_set_color)
add_test(test_lsoetens_test_game_get_next_square ./game_test_lsoetens test_game_get_next_square)
add_test(test_lsoetens_test_game_play_move ./game_test_lsoetens test_game_play_move)
add_test(test_lsoetens_test_game_print ./game_test_lsoetens test_game_print)

add_test(test_cevasseur_test_game_copy ./game_test_cevasseur game_copy)
add_test(test_cevasseur_test_game_set_constraint ./game_test_cevasseur set_constraint)
add_test(test_cevasseur_test_game_get_color ./game_test_cevasseur get_color)
add_test(test_cevasseur_test_game_nb_neighbors ./game_test_cevasseur nb_neighbors)
add_test(test_cevasseur_test_game_restart ./game_test_cevasseur restart)
add_test(test_cevasseur_test_game_default_solution ./game_test_cevasseur solution)

# FAIRE LES TESTS POUR GAME_EXT.C

add_test(test_cevasseur_test_game_new_ext ./game_test_cevasseur new_ext)
add_test(test_cevasseur_test_game_new_empty_ext ./game_test_cevasseur new_empty_ext)
add_test(test_cevasseur_test_game_nb_rows ./game_test_cevasseur rows)
add_test(test_cevasseur_test_game_nb_cols ./game_test_cevasseur cols)
add_test(test_cevasseur_test_game_is_wrapping ./game_test_cevasseur wrapping)
add_test(test_cevasseur_test_game_get_neighbourhood ./game_test_cevasseur neighbourhood)
add_test(test_mtheodoredel_game_undo ./game_test_mtheodoredel game_undo)
add_test(test_mtheodoredel_game_redo ./game_test_mtheodoredel game_redo)


# add_test(test_queue_new_free ./test_queue init_free)
# add_test(test_queue_push_head ./test_queue push_head)
# add_test(test_queue_pop_head ./test_queue pop_head)
# add_test(test_queue_push_tail ./test_queue push_tail)
# add_test(test_queue_pop_tail ./test_queue pop_tail)
# add_test(test_queue_length ./test_queue length)
# add_test(test_queue_empty ./test_queue empty)
# add_test(test_queue_clear ./test_queue clear)