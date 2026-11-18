# MiniForth Explorer – Prüfergebnisse

**Referenz:** unveränderte konfigurierbare 64K-MiniForth.java
(SHA-256 siehe `sha256sum MiniForth.java` oder `shasum -a 256 MiniForth.java`).

## Java-Selbsttest

`./run.sh --test` führt drei Programme mit Originalklasse und externer
Recorder-Unterklasse aus. Verglichen werden **alle 65.536 int-Zellen**, alle
Maschinenzeiger, die 1.024 String-Pool-Einträge und die bytegenaue Ausgabe.

| Programm | Opcodes | Ergebnis |
|---|---:|---|
| Counter mit locals und `.o` | 42 | identisch |
| Vererbung mit VTable und `super` | 53 | identisch |
| Rekursive Fakultät mit vielen Frames | 65 | identisch |

**3 von 3 vollständig identisch.**

## Regressionsvergleich

Zusätzlich wurde die unveränderte aktuelle Referenz aus
`MiniForth-Lehrpaket-64K-mit-Speicherlupe.zip` verwendet. Von 78 positiven
Programmen (35 Lehrbeispiele und 43 positive Regressionstests) wurden:

- **77** vollständig mit dem Recorder ausgeführt und sowohl mit der
  Originalmaschine als auch mit der historischen Sollausgabe verglichen:
  **77/77 identisch**.
- **1** bewusst sehr großer Stressfall (40.000 Objektanlagen) durch die
  voreingestellte Begrenzung von 12.000 Aufzeichnungsereignissen frühzeitig
  abgebrochen: korrekter Status **„begrenzt“** im JSON, kein stiller Erfolg.
- **0** Abweichungen oder Fehler in den vollständig aufgezeichneten Programmen.

## Browser

In Chromium ohne Webserver geprüft:

- Eingebettete Vererbungsdemo ist sofort navigierbar.
- `vorwärts → ganz zurück → wieder nach vorn` stellt den **identischen
  65.536-Zellen-Speicher**, den String-Pool, die Objektliste und den
  Ausgabetext wieder her.
- JSON-Upload der Rekursionsdemo funktioniert.
- Im tiefen Aufruf werden **5 Frames** gleichzeitig dekodiert.
- Kein JavaScript-Ausführungsfehler im geprüften Durchlauf.

## Fehler- und Begrenzungsfälle

Ein Programm mit einem absichtlich unbekannten Wort erzeugte eine gültige
Aufzeichnung mit Fehlermeldung und bis dahin entstandener Programmausgabe.
Eine absichtlich auf 10 Ereignisse begrenzte Aufnahme erzeugte einen gültigen
JSON-Mitschnitt mit `truncated: true`.

**Wichtige Einordnung:** Da die externe Unterklasse die Interpreter-Schleife
`run()` spiegelt, bestätigen Tests die Übereinstimmung auf geprüften
Programmen; sie sind kein formaler Beweis für alle möglichen Programme.
