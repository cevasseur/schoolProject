#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"
#include "game_annexe.h"
#include "game_aux.h"
#include "game_ext.h"
#include "game_struct.h"
#include "game_tools.h"

static void print_all_error(cgame game)
{
  for (int i = 0; i < game_nb_rows(game); i++) {
    for (int j = 0; j < game_nb_cols(game); j++) {
      if (game_get_status(game, i, j) == ERROR) {
        printf("Error at the square (%d,%d)\n", i, j);
      }
    }
  }
}

static void print_help()
{
  printf("-press 's <filename>' to save your game\n");
  printf("-press 'w <i> <j>' to set square (i,j) white\n");
  printf("-press 'b <i> <j>' to set square (i,j) black\n");
  printf("-press 'e <i> <j>' to set square (i,j) empty\n");
  printf("-press 'z' to undo last move\n");
  printf("-press 'y' to redo last canceled move\n");
  printf("-press 'r' to restart\n");
  printf("-press 'q' to quit\n");
}

static bool is_legal(cgame g, uint i, uint j) { return (i < g->rows && j < g->cols); }

int main(int argc, char* argv[])
{
  game g;
  if (argc == 1) {
    g = game_random(4,4,false,FULL,false);//game_default();
  } else {
    g = game_load(argv[1]);
  }
  while (game_won(g) == false) {
    game_print(g);
    print_all_error(g);
    printf("> ? [h for help]\n");
    char letter;
    int first_arg = scanf(" %c", &letter);
    if (letter == 'h' && first_arg == 1) {
      print_help();
    } else if (letter == 's' && first_arg == 1) {
      char* file = NULL;
      int open_file = scanf("%ms", &file);
      if (file == NULL && open_file == 1) {
        printf("You have to give a filename to save the game");
      } else {
        game_save(g, file);
        free(file);
      }
    } else if (letter == 'r' && first_arg == 1) {
      game_restart(g);
    } else if (letter == 'q' && first_arg == 1) {
      game_delete(g);
      printf("Shame\n");
      exit(EXIT_SUCCESS);
    } else if (letter == 'z' && first_arg == 1) {
      game_undo(g);
    } else if (letter == 'y' && first_arg == 1) {
      game_redo(g);
    } else if ((letter == 'w' || 'b' || 'e') && first_arg == 1) {
      int column, row;
      int coords = scanf("%d %d", &column, &row);
      if (is_legal(g, column, row) && coords == 2) {
        game_play_move(g, column, row, convert_to_color(letter));
      }
    } else {
      printf("Please put a valid parameter...\n");
    }
  }
  game_print(g);
  printf("Congratulation\n");
  return EXIT_SUCCESS;
}