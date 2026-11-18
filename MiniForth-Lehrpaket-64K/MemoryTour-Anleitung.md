# Die MiniForth-Speicherlupe – Zellen beobachten statt nur Diagramme lesen

**Dateien:** `MemoryTour.java`, `MiniForth.java` und `MemoryTour.out.txt`.

Das kleine Programm `MemoryTour` verwendet die **Originalimplementierung** von MiniForth. Es verändert keine Konstante und keine Methode der VM. Weil die relevanten Felder und Methoden in `MiniForth.java` paketweit zugänglich sind, kann eine zweite Java-Klasse im selben Verzeichnis die Speicherzellen direkt untersuchen.

## Start

```bash
javac --release 8 MiniForth.java MemoryTour.java
java MemoryTour
```

Mit exakter Soll-Ausgabe vergleichen:

```bash
java MemoryTour > memorytour-ist.txt
# Vergleiche memorytour-ist.txt mit MemoryTour.out.txt
```

## Was kompiliert die Speicherlupe?

```text
class Base
    field x
    : read ( -- n ) x ;
end
class Child extends Base
    field y
    : sum ( -- n ) x y + ;
end
new Child
```

Die Quelle wird als Java-`String` an den normalen `Scanner` der VM gegeben. Anschließend fragt die Speicherlupe die Klassennamen, die Klassenadressen und die Objektadresse ab und liest die entsprechenden `mem[]`-Zellen aus.

## Wie die Adressen zusammenhängen

```text
128  Dictionary-Eintrag Base
        LINK 0 | KIND 1 | OWNER 0 | VALUE 8192 | LEN 4 | 'B','a','s','e'

8192  Base-Klassenblock (35 int-Zellen)
  8192 SUPER = 0
  8193 NFIELDS = 1
  8194 NMETHODS = 1
  8195 Selektor read = 0
  8196 Codeadresse read = 8227

8227  Methodenblock Base.read (9 Zellen)
       3 Kopfzellen + 3 Befehle × 2 Zellen

8236  Child-Klassenblock (35 Zellen)
  8236 SUPER = 8192
  8237 NFIELDS = 2
  8238 NMETHODS = 2
  8239 Selektor read = 0
  8240 geerbte Codeadresse = 8227
  8241 Selektor sum = 1
  8242 Codeadresse sum = 8271

8271  Methodenblock Child.sum (15 Zellen)
       3 Kopfzellen + 6 Befehle × 2 Zellen

8286  Child-Objekt (3 Zellen)
  8286 CLASS_PTR = 8236
  8287 Feld x = 0 (geerbt)
  8288 Feld y = 0 (neu)

65440 Hauptprogramm-Frame (96 Zellen)
  65443 DEPTH = 1
  65460 STACK[0] = 8286 (Objektadresse)
```

Alle angezeigten Werte sind mit dieser speziellen **64K-Referenzdatei** ermittelt. Wenn man zuvor andere Klassen oder globale Wörter definiert, verändern sich viele **absolute Adressen**, nicht aber die **Offsets** in einem Klassen-, Objekt- oder Frameblock.

## Fünf kleine Experimente

1. Ergänze `Base` um ein zweites Feld. Welche Offsets verschieben sich, und wie verändert sich `NFIELDS` in der Unterklasse?
2. Ändere in `Child` die Methode `read` statt `sum` zu überschreiben. Was passiert mit dem bereits geerbten `VTABLE`-Eintrag?
3. Erzeuge zwei `Child`-Objekte. Welche Adressen stehen anschließend auf dem Hauptprogramm-Stack? Worin unterscheiden sich die Objektblöcke?
4. Ergänze in einer Instanzmethode einen Parameter und `local temp`. Welche Veränderung ist bei einem **Aufruf** im neuen Frame zu beobachten?
5. Ändere in `MiniForth.java` *nur* `MAX_METHODS`, `MAX_FIELDS` oder `MAX_LOCALS` auf `24`, kompiliere neu und erkläre die abgeleiteten Klassen- und Framegrößen. **Achtung:** Die bisherigen *absoluten Adressen* sind danach nicht mehr die Sollwerte der 16er-Konfiguration.

## Java-Bezug

Das Beispiel veranschaulicht:

- **Referenzen:** Der Hauptstack enthält die Objektadresse `8286`, nicht die Kopie der Daten.
- **Vererbung:** `Child` besitzt den geerbten Feldoffset `x=0`; `y` folgt bei Offset `1`.
- **Polymorphie:** Die VTable enthält Selektor-/Codeadresspaare und kopiert geerbte Implementierungen.
- **Methodencode:** Die Methode `read` bleibt an derselben Adresse, selbst wenn sie in der Unterklasse geerbt wird.
- **Frame:** Ein Methodenaufruf würde `fp` um `96` verkleinern und neue lokale Slots erzeugen.

Die reale Java Virtual Machine ist komplizierter und muss ihre internen Objektlayouts nicht so verwenden. Der Reiz von MiniForth ist, dass das **Prinzip** hier vollständig sichtbar ist.

## Weitere Zellen aus der erweiterten Speicherlupe

Die Speicherlupe zeigt nun auch die **Zeichencodes im Dictionary**, den **Methodenkopf**, das **Disassembly** und die **Headerzellen des Hauptframes**:

```text
133  ->  66  Zeichen 'B'
134  ->  97  Zeichen 'a'
135  ->  115  Zeichen 's'
136  ->  101  Zeichen 'e'
Zeiger: dp=211 here=8289 ctrl=16 dictionary=203
Methode Base.read @8227
8227  ->  1  Methode NARGS (this)
8228  ->  1  Methode NRESULTS
8229  ->  3  Methode LENGTH
Bytecode von Base.read:
Base.read @8227:
  8230: LOAD 0
  8232: GETFIELD 0
  8234: RETURN 0
65440  ->  0  Frame main OLD_FP
65441  ->  0  Frame main RET_IP
65442  ->  0  Frame main FMETHOD
65443  ->  1  Frame main DEPTH
65444  ->  0  Frame main LOCAL0
65460  ->  8286  Frame main STACK0
```

`dp=211` bedeutet, dass die nächste freie Wörterbuchzelle `211` ist; `dictionary=203` verweist auf den zuletzt definierten **Selektor**. `here=8289` liegt unmittelbar *hinter* dem zuletzt angelegten dreizelligen Objekt `8286–8288`. Die Methode `Base.read` besitzt genau **drei Instruktionen**: `LOAD 0` (Objektadresse `this` lesen), `GETFIELD 0` (geerbten Feldwert lesen), `RETURN 0` (einen Rückgabewert an den Aufrufer übertragen). Der Hauptframe hat noch keinen aktiven Methodenkopf (`FMETHOD=0`), aber bereits einen Stackwert (`DEPTH=1`).
