#ifndef __GAME_STRUCT_H__
#define __GAME_STRUCT_H__

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "game.h"
#include "game_ext.h"
#include "queue.h"

struct game_s {
  uint size;
  uint rows;
  uint cols;
  constraint* constraints;  // Tableau de contraintes
  color* colors;            // Tableau de couleurs
  bool wrapping;            // Torique ou non
  neighbourhood neigh;      // Type de voisinage
  queue* moves;
  queue* canceled_moves;
};

#endif