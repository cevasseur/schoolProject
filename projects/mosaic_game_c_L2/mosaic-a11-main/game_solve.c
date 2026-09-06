#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "game.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_tools.h"
int main(int argc, char* argv[])
{
  char* option = argv[1];
  game g = game_load(argv[2]);
  if (strcmp("-s", option) == 0) {
    bool ok = game_solve(g);
    if (!ok) {
      game_print(g);
      return EXIT_FAILURE;
    } else if (argc == 4) {
      game_save(g, argv[3]);
    } else {
      game_print(g);
    }
  } else if (strcmp("-c", option) == 0) {
    uint nb_sol = game_nb_solutions((cgame)g);
    if (argc == 4) {
      FILE* outpout = fopen(argv[3], "w");
      fprintf(outpout, "%u\n", nb_sol);
      fclose(outpout);
    } else {
      printf("%u\n", nb_sol);
    }
  } else {
    return EXIT_FAILURE;
  }
  return EXIT_SUCCESS;
}
