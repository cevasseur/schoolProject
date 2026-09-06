##README

**Mosaic**

**The game mosaic is a single-player logic puzzle.**

Rules
According to the Simon Tatham's Portable Puzzle Collection:

‍Given a grid of squares, you must color it either black or white. Some squares contain clue numbers. Each clue tells you the number of black squares in the 3×3 region surrounding the clue - including the clue square itself.

So, to play this game, you have to choose the white or black color of each square to fill in all the grid. For example, if an empty cell contains the number 0, then the 3x3 neighbourhood must be white. Similarly, if a square contains the number 9, the 3x3 neighbourhood will be black.

Moreover, we will limit our game to a square grid of size 5x5.

You can try this game here.

By convention, the square in the i-th row and j-th column of the grid is referred to as the coordinate (i, j), and the coordinate (0, 0) corresponds to the top left corner (like in matrices).

In summary, the game uses the following squares, that can be printed in a terminal:

empty squares: 0 1 2 3 4 5 6 7 8 9 and ' ' if unconstrained

white squares: 🄋 ➀ ➁ ➂ ➃ ➄ ➅ ➆ ➇ ➈ and □ if unconstrained

black squares: 🄌 ➊ ➋ ➌ ➍ ➎ ➏ ➐ ➑ ➒ and ■ if unconstrained

In our graphical interface, you can click on a square to set it to a certain colour. The colours rotate as follows: EMPTY,WHITE,BLACK. If the constraint is satisfied, the number of the constraint will turn green. Conversely, if the number of black squares is greater than the constraint, the number will turn red. It is possible to undo by pressing z, redo by pressing y, save by pressing s, solve by pressing w and restart by pressing r. Saving a game creates a game.txt file that will contain the game. It is also possible to load a game by setting a file as a parameter during the executable. 

*Authors : Lukas Soetens, Cengiz Vasseur, Milan Théodore--Delage.*