# MiniForth Explorer – die laufende VM im Browser beobachten

**Eine vollständig lokale, didaktische Speicherlupe mit Zeitreise.** Die mitgelieferte
`MiniForth.java` stammt unverändert aus der konfigurierbaren **64K-Fassung**;
auch das beobachtete `.mf`-Programm wird weder umgeschrieben noch mit `trace`
oder neuen Instruktionen versehen.

## Schnellstart auf macOS

Voraussetzung: **Java Development Kit (JDK) ab Version 8**, inklusive `javac`.
Java allein ohne Compiler reicht nicht. Kein Maven, keine Python-Pakete und
kein Webserver werden benötigt.

```bash
cd MiniForth-Explorer
chmod +x run.sh           # einmalig, falls beim Entpacken nötig
./run.sh                  # kompiliert, zeichnet Vererbungsdemo auf, öffnet Viewer (Mac)
```

Die Datei `viewer.html` kann auch einfach per Doppelklick im Browser geöffnet
werden. Eine passende Vererbungsdemo ist **bereits in die HTML-Seite eingebettet**:
Du kannst sofort auf **▶**, **◀** und den Zeitregler klicken.

Eigenes Programm unverändert aufzeichnen:

```bash
./run.sh mein-programm.mf mein-mitschnitt.json
```

Anschließend `viewer.html` öffnen und über **Aufzeichnung laden** die Datei
`mein-mitschnitt.json` auswählen – alternativ JSON auf die Seite ziehen.
Mit `./run.sh --memory` läuft die frühere `MemoryTour.java`.

## Was man sieht

| Ansicht | Dekodierte Informationen |
|---|---|
| Aktueller Schritt | Originalopcode, Operand, absolute IP-Adresse, Quellwort, `fp`, `here`, `dp` |
| Zeitreise | Einzelschritt vor/zurück, beliebiger Zeitsprung, automatische Wiedergabe |
| Frames | Jeder aktive Frame mit Startadresse, `OLD_FP`, `RET_IP`, `FMETHOD`, benannten Locals, Operandenstack und Zelladressen |
| Bytecode | Aktive Methode, Opcodepaare, Methodenname, nächster IP und zuletzt ausgeführte Instruktion |
| Rohspeicher | Beliebige `int[]`-Adressen, Zellwerte, zugehörige Klasse/Methode/Feld/Dictionary/Frame, markierte Änderungen |
| Objekte | Tatsächliche Heapadressen aller bisher mit `NEW` angelegten Instanzen, Klassenzeiger, geerbte und eigene Felder |
| Klassen und VTables | Oberklasse, `NFIELDS`, `NMETHODS`, Selektoren, Codeadressen, überschriebene Methoden |
| Dictionary | Name, Art, Adresse und Wert der Symboltabelleneinträge |
| String-Pool | Internierte Zeichenketten mit Poolindex |
| Ausgabe | **Nur** die bis zum ausgewählten Zeitpunkt erzeugten Ausgabezeilen |
| Quelle | Das originale `.mf`-Programm, unverändert aus der Aufzeichnung |

**Tastatur:** `←` einen Schritt zurück, `→` vor, `Pos1` zum Anfang,
`Ende` ans Ende, Leertaste Wiedergabe/Pause (solange kein Eingabefeld aktiv ist).
Blaue Speicheradressen sind anklickbar und führen zur entsprechenden Zelle.

## Warum ein Recorder statt eines Servers?

`TraceRecorder` ist eine **externe Java-Unterklasse** der Originalmaschine.
Sie überschreibt ausschließlich die Methode `run()`, damit nach jedem Opcode
alle geänderten Speicherzellen erfasst werden. Diese eine Interpreter-Schleife
ist dafür **semantisch aus der Originalfassung gespiegelt**; `word()`,
`invoke()`, `ret()`, `push()`, `pop()`, der Compiler, das Dictionary, die
Klassenverwaltung und die Objektverwaltung arbeiten unverändert in der
Originalklasse. Beim Einlesen werden auch Compiler-/Dictionaryänderungen
aufgezeichnet.

Die Originaldatei wird **weder editiert noch gepatcht**. Das mitgelieferte
`.mf`-Programm wird als UTF-8 gelesen und unverändert an den Originalscanner
übergeben. Der Recorder fängt lediglich die normale Programmausgabe auf.
Der beobachtete Bytecode wird nicht mit zusätzlichen Befehlen versehen.

Die Aufzeichnung ist eine **gewöhnliche JSON-Datei**. Jedes Ereignis enthält
`[Adresse, alter Wert, neuer Wert]` für alle geänderten `int[]`-Zellen, den
vollständigen Satz der Maschinenzeiger, neue String-Pool-Einträge,
Objekterzeugungen und zusätzliche Ausgabetexte. Die Änderungen sind deshalb
**reversibel**: Der Browser kann beliebig vor und zurück springen, ohne die VM
neu zu starten. Der Ausgangsquelltext und sein SHA-256-Hash stehen im Trace.

**Datei-Zuschnitt:**

```text
MiniForth-Explorer/
├── MiniForth.java          Originalmaschine, unverändert
├── TraceRecorder.java      externe Unterklasse + JSON-Recorder
├── MemoryTour.java         ursprüngliche Speicherlupe (Terminal)
├── TraceRecorderSelfTest.java   vergleicht alle 65536 Speicherzellen
├── viewer.html             vollständiger Offline-Browser-Viewer
├── run.sh                  Mac-/Linux-Startskript
├── beispiele/
│   ├── 01_counter.mf       Objektzustand, locals, .o
│   ├── 02_vererbung.mf     Vererbung, super, virtueller Dispatch
│   ├── 03_rekursion.mf     rekursive Frames
│   └── *.json              fertige Mitschnitte + *.out.txt
└── tests/
    └── test_recordings.py zusätzlicher Python-Vergleich (optional)
```

## Grenzen, die für den Unterricht wichtig sind

- **Kein Live-Einzelschritt:** Die Ausführung wird zunächst aufgenommen, danach
  im Browser beliebig vor und zurück betrachtet. Das macht den Viewer offline
  und betriebssystemunabhängig.
- **Quellzeilen nicht zuverlässig markierbar:** Der Originalscanner behält
  Quellzeilen-/Spaltennummern nicht. Der Viewer zeigt das letzte verarbeitete
  Compilerwort, den echten Bytecode, die Methode und das gesamte Quellprogramm.
  Ein erfundenes Mapping auf Quellzeilen wäre didaktisch irreführend.
- **Keine Laufzeittypen:** Ein `int` kann gleichzeitig Wert, Objektadresse
  oder Stringindex sein. Der Viewer zeigt eindeutige Objektadressen aus
  `NEW` und kennzeichnet Interpretationen von Stringindizes als *möglich*.
- **Unbenutzte Heap-Zellen sind nicht alle Objekte:** Objekte werden nur dann
  als solche gezeigt, wenn sie bei einem tatsächlichen `NEW` beobachtet wurden.
- **Aufzeichnungsgrenze:** Standardmäßig maximal **12.000 Ereignisse**, um
  gigantische Aufnahmen zu vermeiden. Das lässt sich mit dem dritten
  CLI-Argument erhöhen, höchstens auf 200.000. Bei Erreichen wird die Aufnahme
  bewusst als **begrenzt** markiert. In der HTML-Seite werden maximal die
  jüngsten 30 Objekte gleichzeitig als Karten gezeigt; die übrigen bleiben in
  der Aufzeichnung und im Rohspeicher erhalten.
- **Semantik muss mit der Basis-Version synchron bleiben:** Weil der Recorder
  eine instrumentierte Kopie von `run()` benutzt, müssen künftige Änderungen
  an den Opcodes beider Dateien abgeglichen werden. Das Testskript vergleicht
  dafür echte Programmausgaben, statt semantische Identität bloß zu behaupten.
- **Datenschutz:** JSON enthält den vollständigen Quelltext, Strings und
  Speicherwerte. Aufzeichnungen vertraulicher Programme nicht weitergeben.
  Der Viewer lädt nichts aus dem Internet.

## Tests

```bash
./run.sh --test
```

Der **Java-Selbsttest** vergleicht die drei beigelegten Programme mit der
unveränderten `MiniForth.java`: **alle 65536 int-Zellen**, sämtliche
Maschinenzeiger, den kompletten String-Pool und die Programmausgabe. Die
optionale Python-Variante (`tests/test_recordings.py`) prüft zusätzlich die
SHA-256-Werte der Quelldateien in den Mitschnitten.
Die HTML-Steuerung wurde außerdem im Chromium-Browser auf Vorwärts-/Rückwärts-
Navigation und die Darstellung rekursiver Frames getestet.
