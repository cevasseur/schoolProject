#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "game.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_tools.h"

bool test_dummy() { return true; }

// Test pour créer un jeu vide
bool test_game_new_empty()
{
  game g = game_new_empty();
  // Vérifier les propriétés du jeu
  if (game_nb_cols(g) != DEFAULT_SIZE || game_nb_rows(g) != DEFAULT_SIZE || game_get_neighbourhood(g) != FULL ||
      game_is_wrapping(g) != false) {
    return false;
  }
  // Vérifier la couleur et la contrainte de chaque cellule
  for (unsigned int i = 0; i < DEFAULT_SIZE; i++) {
    for (unsigned int j = 0; j < DEFAULT_SIZE; j++) {
      if (game_get_color(g, i, j) != EMPTY) {
        game_delete(g);
        return false;
      }
      if (game_get_constraint(g, i, j) != -1) {
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

// Test pour supprimer un jeu
bool test_game_delete()
{
  game g = game_new_empty();
  game_delete(g);
  return true;
}

// Test pour obtenir les contraintes d'un jeu
bool test_game_get_constraint()
{
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8,  9,  -1, 0,
                                                         1,  2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new(constraints, colors);
  // Vérifier si les contraintes sont correctement récupérées
  for (unsigned int i = 0; i < DEFAULT_SIZE; i++) {
    for (unsigned int j = 0; j < DEFAULT_SIZE; j++) {
      if (game_get_constraint(g, i, j) != constraints[DEFAULT_SIZE * i + j]) {
        game_delete(g);
        return false;
      }
    }
  }
  game_delete(g);
  return true;
}

// Test pour obtenir l'état des cellules d'un jeu
bool test_game_get_status()
{
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {4,  4,  4, -1, -1, -1, -1, -1, -1, 2, -1, -1, -1,
                                                         -1, -1, 3, -1, -1, -1, -1, -1, -1, 4, -1, 3};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {BLACK, BLACK, BLACK, EMPTY, EMPTY, BLACK, BLACK, BLACK, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new(constraints, colors);
  // On vérifie tous les cas possibles pour chaque type de voisinage
  if (game_get_status(g, 0, 0) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, false, ORTHO);
  if (game_get_status(g, 0, 1) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 0) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  constraints[0] = 2;
  constraints[1] = 2;
  constraints[2] = 2;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, false, ORTHO_EXCLUDE);
  if (game_get_status(g, 0, 0) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != ERROR) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  constraints[0] = 3;
  constraints[1] = 4;
  constraints[2] = 4;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, false, FULL_EXCLUDE);
  if (game_get_status(g, 0, 0) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != ERROR) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  colors[20] = BLACK;
  colors[21] = BLACK;
  colors[22] = BLACK;
  constraints[0] = 7;
  constraints[1] = 9;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, true, FULL);
  if (game_get_status(g, 0, 0) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  constraints[1] = 8;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, true, FULL_EXCLUDE);
  if (game_get_status(g, 0, 0) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  constraints[0] = 5;
  constraints[1] = 5;
  constraints[2] = 3;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, true, ORTHO);
  if (game_get_status(g, 0, 0) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  constraints[0] = 4;
  constraints[1] = 4;
  constraints[2] = 2;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints, colors, true, ORTHO_EXCLUDE);
  if (game_get_status(g, 0, 0) != UNSATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 1) != SATISFIED) {
    game_delete(g);
    return false;
  }
  if (game_get_status(g, 0, 2) != ERROR) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  return true;
}

// Test pour vérifier si le jeu a été remporté
bool test_game_won()
{
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {-1, 6,  -1, -1, -1, -1, 7, -1, -1, 6, -1, 6, 6,
                                                         -1, -1, -1, -1, -1, -1, 2, 3,  -1, 4, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK,
                                               BLACK, BLACK, WHITE, WHITE, BLACK, BLACK, WHITE, BLACK, BLACK,
                                               WHITE, WHITE, BLACK, BLACK, BLACK, WHITE, WHITE};
  color colors2[DEFAULT_SIZE * DEFAULT_SIZE] = {BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK,
                                                BLACK, BLACK, WHITE, WHITE, BLACK, BLACK, WHITE, BLACK, BLACK,
                                                WHITE, WHITE, BLACK, BLACK, BLACK, WHITE, BLACK};
  game g = game_new(constraints, colors);
  game h = game_new(constraints, colors2);
  // On vérifie tous les cas possibles pour chaque type de voisinage et pour différentes dispositions
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(h);
    game_delete(g);
    return false;
  }
  game_delete(g);
  game_delete(h);
  constraint constraints2[] = {5,  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                               -1, -1, -1, -1, -1, 5,  -1, -1, -1, -1, -1, -1};
  color colors3[] = {BLACK, BLACK, WHITE, WHITE, BLACK, BLACK, WHITE, WHITE, WHITE, WHITE, WHITE, WHITE, WHITE,
                     BLACK, WHITE, WHITE, WHITE, BLACK, BLACK, BLACK, BLACK, WHITE, WHITE, BLACK, WHITE};
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, ORTHO);
  constraints2[0] = 3;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, ORTHO);
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  constraints2[0] = 5;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, ORTHO);
  constraints2[0] = 3;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, ORTHO);
  if (game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (!game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  colors[0] = WHITE;
  constraints2[0] = 4;
  constraints2[18] = 4;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, ORTHO_EXCLUDE);
  constraints2[0] = 3;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, ORTHO_EXCLUDE);
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  constraints2[0] = 4;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, ORTHO_EXCLUDE);
  constraints2[0] = 2;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, ORTHO_EXCLUDE);
  if (game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (!game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  colors3[6] = BLACK;
  colors3[21] = BLACK;
  constraints2[0] = 6;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, FULL_EXCLUDE);
  constraints2[0] = 3;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, FULL_EXCLUDE);
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  constraints2[0] = 3;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, FULL_EXCLUDE);
  constraints2[0] = 1;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, false, FULL_EXCLUDE);
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  colors3[0] = BLACK;
  constraints2[0] = 7;
  constraints2[18] = 5;
  g = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, FULL);
  constraints2[0] = 3;
  h = game_new_ext(DEFAULT_SIZE, DEFAULT_SIZE, constraints2, colors3, true, FULL);
  if (!game_won(g)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  if (game_won(h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  return true;
}

// Test pour le jeu par défaut
bool test_game_default(void)
{
  // On créée un jeu par défaut manuellement et on vérifie qu'il est égal au jeu par défaut créé par la fonction
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                         -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_default();
  game h = game_new(constraints, colors);
  if (!game_equal(g, h)) {
    game_delete(g);
    game_delete(h);
    return false;
  }
  game_delete(g);
  game_delete(h);
  return true;
}

// Test pour annuler des coups dans le jeu
bool test_game_undo(void)
{
  // Initialisation des contraintes et des couleurs pour le jeu
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                         -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new(constraints, colors);
  game_undo(g);
  // On teste encore tous les cas de figure lors de l'annulation de coups et on vérifie que les bonnes couleurs sont
  // retournées
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, WHITE);
  game_undo(g);
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, WHITE);
  game_play_move(g, 0, 0, BLACK);
  game_undo(g);
  if (game_get_color(g, 0, 0) != WHITE) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, EMPTY);
  game_play_move(g, 0, 0, WHITE);
  game_play_move(g, 0, 0, BLACK);
  game_undo(g);
  game_undo(g);
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, BLACK);
  game_undo(g);
  game_redo(g);
  game_undo(g);
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  return true;
}

// Test pour rejouer des coups annulés dans le jeu
bool test_game_redo(void)
{
  // Initialisation des contraintes et des couleurs pour le jeu
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                         -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new(constraints, colors);
  game_redo(g);
  // On teste encore tous les cas de figure de redo de coups et on vérifie que les bonnes couleurs sont retournées
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, WHITE);
  game_undo(g);
  game_redo(g);
  if (game_get_color(g, 0, 0) != WHITE) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, BLACK);
  game_undo(g);
  game_undo(g);
  game_redo(g);
  game_redo(g);
  if (game_get_color(g, 0, 0) != BLACK) {
    game_delete(g);
    return false;
  }
  game_undo(g);
  game_play_move(g, 0, 0, EMPTY);
  game_redo(g);
  if (game_get_color(g, 0, 0) != EMPTY) {
    game_delete(g);
    return false;
  }
  game_play_move(g, 0, 0, WHITE);
  game_undo(g);
  game_redo(g);
  game_redo(g);
  if (game_get_color(g, 0, 0) != WHITE) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  return true;
}

bool test_game_save()
{
  constraint constraints[4 * 6] = {0, -1, -1, 3, -1, -1, 5, -1, -1, -1, -1, -1,
                                   4, -1, 1,  6, -1, 6,  3, -1, -1, -1, -1, -1};
  color colors[4 * 6] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new_ext(4, 6, constraints, colors, false, FULL);
  game_save(g, "test.txt");
  game h = game_load("test.txt");
  if (!game_equal(g, h)) {
    game_delete(g);
    game_delete(h);
    return false;
  } else {
    game_delete(g);
    game_delete(h);
    return true;
  }
}

bool test_game_load()
{
  constraint constraints[4 * 6] = {0, -1, -1, 3, -1, -1, 5, -1, -1, -1, -1, -1,
                                   4, -1, 1,  6, -1, 6,  3, -1, -1, -1, -1, -1};
  color colors[4 * 6] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new_ext(4, 6, constraints, colors, false, FULL);
  game_save(g, "test.txt");
  game h = game_load("test.txt");
  if (!game_equal(g, h)) {
    game_delete(g);
    game_delete(h);
    return false;
  } else {
    game_delete(g);
    game_delete(h);
    return true;
  }
}
bool test_game_solve()
{
  constraint constraints[4] = {1, -1, -1, -1};
  color colors[4] = {EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new_ext(2, 2, constraints, colors, false, FULL);
  game_save(g, "dgdgrrg.txt");
  game_solve(g);
  printf("game won : %d\n", game_solve(g));
  if (!game_won(g)) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  return true;
}

bool test_game_nb_solutions()
{
  constraint constraints[4] = {1, -1, -1, -1};
  color colors[4] = {EMPTY, EMPTY, EMPTY, EMPTY};
  game g = game_new_ext(2, 2, constraints, colors, false, FULL);
  if (game_nb_solutions(g) != 4) {
    game_delete(g);
    return false;
  }
  game_delete(g);
  return true;
}

// Le main appelle la fonction de test correspondante au premier argument et vérifie si le test a réussi ou non
int main(int argc, char* argv[])
{
  if (argc < 2) {
    fprintf(stderr, "Error: not enough arguments\n");
    return EXIT_FAILURE;
  }

  bool ok = false;
  if (strcmp("dummy", argv[1]) == 0) {
    ok = test_dummy();
  } else if (strcmp("game_new_empty", argv[1]) == 0) {
    ok = test_game_new_empty();
  } else if (strcmp("game_delete", argv[1]) == 0) {
    ok = test_game_delete();
  } else if (strcmp("game_get_constraint", argv[1]) == 0) {
    ok = test_game_get_constraint();
  } else if (strcmp("game_get_status", argv[1]) == 0) {
    ok = test_game_get_status();
  } else if (strcmp("game_won", argv[1]) == 0) {
    ok = test_game_won();
  } else if (strcmp("game_default", argv[1]) == 0) {
    ok = test_game_default();
  } else if (strcmp("game_undo", argv[1]) == 0) {
    ok = test_game_undo();
  } else if (strcmp("game_redo", argv[1]) == 0) {
    ok = test_game_redo();
  } else if (strcmp("game_save", argv[1]) == 0) {
    ok = test_game_save();
  } else if (strcmp("game_load", argv[1]) == 0) {
    ok = test_game_load();
  } else if (strcmp("game_solve", argv[1]) == 0) {
    ok = test_game_solve();
  } else if (strcmp("game_nb_solutions", argv[1]) == 0) {
    ok = test_game_nb_solutions();
  } else {
    fprintf(stderr, "Error: test \"%s\" not found!\n", argv[1]);
    exit(EXIT_FAILURE);
  }

  if (ok == true) {
    fprintf(stderr, "Test \"%s\" finished: SUCCESS\n", argv[1]);
    return EXIT_SUCCESS;
  } else {
    fprintf(stderr, "Test \"%s\" finished: FAILURE\n", argv[1]);
    return EXIT_FAILURE;
  }
}