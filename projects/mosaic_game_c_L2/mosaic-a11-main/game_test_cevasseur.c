#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "game.h"
#include "game_annexe.h"
#include "game_aux.h"
#include "game_ext.h"

bool test_game_copy()
{  // 3
  constraint constraint_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {2, 4, 3, 8,  6, 5, 5, 4, 3, 1, 7,  4, -1,
                                                            3, 2, 5, -1, 8, 6, 5, 1, 7, 4, -1, 3};
  color color_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, BLACK, WHITE, BLACK, WHITE, EMPTY, WHITE, EMPTY, BLACK,
                                                  BLACK, WHITE, BLACK, WHITE, EMPTY, WHITE, EMPTY, BLACK, EMPTY,
                                                  WHITE, BLACK, WHITE, EMPTY, WHITE, EMPTY, BLACK};
  game g = game_new(constraint_tab, color_tab);
  game g_copy = game_copy(g);
  game g_ext = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraint_tab, color_tab, true, FULL);
  game g_ext_copy = game_copy(g_ext);
  for (uint i = 0; i < DEFAULT_SIZE; i++) {
    for (uint j = 0; j < DEFAULT_SIZE; j++) {
      if ((game_get_color(g, i, j) != game_get_color(g_copy, i, j)) ||
          (game_get_constraint(g, i, j) != game_get_constraint(g_copy, i, j)) ||
          (game_get_color(g_ext, i, j) != game_get_color(g_ext_copy, i, j)) ||
          (game_get_constraint(g_ext, i, j) != game_get_constraint(g_ext_copy, i, j)) ||
          (game_get_neighbourhood(g_ext) != game_get_neighbourhood(g_ext_copy)) ||
          (game_is_wrapping(g_ext) !=
           game_is_wrapping(
               g_ext_copy))) {  // On compare que les valeurs des parties copiées correspondent aux parties de base
        game_delete(g_copy);
        game_delete(g);
        game_delete(g_ext);
        game_delete(g_ext_copy);
        return false;
      }
    }
  }
  game_delete(g_copy);
  game_delete(g);
  game_delete(g_ext);
  game_delete(g_ext_copy);
  return true;
}

bool test_game_set_constraint()
{  // 6
  game g = game_default();
  game g_ext_ortho = game_new_empty_ext(DEFAULT_SIZE, DEFAULT_SIZE, true, ORTHO);
  game g_ext_orthoex = game_new_empty_ext(DEFAULT_SIZE, DEFAULT_SIZE, true, ORTHO_EXCLUDE);
  game g_ext_fullex = game_new_empty_ext(DEFAULT_SIZE, DEFAULT_SIZE, true, FULL_EXCLUDE);
  game_set_constraint(g, 0, 0, 4);
  game_set_constraint(g_ext_ortho, 0, 0, 4);
  game_set_constraint(g_ext_orthoex, 0, 0, 4);
  game_set_constraint(g_ext_fullex, 0, 0, 4);  // On change une coordonnées dans chaque partie
  constraint change = game_get_constraint(g, 0, 0), change_ortho = game_get_constraint(g_ext_ortho, 0, 0),
             change_orthoex = game_get_constraint(g_ext_orthoex, 0, 0),
             change_fullex = game_get_constraint(g_ext_fullex, 0,
                                                 0);  // On recupere la valeur voulue changé pour la verifier plus tard
  game_delete(g);
  game_delete(g_ext_ortho);
  game_delete(g_ext_orthoex);
  game_delete(g_ext_fullex);  // Ca permet de tout delete directement
  return (change == 4 && change_ortho == 4 && change_orthoex == 4 && change_fullex == 4);
}

bool test_game_get_color()
{  // 9
  constraint constraint_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {2, 4, 3, 8,  6, 5, 5, 4, 3, 1, 7,  4, -1,
                                                            3, 2, 5, -1, 8, 6, 5, 1, 7, 4, -1, 3};
  color color_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                                  EMPTY, WHITE, WHITE, WHITE, WHITE, WHITE, BLACK, BLACK, BLACK,
                                                  BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK};
  game g = game_new(constraint_tab, color_tab);
  for (uint i = 0; i < DEFAULT_SIZE; i++) {
    for (uint j = 0; j < DEFAULT_SIZE; j++) {
      if ((i < 0) || (i >= DEFAULT_SIZE) || (j < 0) || (j >= DEFAULT_SIZE)) {  // Verification des coordonnees valides
        game_delete(g);
        return false;
      }
      if (i < 2 && game_get_color(g, i, j) != EMPTY) {  // On verifie bien la couleur
        game_delete(g);
        return false;
      }
      if (i == 2 && game_get_color(g, i, j) != WHITE) {
        game_delete(g);
        return false;
      }
      if (i > 2 && game_get_color(g, i, j) != BLACK) {
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

bool test_game_nb_neighbors()
{  // 12
  constraint constraint_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {2, 4, 3, 8,  6, 5, 5, 4, 3, 1, 7,  4, -1,
                                                            3, 2, 5, -1, 8, 6, 5, 1, 7, 4, -1, 3};
  color color_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, EMPTY, BLACK, WHITE,
                                                  EMPTY, BLACK, BLACK, WHITE, EMPTY, WHITE, BLACK, WHITE, EMPTY,
                                                  WHITE, BLACK, EMPTY, BLACK, WHITE, EMPTY, EMPTY};
  game g = game_new(constraint_tab, color_tab);
  game g2 = game_new_ext(5, 5, constraint_tab, color_tab, true, ORTHO);
  bool good_nb_neigh = ((game_nb_neighbors(g2, 1, 4, game_get_color(g2, 1, 4)) == 1) &&
                        (game_nb_neighbors(g, 2, 2, game_get_color(g, 2, 2)) == 4));
  game_delete(g);
  game_delete(g2);
  return good_nb_neigh;
}

bool test_game_restart()
{  // 15
  constraint constraint_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {2, 4, 3, 8,  6, 5, 5, 4, 3, 1, 7,  4, -1,
                                                            3, 2, 5, -1, 8, 6, 5, 1, 7, 4, -1, 3};
  color color_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, WHITE, BLACK, EMPTY, WHITE, WHITE, EMPTY, BLACK, WHITE,
                                                  EMPTY, BLACK, BLACK, WHITE, EMPTY, WHITE, BLACK, WHITE, EMPTY,
                                                  WHITE, BLACK, EMPTY, BLACK, WHITE, EMPTY, EMPTY};
  game g = game_new(constraint_tab, color_tab);
  game_restart(g);
  for (uint i = 0; i < DEFAULT_SIZE; i++) {
    for (uint j = 0; j < DEFAULT_SIZE; j++) {
      if (game_get_color(g, i, j) != EMPTY) {  // On verifie bien que les cases ont bien ete reinitialise
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

bool test_game_default_solution()
{  // 18
  constraint constraint_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                            -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color color_tab[DEFAULT_SIZE * DEFAULT_SIZE] = {WHITE, WHITE, BLACK, WHITE, WHITE, WHITE, WHITE, BLACK, WHITE,
                                                  BLACK, BLACK, BLACK, BLACK, WHITE, WHITE, BLACK, BLACK, WHITE,
                                                  WHITE, WHITE, BLACK, BLACK, BLACK, BLACK, WHITE};
  game g = game_new(constraint_tab, color_tab);
  game g_sol = game_default_solution();
  bool equal =
      game_equal(g, g_sol);  // On compare que les deux parties sont bien égales pour être sur qu'elle soit gagné
  game_delete(g);
  game_delete(g_sol);
  return equal;
}

bool test_game_new_ext()
{
  constraint constraints[4 * 5] = {-1, -1, -1, 5, 3, -1, 2, -1, -1, 0, 3, -1, 2, -1, -1, 1, 5, 4, -1, 5};
  color colors[4 * 5] = {EMPTY, EMPTY, EMPTY, BLACK, BLACK, EMPTY, BLACK, EMPTY, EMPTY, WHITE,
                         WHITE, EMPTY, BLACK, EMPTY, EMPTY, WHITE, WHITE, BLACK, EMPTY, BLACK};
  game g = game_new_ext(4, 5, constraints, colors, true, ORTHO);
  uint nb_cols = game_nb_cols(g), nb_rows = game_nb_rows(g);
  if (!game_is_wrapping(g) || nb_rows != 4 || nb_cols != 5 || game_get_neighbourhood(g) != ORTHO) {
    game_delete(g);
    return false;
  }
  for (uint i = 0; i < nb_rows; i++) {
    for (uint j = 0; j < nb_cols; j++) {
      if (game_get_constraint(g, i, j) != constraints[i * nb_cols + j] ||
          game_get_color(g, i, j) !=
              colors[i * nb_cols + j]) {  // On verifie que les contraintes et couleurs sont correctes
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

bool test_game_new_empty_ext()
{
  uint rows = 4, cols = 5;
  game g = game_new_empty_ext(rows, cols, true, ORTHO);
  if (game_is_wrapping(g) != true || game_nb_rows(g) != 4 || game_nb_cols(g) != 5 ||
      game_get_neighbourhood(g) != ORTHO) {  // On verifie que les valeurs sont correctement associé
    game_delete(g);
    return false;
  }
  for (uint i = 0; i < game_nb_rows(g); i++) {
    for (uint j = 0; j < game_nb_cols(g); j++) {
      if (game_get_color(g, i, j) != EMPTY) {  // Toutes les cases sont vides
        game_delete(g);
        return false;
      }
      if (game_get_constraint(g, i, j) != UNCONSTRAINED) {  // Tout est bien sans contrainte
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

bool test_game_nb_rows()
{
  game g = game_new_empty_ext(7, 5, true, FULL);
  bool good_nb_rows = (g != NULL && game_nb_rows(g) == 7);
  game_delete(g);
  return good_nb_rows;
}

bool test_game_nb_cols()
{
  game g = game_new_empty_ext(7, 5, false, ORTHO);
  bool good_nb_cols = (g != NULL && game_nb_cols(g) == 5);
  game_delete(g);
  return good_nb_cols;
}

bool test_game_is_wrapping()
{
  game g = game_new_empty_ext(7, 5, true, FULL_EXCLUDE);
  bool good_wrap = (g != NULL && game_is_wrapping(g));
  game_delete(g);
  return good_wrap;
}

bool test_game_get_neighbourhood()
{
  game g = game_new_empty_ext(7, 5, false, ORTHO_EXCLUDE);
  bool good_neigh = (game_get_neighbourhood(g) == ORTHO_EXCLUDE && g != NULL);
  game_delete(g);
  return good_neigh;
}

int main(int argc, char* argv[])
{
  if (argc <= 1) {
    fprintf(stderr, "Usage: %s <testname> [<...>]\n", argv[0]);
    exit(EXIT_FAILURE);
  }
  fprintf(stderr, "=> Start test \"%s\"\n", argv[1]);
  bool ok = false;
  if (strcmp("game_copy", argv[1]) == 0) {
    ok = test_game_copy();
  } else if (strcmp("set_constraint", argv[1]) == 0) {
    ok = test_game_set_constraint();
  } else if (strcmp("get_color", argv[1]) == 0) {
    ok = test_game_get_color();
  } else if (strcmp("nb_neighbors", argv[1]) == 0) {
    ok = test_game_nb_neighbors();
  } else if (strcmp("restart", argv[1]) == 0) {
    ok = test_game_restart();
  } else if (strcmp("solution", argv[1]) == 0) {
    ok = test_game_default_solution();
  } else if (strcmp("new_ext", argv[1]) == 0) {
    ok = test_game_new_ext();
  } else if (strcmp("new_empty_ext", argv[1]) == 0) {
    ok = test_game_new_empty_ext();
  } else if (strcmp("rows", argv[1]) == 0) {
    ok = test_game_nb_rows();
  } else if (strcmp("cols", argv[1]) == 0) {
    ok = test_game_nb_cols();
  } else if (strcmp("wrapping", argv[1]) == 0) {
    ok = test_game_is_wrapping();
  } else if (strcmp("neighbourhood", argv[1]) == 0) {
    ok = test_game_get_neighbourhood();
  } else {
    fprintf(stderr, "Error: test \"%s\" not found!\n", argv[1]);
    exit(EXIT_FAILURE);
  }
  if (ok) {
    fprintf(stderr, "Test \"%s\" finished: SUCCESS\n", argv[1]);
    return EXIT_SUCCESS;
  } else {
    fprintf(stderr, "Test \"%s\" finished: FAILURE\n", argv[1]);
    return EXIT_FAILURE;
  }
}