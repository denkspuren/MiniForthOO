# Ein Unterrichtsrundgang durch den MiniForth Explorer

Die fertige Vererbungsdemo ist bereits in `viewer.html` enthalten.

## 1. Der Anfang: noch kein Objekt

Auf `⏮` klicken. Der Speicher enthält zunächst kein Dictionary und keine
Klassen. Der erste Frame ist der Hauptframe ab Adresse **65440**.
Frage: Welche Zellen gehören zu `OLD_FP`, `RET_IP`, `FMETHOD`, `DEPTH`?

## 2. Das Dictionary wächst

Die ersten Schritte durchblättern. Bei `class Animal` entsteht ein
Dictionaryeintrag für `Animal` und eine Klassenbeschreibung im Heap.
`dp` und `here` steigen getrennt. Im Bereich **Dictionary** sieht man
`LINK`, `KIND`, `OWNER`, `VALUE`, `LEN` und die Zeichen des Namens.

## 3. Klassen und ihre Tabellen

Das Panel **Klassen und VTables** zeigt die virtuelle Methode `describe`, die
bei `Dog` überschrieben wird. Die Codeadresse der geerbten Methode `init`
bleibt unverändert. Trotzdem gehört das Objekt zu `Dog`.

## 4. Objektreferenzen und `this`

Bis zur ersten `NEW`-Instruktion springen (Timeline oder Zeitregler).
Vergleiche den Stackwert nach `NEW` mit der Objektadresse rechts.
Die erste Objektzelle ist der Klassenzeiger, die zweite das Feld `age`.
Beim Aufruf von `init` wandert diese Objektadresse in den Frame-Slot `this`.

## 5. Drei Arten der Bindung

Beim Aufruf `d .o` wird `toString` über die VTable der tatsächlichen Klasse
`Dog` gesucht. `Animal.toString` ruft dann wiederum virtuell `describe` auf:
die überschriebene Methode `Dog.describe` gewinnt. In `Dog.describe` ruft
`super describe` dagegen die fest gebundene Oberklassenmethode auf.

Die verschachtelten Aufrufe sind links im **Callstack** gleichzeitig sichtbar.
Die Rückkehr wird mit `RET_IP` nachvollziehbar.

## 6. Rückwärts gehen

Von einem Zustand mit `Animal` und `Dog` zum Zeitpunkt *vor* `NEW` gehen.
Das entsprechende Objekt verschwindet aus der Objektliste, und die
Speicherzellen erhalten ihre alten Werte. Das ist keine erneute Ausführung,
sondern die Anwendung der gespeicherten **alten** Werte.

## 7. Rekursion laden

`beispiele/03_rekursion.json` laden und zu einem tiefen `INVOKESTATIC`-
Ereignis gehen. Das Frame-Panel zeigt `fact` mehrfach aktiv, mit
verschiedenen `n`-Werten an unterschiedlichen Frameadressen. Danach per
Einzelschritt den Rückweg über `RETURN` verfolgen.

## 8. Fragen für Studierende

1. Warum wird `fp` bei einem Aufruf kleiner, `here` bei `new` aber größer?
2. Wo liegen `this`, der Methodenname, die Bytecodeadresse und die Feldwerte?
3. Warum braucht `Dog` eine eigene VTable, aber keine Kopie von `Animal.init`?
4. Was ist der Unterschied zwischen dem Quellwort `.o` und `PRINTS`?
5. Welche Informationen wären nötig, um zusätzlich die **exakte Quellzeile**
   jeder Bytecode-Instruktion zu markieren?
6. Weshalb reichen ein JSON-Mitschnitt und reversible Speicheränderungen aus,
   um rückwärts durch die Ausführung zu gehen?
