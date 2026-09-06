#include "game_tools.h"

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "game.h"
#include "game_annexe.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_struct.h"
#include "queue.h"

game game_load(char* filename)
{
  if (filename == NULL) {
    exit(EXIT_FAILURE);
  }
  FILE* file = fopen(filename, "r");
  if (file == NULL) {
    fprintf(stderr, "Impossible d'ouvrir le fichier\n");
    exit(EXIT_FAILURE);
  }
  uint nb_rows = 0, nb_cols = 0, wrapping = 0, neigh = 0;
  int res = fscanf(file, "%u %u %u %u\n", &nb_rows, &nb_cols, &wrapping, &neigh);
  if (res != 4) {
    fprintf(stderr, "Erreur de lecture\n");
    exit(EXIT_FAILURE);
  }
  if ((wrapping != 0 && wrapping != 1) || (neigh < 0 || neigh > 3)) {
    exit(EXIT_FAILURE);
  }
  game g = game_new_empty_ext(nb_rows, nb_cols, wrapping, neigh);
  for (uint i = 0; i < nb_rows; i++) {
    for (uint j = 0; j < nb_cols; j++) {
      char ch1, ch2;
      res = fscanf(file, "%c%c", &ch1, &ch2);
      if (res != 2) {
        fprintf(stderr, "Erreur de lecture\n");
        exit(EXIT_FAILURE);
      }
      constraint c1 = convert_to_constraint(ch1);
      color c2 = convert_to_color(ch2);
      game_set_constraint(g, i, j, c1);
      game_set_color(g, i, j, c2);
    }
    fgetc(file);
  }
  fclose(file);
  return g;
}

void game_save(cgame g, char* filename)
{
  if (g == NULL) {
    exit(EXIT_FAILURE);
  }
  FILE* file = fopen(filename, "w");
  if (file == NULL) {
    fprintf(stderr, "Impossible d'ouvrir le fichier\n");
    exit(EXIT_FAILURE);
  }
  fprintf(file, "%u %u %u %u\n", game_nb_rows(g), game_nb_cols(g), game_is_wrapping(g), game_get_neighbourhood(g));
  for (uint i = 0; i < game_nb_rows(g); i++) {
    for (uint j = 0; j < game_nb_cols(g); j++) {
      char ch1 = convert_from_constraint(game_get_constraint(g, i, j));
      char ch2 = convert_from_color(game_get_color(g, i, j));
      fprintf(file, "%c%c", ch1, ch2);
    }
    fprintf(file, "\n");
  }
  fclose(file);
}

uint game_nb_solutions(cgame g)
{
  game g2 = game_copy(g);
  int len_tab = game_nb_cols(g) * game_nb_rows(g);
  unsigned long nbwords = 0;
  bool win = false;
  genWords(0, len_tab, g2->colors, &nbwords, g2, false, &win);
  game_delete(g2);
  return nbwords;
}

bool game_solve(game g)
{
  unsigned long nbwords = 0;
  bool win = false;
  genWords(0, game_nb_cols(g) * game_nb_rows(g), g->colors, &nbwords, g, true, &win);
  if (!win) {
    for (int i = 0; i < game_nb_rows(g); i++) {
      for (int j = 0; j < game_nb_cols(g); j++) {
        game_set_color(g, i, j, EMPTY);
      }
    }
  }
  return win;
}
