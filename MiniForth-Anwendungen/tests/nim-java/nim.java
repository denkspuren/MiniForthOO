// Nim: interactive JShell source for the course exercises.
// Standard Nim: each move removes at least one stick from exactly one row.
// Taking the last stick wins. Rows are addressed with zero-based indices.
// At most 16 sticks in total, at most 7 per row, at most 4 non-empty rows.
// A board may contain additional empty rows.

// start snippet numberOfSticks
// Count all sticks still on the board.
int numberOfSticks(int[] board) {
    int sum = 0;
    for (int row = 0; row < board.length; row++)
        sum += board[row];
    return sum;
}
// end snippet numberOfSticks

// start snippet isValid
// Check all restrictions before starting a game.
boolean isValid(int[] board) {
    if (board == null || board.length <= 0)
        return false;
    int nonEmptyRows = 0;
    for (int row = 0; row < board.length; row++) {
        if (board[row] < 0 || board[row] > 7)
            return false;
        if (board[row] > 0)
            nonEmptyRows++;
    }
    return numberOfSticks(board) <= 16 && nonEmptyRows <= 4;
}
// end snippet isValid

// start snippet init
// Varargs: callers may pass individual row lengths or an int[].
int[] init(int... rows) {
    assert isValid(rows) : "Invalid Nim board";
    return rows;
}
// end snippet init

// start snippet show
// Represent the board as a readable, multi-line String.
String show(int[] board) {
    String s = "";
    for (int row = 0; row < board.length; row++) {
        s += row + ":";
        int sticks = board[row];
        while (sticks-- > 0)
            s += " I";
        if (row < board.length - 1)
            s += "\n";
    }
    return s;
}
// end snippet show

// start snippet move_triple
// Apply a legal move directly to the given board.
int[] move(int[] board, int row, int sticksToRemove) {
    assert row >= 0 && row < board.length;
    assert sticksToRemove > 0 && sticksToRemove <= board[row];
    board[row] -= sticksToRemove;
    return board;
}
// end snippet move_triple

// start snippet move_pair
// Overload: represent a move as {row, numberOfSticks}.
int[] move(int[] board, int[] move) {
    assert move != null && move.length == 2;
    return move(board, move[0], move[1]);
}
// end snippet move_pair

// start snippet undo_triple
// Undo the most recently tried move using the same row and count.
int[] undo(int[] board, int row, int sticksToUndo) {
    assert row >= 0 && row < board.length && sticksToUndo > 0;
    board[row] += sticksToUndo;
    return board;
}
// end snippet undo_triple

// start snippet undo_pair
// Overload: undo a move represented by a two-element array.
int[] undo(int[] board, int[] move) {
    assert move != null && move.length == 2;
    return undo(board, move[0], move[1]);
}
// end snippet undo_pair

// start snippet moves
// Enumerate all legal moves as pairs {row, numberOfSticks}.
int[][] moves(int[] board) {
    int sum = numberOfSticks(board);
    int[][] moves = new int[sum][2];
    int index = 0;
    for (int row = 0; row < board.length; row++)
        for (int sticks = board[row]; sticks >= 1; sticks--)
            moves[index++] = new int[]{row, sticks};
    return moves;
}
// end snippet moves

// start snippet gameOver
// Return true if no sticks remain.
boolean gameOver(int[] board) {
    for (int row = 0; row < board.length; row++)
        if (board[row] > 0)
            return false;
    return true;
}
// end snippet gameOver

// start snippet copy
// Make an independent copy of a board before sorting it.
int[] copy(int[] array) {
    int[] copy = new int[array.length];
    for (int i = 0; i < array.length; i++)
        copy[i] = array[i];
    return copy;
}
// end snippet copy

// start snippet insertionSort
// Sort row lengths in place.
void insertionSort(int[] array) { 
    for (int i = 1; i < array.length; i++) {
        int key = array[i];
        int j = i - 1;
        // Shift larger elements one position to the right.
        while (j >= 0 && array[j] > key) {
            array[j + 1] = array[j];
            j--;
        }
        array[j + 1] = key;
    }
}
// end snippet insertionSort

// start snippet key
// Encode sorted, non-empty rows with three bits per row.
int key(int[] unchangedBoard) {
    assert isValid(unchangedBoard) : "Invalid board";
    // Work on a copy to leave the original board unchanged.
    int[] board = copy(unchangedBoard);
    // Sort so equivalent positions produce the same key.
    insertionSort(board);
    final int shift = 3;
    int key = 0;
    for (int row = 0; row < board.length; row++) {
        if (board[row] == 0)
            continue;
        key = (key << shift) | board[row]; 
    }
    return key;
}
// end snippet key

// start snippet negamax
// Evaluate a position for the player who is about to move.
int negamax(int[] cache, int[] board) {
    if (gameOver(board))
        return -1; // The previous player took the last stick and won.
    int id = key(board);
    if (cache[id] != 0)
        return cache[id];
    int[][] legalMoves = moves(board);
    for (int i = 0; i < legalMoves.length; i++) {
        int row = legalMoves[i][0], sticks = legalMoves[i][1];
        move(board, row, sticks);
        int value = -negamax(cache, board);
        undo(board, row, sticks);
        if (value == 1)
            return cache[id] = 1;
    }
    return cache[id] = -1;
}
// end snippet negamax

// start snippet bestMove
// Prefer a move that forces a win; otherwise choose the first move.
int[] bestMove(int[] board) {
    int[][] legalMoves = moves(board);
    if (legalMoves.length == 0)
        return null;
    // Create one empty cache shared by all candidate moves.
    int[] cache = new int[1 << 12]; // cache size is 4096
    for (int i = 0; i < legalMoves.length; i++) {
        int row = legalMoves[i][0], sticks = legalMoves[i][1];
        move(board, row, sticks);
        int value = -negamax(cache, board);
        undo(board, row, sticks);
        if (value == 1)
            return legalMoves[i];
    }
    return legalMoves[0]; // No move can force a win against perfect play.
    // Alternatively, make a random choice.
    // return legalMoves[java.util.concurrent.ThreadLocalRandom.current().nextInt(legalMoves.length)]; 
}
// end snippet bestMove

// start snippet play
// Execute and show one computer move.
int[] play(int[] board) {
    if (gameOver(board)) {
        System.out.println("Das Spiel ist bereits beendet.");
        return null;
    }
    int[] choice = bestMove(board);
    move(board, choice);
    System.out.println("Computer entfernt " + choice[1] + (choice[1] > 1 ? " Sticks" : " Stick") + " aus Reihe " + choice[0] + ".");
    System.out.println(show(board));
    if (gameOver(board))
        System.out.println("Computer gewinnt!");
    return choice;
}
// end snippet play

// start snippet help
// Recommend a move without changing the board.
int[] help(int[] board) {
    if (gameOver(board)) {
        System.out.println("Das Spiel ist bereits beendet.");
        return null;
    }
    int value = negamax(new int[1 << 12], board);
    if (value == 1) {
        System.out.println("Dies ist eine Gewinnposition.");
        int[] choice = bestMove(board);
        System.out.println("Vorschlag: Entferne " + choice[1] + (choice[1] > 1 ? " Sticks" : " Stick") + " aus Reihe " + choice[0] + ".");
        return choice;
    }
    System.out.println("Dies ist eine Verlustposition. Bei perfektem Gegenspiel gibt es keinen Gewinnzug.");
    int[][] legalMoves = moves(board);
    int[] choice = legalMoves[java.util.concurrent.ThreadLocalRandom.current().nextInt(legalMoves.length)];
    System.out.println("Vorschlag: Entferne " + choice[1] + (choice[1] > 1 ? " Sticks" : " Stick") + " aus Reihe " + choice[0] + ".");
    return choice;
}
// end snippet help

// start snippet demo
// Demo: load this file in JShell, then call methods interactively.
int[] board = init(2, 3, 4, 5);
System.out.println(show(board));
System.out.println("Try: System.out.println(show(move(board, 0, 2))); ");
System.out.println("Hint: help(board);");
System.out.println("Computer move: play(board);");
// end snippet demo
