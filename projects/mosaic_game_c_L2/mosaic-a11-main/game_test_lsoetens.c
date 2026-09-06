#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "game.h"
#include "game_aux.h"
#include "game_ext.h"
//
int test_dummy() { return EXIT_SUCCESS; }

int test_game_new()
{
  // Test si une game est bien crée
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                         -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, WHITE};
  game g = game_new(constraints, colors);
  // vérification si les constraints et les couleurs sont bonnes
  for (unsigned int i = 0; i < DEFAULT_SIZE; i++) {
    for (unsigned int j = 0; j < DEFAULT_SIZE; j++) {
      if (game_get_constraint(g, i, j) != constraints[i * DEFAULT_SIZE + j] ||
          game_get_color(g, i, j) != colors[i * DEFAULT_SIZE + j]) {
        game_delete(g);
        return EXIT_FAILURE;
      }
    }
  }
  if (game_is_wrapping(g) != false || game_nb_cols(g) != DEFAULT_SIZE || game_nb_rows(g) != DEFAULT_SIZE ||
      game_get_neighbourhood(g) != FULL) {
    game_delete(g);
    return EXIT_FAILURE;
  }
  game_delete(g);
  return EXIT_SUCCESS;
}

int test_game_equal()
{
  // Test si deux game sont bien égales
  constraint constraints1[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                          -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0};
  constraint constraints2[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                          -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, WHITE};
  game g1 = game_new(constraints1, colors);
  game g2 = game_new(constraints1, colors);
  game g3 = game_new(constraints2, colors);
  game g4 = game_new(constraints1, colors);
  game g1_wrap = game_new_ext(5, 5, constraints1, colors, true, FULL);
  game g2_wrap = game_new_ext(5, 5, constraints2, colors, true, FULL);
  if (game_equal(g1, g2) == true && game_equal(g3, g4) == false && game_equal(g1_wrap, g2_wrap) == false) {
    game_delete(g1);
    game_delete(g2);
    game_delete(g3);
    game_delete(g4);
    game_delete(g1_wrap);
    game_delete(g2_wrap);
    return EXIT_SUCCESS;
  } else {
    game_delete(g1);
    game_delete(g2);
    game_delete(g3);
    game_delete(g4);
    game_delete(g1_wrap);
    game_delete(g2_wrap);
    return EXIT_FAILURE;
  }
}

int test_game_set_color()
{
  // Test si une couleur est bien set
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                         -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, WHITE};
  game g = game_new(constraints, colors);
  game g2 = game_copy(g);
  game_set_color(g, 1, 2, BLACK);
  if (game_equal(g, g2) == false && game_get_color(g, 1, 2) == BLACK) {
    game_delete(g2);
    game_delete(g);
    return EXIT_SUCCESS;
  } else {
    game_delete(g2);
    game_delete(g);
    return EXIT_FAILURE;
  }
}

int test_game_get_next_square()
{
  // Test pour obtenir l'emplacement de la prochaine case
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                         -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, WHITE};
  game g = game_new(constraints, colors);
  game g2 = game_new_ext(5, 5, constraints, colors, true, FULL);
  uint pi;
  uint pj;
  uint pi_wrap;
  uint pj_wrap;
  uint pi_wrap2;
  uint pj_wrap2;
  uint pi_wrap3;
  uint pj_wrap3;
  uint pi_wrap4;
  uint pj_wrap4;
  // test des directions dans plusieurs coins
  bool down = game_get_next_square(g, 4, 4, DOWN, &pi, &pj);
  bool right = game_get_next_square(g, 4, 4, RIGHT, &pi, &pj);
  bool up = game_get_next_square(g, 0, 0, UP, &pi, &pj);
  bool left = game_get_next_square(g, 0, 0, LEFT, &pi, &pj);
  bool down_left = game_get_next_square(g, 4, 4, DOWN_LEFT, &pi, &pj);
  bool up_right = game_get_next_square(g, 4, 4, UP_RIGHT, &pi, &pj);
  bool up_left = game_get_next_square(g, 0, 0, UP_LEFT, &pi, &pj);
  bool down_right = game_get_next_square(g, 4, 4, DOWN_RIGHT, &pi, &pj);
  if (down || right || up || left || down_left || up_right || up_left || down_right) {
    game_delete(g);
    return EXIT_FAILURE;
  }
  direction tab[9] = {HERE, UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT};
  for (int i = 0; i < 9; i++) {
    // Test en même temps sur les 4 coins et une autre case
    game_get_next_square(g, 1, 1, tab[i], &pi, &pj);
    game_get_next_square(g2, 0, 0, tab[i], &pi_wrap, &pj_wrap);
    game_get_next_square(g2, 4, 4, tab[i], &pi_wrap2, &pj_wrap2);
    game_get_next_square(g2, 4, 0, tab[i], &pi_wrap3, &pj_wrap3);
    game_get_next_square(g2, 0, 4, tab[i], &pi_wrap4, &pj_wrap4);
    // vérifie que les coordonnées renvoyer par get_next_square dans les variables sont bon
    if (tab[i] == HERE && (pi != 1 || pj != 1) && (pi_wrap != 0 || pj_wrap != 0) && (pi_wrap2 != 4 || pj_wrap2 != 4) &&
        (pi_wrap3 != 4 || pj_wrap3 != 0) && (pi_wrap4 != 0 || pj_wrap4 != 4)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == UP && (pi != 0 || pj != 1) && (pi_wrap != 4 || pj_wrap != 1) && (pi_wrap2 != 3 || pj_wrap2 != 4) &&
        (pi_wrap3 != 3 || pj_wrap3 != 0) && (pi_wrap4 != 4 || pj_wrap4 != 4)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == DOWN && (pi != 2 || pj != 1) && (pi_wrap != 1 || pj_wrap != 1) && (pi_wrap2 != 0 || pj_wrap2 != 4) &&
        (pi_wrap3 != 0 || pj_wrap3 != 4) && (pi_wrap4 != 1 || pj_wrap4 != 4)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == LEFT && (pi != 1 || pj != 0) && (pi_wrap != 0 || pj_wrap != 4) && (pi_wrap2 != 4 || pj_wrap2 != 3) &&
        (pi_wrap3 != 4 || pj_wrap3 != 4) && (pi_wrap4 != 0 || pj_wrap4 != 4)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == RIGHT && (pi != 1 || pj != 2) && (pi_wrap != 0 || pj_wrap != 1) && (pi_wrap2 != 4 || pj_wrap2 != 0) &&
        (pi_wrap3 != 4 || pj_wrap3 != 1) && (pi_wrap4 != 0 || pj_wrap4 != 0)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == UP_LEFT && (pi != 0 || pj != 0) && (pi_wrap != 4 || pj_wrap != 4) &&
        (pi_wrap2 != 3 || pj_wrap2 != 3) && (pi_wrap3 != 3 || pj_wrap3 != 4) && (pi_wrap4 != 4 || pj_wrap4 != 3)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == UP_RIGHT && (pi != 0 || pj != 2) && (pi_wrap != 4 || pj_wrap != 1) &&
        (pi_wrap2 != 3 || pj_wrap2 != 0) && (pi_wrap3 != 3 || pj_wrap3 != 1) && (pi_wrap4 != 4 || pj_wrap4 != 0)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == DOWN_LEFT && (pi != 2 || pj != 0) && (pi_wrap != 1 || pj_wrap != 4) &&
        (pi_wrap2 != 0 || pj_wrap2 != 3) && (pi_wrap3 != 0 || pj_wrap3 != 4) && (pi_wrap4 != 3 || pj_wrap4 != 3)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
    if (tab[i] == DOWN_RIGHT && (pi != 2 || pj != 2) && (pi_wrap != 1 || pj_wrap != 1) &&
        (pi_wrap2 != 0 || pj_wrap2 != 0) && (pi_wrap3 != 0 || pj_wrap3 != 1) && (pi_wrap4 != 3 || pj_wrap4 != 0)) {
      game_delete(g2);
      game_delete(g);
      return EXIT_FAILURE;
    }
  }
  game_delete(g);
  game_delete(g2);
  return EXIT_SUCCESS;
}

int test_game_play_move()
{
  // Test si un coup est bien jouer
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {1,  2, 3, 4, 5, 7, 9, -1, -1, 3, 4, 8, 9,
                                                         -1, 3, 7, 4, 8, 2, 1, -1, 0,  3, 4, 0};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY};
  game g = game_new(constraints, colors);
  game g2 = game_copy(g);
  game_play_move(g, 3, 2, EMPTY);
  game_undo(g);
  game_redo(g);
  if (game_get_color(g, 3, 2) != EMPTY || game_equal(g, g2)) {
    game_delete(g);
    game_delete(g2);
    return EXIT_FAILURE;
  }
  game_delete(g);
  game_delete(g2);
  return EXIT_SUCCESS;
}

int test_game_print()
{  // Vérifie que la fonction print s'excécute bien
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0, -1,
                                                         -1, 3, 7, 6, 2, 4, 8, 2, 1, 0, 5, 0};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK, EMPTY, WHITE, BLACK,
                                               EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, WHITE};
  game g = game_new(constraints, colors);
  game_print(g);
  game_delete(g);
  return EXIT_SUCCESS;
}

int main(int argc, char* argv[])
{
  if (argc < 2) {
    return EXIT_FAILURE;
  }
  int ok = 1;
  if (strcmp("dummy", argv[1]) == 0) {
    ok = test_dummy();
  } else if (strcmp("test_game_new", argv[1]) == 0) {
    ok = test_game_new();
  } else if (strcmp("test_game_equal", argv[1]) == 0) {
    ok = test_game_equal();
  } else if (strcmp("test_game_set_color", argv[1]) == 0) {
    ok = test_game_set_color();
  } else if (strcmp("test_game_get_next_square", argv[1]) == 0) {
    ok = test_game_get_next_square();
  } else if (strcmp("test_game_play_move", argv[1]) == 0) {
    ok = test_game_play_move();
  } else if (strcmp("test_game_print", argv[1]) == 0) {
    ok = test_game_print();
  } else {
    fprintf(stderr, "Error: test \"%s\" not found!\n", argv[1]);
    return EXIT_FAILURE;
  }
  if (ok == 0) {
    fprintf(stderr, "Test \"%s\" finished: SUCCESS\n", argv[1]);
    return EXIT_SUCCESS;
  } else {
    fprintf(stderr, "Test \"%s\" finished: FAILED\n", argv[1]);
    return EXIT_FAILURE;
  }
}