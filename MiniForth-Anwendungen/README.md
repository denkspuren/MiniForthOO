# MiniForth-Anwendungen

Neun Beispielprogramme für die 64K-Fassung von MiniForth (mit `local`, ohne Stack-Shuffler):
Liste, Ausdrucksbaum, Division, Brüche, Trigonometrie, Turtle-Grafik als SVG und Nim nach `nim.java`.
`MiniForth.java` ist unverändert aus dem 64K-Paket übernommen.

```sh
javac -encoding UTF-8 MiniForth.java
java MiniForth beispiele/01-liste.mf
java MiniForth beispiele/06-turtle.mf > turtle.svg
cat beispiele/08-nim-negamax.mf - | java MiniForth     # danach selbst Nim spielen
python3 run_tests.py                                   # alle Tests (Java 8+, Python 3; jshell optional)
```

Der Rundgang durch alle Beispiele steht in **`MiniForth-Anwendungen.md`**.

| Ordner/Datei | Inhalt |
|---|---|
| `beispiele/` | `01-liste.mf` … `09-nim-vergleich.mf`, jeweils mit Sollausgabe `*.out.txt` bzw. `06-turtle.out.svg` |
| `bibliothek/` | `arith`, `trig`, `turtle`, `map`, `nim`, `nim-xor`, `nim-negamax`; wörtlich in die Beispiele eingebunden |
| `einbinden.py` | überträgt Änderungen an `bibliothek/` in die Beispiele und Tests (`--check` prüft nur) |
| `tests/` | Testprogramme für Division und Trigonometrie, Vergleich mit `nim.java` |
| `run_tests.py` | 19 Prüfungen; `--write` schreibt die Sollausgaben neu |
| `TEST-RESULTS.txt` | Protokoll des letzten Testlaufs |
