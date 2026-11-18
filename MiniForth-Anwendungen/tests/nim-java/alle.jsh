/open nim.java
for (int a = 0; a < 8; a++) for (int b = 0; b < 8; b++) for (int c = 0; c < 8; c++) for (int d = 0; d < 8; d++) {
    int[] x = {a, b, c, d};
    if (!isValid(x) || gameOver(x)) continue;
    int[] m = bestMove(x);
    int v = negamax(new int[1 << 12], x);
    System.out.println(a + " " + b + " " + c + " " + d + " " + m[0] + " " + m[1] + " " + v + " " + key(x));
}
/exit
