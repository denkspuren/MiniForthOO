# MiniForth 64K – vollständig überarbeitetes Lehrskript und Speicherlupe

Dieses Lehrpaket erklärt die **unveränderte aktuelle `MiniForth.java` mit 300 Zeilen** von den Konstanten der ersten 30 Zeilen über alle wichtigen Java-Methoden bis zum vollständigen Interpreter `run()` am Schluss.

## Zentrale Dateien

- `MiniForth-Lehrskript-64K.qmd`: komplettes, **bearbeitbares Quarto-Lehrskript** mit 8 Teilen, Speicherlayouts, Methodenkatalog, Java-Vergleichen, 35 geprüften Beispielen und Übungen.
- `MiniForth.java`: die **genau zum Skript gehörende** Java-8-Referenzmaschine mit `int[65536]`, je 16 Methodeneinträgen, Instanzfeldern und lokalen Slots sowie String-Interning.
- `MemoryTour.java`: **Speicherlupe**, die die `mem[]`-Zellen der echten VM inspiziert, ohne sie zu verändern.
- `MemoryTour.out.txt`: bytegenau geprüfte Ausgabe der Speicherlupe.
- `MemoryTour-Anleitung.md`: umfangreiche Adressrechnung und fünf Experimente mit der Speicherlupe.
- `beispiele/`: **35** unabhängige MiniForth-Programme und zu jedem ein `.out.txt` mit verifizierter Ausgabe.
- `verify_examples.py`: prüft 35 Lehrbeispiele, die Speicherlupe und 24 interne Layoutassertions.
- `tests/` und `run_tests.py`: große bestehende Regressionstests: 43 positive, 23 negative Programme.
- `MemoryLimitsTests.java`: 24 interne Assertions zu Speicheraufbau, Feldern, VTables, Aufruf-Frames.
- `verify_configurable.py`: Zusatzprüfung mit den Grenzen `MAX_METHODS=24`, `MAX_FIELDS=24`, `MAX_LOCALS=24` und 20 tatsächlich verwendeten Einträgen.

## Schnellstart

```bash
javac --release 8 MiniForth.java MemoryTour.java MemoryLimitsTests.java
java MemoryTour
java MiniForth beispiele/02_square_bytecode.mf
java MiniForth beispiele/17_dispatch_virtual.mf
python3 verify_examples.py
python3 run_tests.py
```

`python3` ist für Studierende **nicht erforderlich**. Der Interpreter verwendet ausschließlich Java 8.

## Quarto

Mit installiertem Quarto:

```bash
quarto render MiniForth-Lehrskript-64K.qmd --to html
```

Die `.qmd` ist vollständig editierbarer Markdown-Quelltext. Die Quarto-CLI war bei der Erstellung nicht installiert; ein vollständiges Quarto-Rendering konnte deshalb nicht überprüft werden. Markdown-Syntax und alle lokalen Links wurden separat geprüft.

## Wichtige didaktische Hinweise

1. Die Quellsprache kennt **benannte Parameter und `local`**, keine `dup`-, `drop`- oder `swap`-Primitive.
2. Der **interne Operandenstack** existiert weiter, aber als Bestandteil jedes Frames.
3. Die VTables enthalten maximal **16 Selektor-/Codepaare pro Klasse**, nicht global.
4. Der Dictionary-Bereich reicht von **128 bis 8191**, der Heap beginnt bei **8192**.
5. Der erste Hauptframe beginnt bei **65440** und belegt bis 65535.
6. Die Zeichenketten liegen als einzige Ausnahme in einem separaten `String[1024]`.
7. Angaben über **absolute** Speicheradressen gelten genau für die mitgelieferte VM und die isolierten Beispiele.

Der Foliensatz zur älteren MiniForth-Fassung ist in Details **nicht** mehr aktuell. Dieses Skript ist auf die neue Version abgestimmt.
