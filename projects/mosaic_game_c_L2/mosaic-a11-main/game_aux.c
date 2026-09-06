#include "game_aux.h"

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "game.h"
#include "game_ext.h"
#include "game_struct.h"
#include <assert.h>

void print_first2lines(cgame g)
{
  // Fonction qui affiche les deux premieres lignes qui delimite la game
  printf("   ");
  for (int number = 0; number < game_nb_cols(g); number++) {
    printf("%d ", number);
  }
  printf("\n   ");
  for (int hyphen = 0; hyphen < game_nb_cols(g); hyphen++) {
    printf("--");
  }
  printf("\n");
}

void print_core_game(cgame g, char** tab_char)
{
  // Fonction qui affiche le coeur du jeu (nombre gauche, bordure gauche, les nombres et les carrés de couleurs et enfin
  // bordure droite)
  for (int rows = 0; rows < game_nb_rows(g); rows++) {
    printf("%d |", rows);  // Affichage de la bordure gauche
    for (int cols = 0; cols < game_nb_cols(g); cols++) {
      constraint cst = game_get_constraint(g, rows, cols);
      color clr = game_get_color(g, rows, cols);
      // Si la case possède un contrainte
      if (cst != -1) {
        // On affiche le carractère de la bonne couleur si une couleur lui à été attribué
        if (clr != EMPTY) {
          printf("%s ", tab_char[cst + 10 * (2 - clr)]);
        } else {  // Sinon on affiche juste le nombre de contrainte de la case
          printf("%d ", cst);
        }
        // Si la case n'a pas de contrainte si une couleur lui à été attribué alors on affiche un carré de la bonne
        // couleur sinon on affiche des espaces
      } else {
        printf("%s ", tab_char[20 + clr]);
      }
      // Affichage de la bordure droite du jeu
      if (cols == game_nb_cols(g) - 1) {
        printf("|");
      }
    }
    printf("\n");
  }
}

void game_print(cgame g)
{
  char* caractere[23] = {"🄌", "➊", "➋", "➌", "➍", "➎", "➏", "➐", "➑", "➒", "🄋", "➀",
                         "➁",    "➂", "➃", "➄", "➅", "➆", "➇", "➈", " ", "□", "■"};
  print_first2lines(g);
  print_core_game(g, caractere);
  // Affichage de la bordure basse du jeu
  printf("   ");
  for (int i = 0; i < game_nb_cols(g); i++) {
    printf("--");
  }
  printf(" \n");
}

game game_default(void)  // Création d'une nouvelle partie par défaut avec des contraintes et des couleurs prédéfinies
{
  // Tableaux de contraintes et de couleurs pour une configuration par défaut
  constraint constraints[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                         -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color colors[DEFAULT_SIZE * DEFAULT_SIZE] = {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
                                               EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};

  // Création d'une nouvelle partie avec les contraintes et les couleurs définies ci-dessus
  game g = game_new(constraints, colors);
  return g;
}

game game_default_solution(void)
{
  // Retourne la solution de la partie par défaut
  constraint cons[DEFAULT_SIZE * DEFAULT_SIZE] = {0,  -1, -1, 3,  -1, -1, 5,  -1, -1, -1, -1, -1, 4,
                                                  -1, 1,  6,  -1, 6,  3,  -1, -1, -1, -1, -1, -1};
  color col[DEFAULT_SIZE * DEFAULT_SIZE] = {WHITE, WHITE, BLACK, WHITE, WHITE, WHITE, WHITE, BLACK, WHITE,
                                            BLACK, BLACK, BLACK, BLACK, WHITE, WHITE, BLACK, BLACK, WHITE,
                                            WHITE, WHITE, BLACK, BLACK, BLACK, BLACK, WHITE};
  game g_default_solution = game_new(cons, col);
  return g_default_solution;
}

float randomFloat(float min, float max) {
    return min + ((float)rand() / (float)RAND_MAX) * (max - min);
}

game game_random(uint nb_rows, uint nb_cols, bool wrapping, neighbourhood neigh,
                 bool with_solution)
{
  srand(time(NULL));
  float black_rate=randomFloat(0.0,1.0);
  float constraint_rate=randomFloat(0.0,1.0);
  assert(black_rate >= 0.0f && black_rate <= 1.0f);
  assert(constraint_rate >= 0.0f && constraint_rate <= 1.0f);
  game g = game_new_empty_ext(nb_rows, nb_cols, wrapping, neigh);
  assert(g);

  // fill the grid with random colors
  for (uint i = 0; i < nb_rows; i++)
  {
    for (uint j = 0; j < nb_cols; j++)
    {
      color c = (rand() < black_rate * (float)RAND_MAX) ? BLACK : WHITE;
      game_set_color(g, i, j, c);
    }
  }

  // fill the grid with actual constraint at random positions
  uint nb_squares = nb_rows * nb_cols;
  uint nb_constraints = constraint_rate * nb_squares;
  for (uint i = 0; i < nb_constraints; i++)
  {
    uint row = rand() % nb_rows;
    uint col = rand() % nb_cols;
    int nb_blacks = game_nb_neighbors(g, row, col, BLACK);
    game_set_constraint(g, row, col, nb_blacks);
  }

  // check solution
  if (!game_won(g))
  {
    game_delete(g);
    return NULL;
  }

  if (!with_solution)
    game_restart(g);
  return g;
}
