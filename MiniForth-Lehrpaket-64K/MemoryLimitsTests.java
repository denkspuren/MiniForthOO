import java.util.Scanner;
/** Interne Speicherprüfungen: Layout und dynamisches Binden bei 64K. */
public class MemoryLimitsTests {
    static int checks;
    static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
    static MiniForth compile(String source) {
        MiniForth vm = new MiniForth();
        vm.input = new Scanner(source);
        while (vm.input.hasNext()) vm.word(vm.next());
        vm.input.close();
        return vm;
    }
    public static void main(String[] args) {
        check(MiniForth.MEMORY == 65536, "64K Speicher");
        check(MiniForth.MAX_FIELDS == 16, "16 Felder");
        check(MiniForth.MAX_METHODS == 16, "16 Methoden");
        check(MiniForth.MAX_LOCALS == 16, "16 lokale Slots");
        check(MiniForth.STACK == MiniForth.LOCAL0 + MiniForth.MAX_LOCALS, "Stack beginnt nach Locals");
        check(MiniForth.FRAME - MiniForth.STACK == 76, "76 Stackzellen pro Frame");
        check(MiniForth.HEAP == 8192, "8K Code-Heap-Start");
        MiniForth vm = compile(
                "class Base " +
                "field a field b " +
                ": x ( -- n ) 11 ; " +
                ": y ( -- n ) 22 ; " +
                "end " +
                "class Child extends Base " +
                "field c " +
                ": x ( -- n ) 33 ; " +
                ": z ( -- n ) 44 ; " +
                "end " +
                "new Child send x ");
        int base = vm.klass("Base"), child = vm.klass("Child");
        check(base == MiniForth.HEAP, "Klasse am Beginn des Heaps");
        check(child > base, "Unterklasse nach Basisklasse");
        check(vm.mem[base + MiniForth.SUPER] == 0, "Base hat keine Oberklasse");
        check(vm.mem[child + MiniForth.SUPER] == base, "Oberklassenzeiger");
        check(vm.mem[base + MiniForth.NFIELDS] == 2, "zwei Felder bei Base");
        check(vm.mem[child + MiniForth.NFIELDS] == 3, "geerbte Feldnummern plus ein Feld");
        check(vm.mem[base + MiniForth.NMETHODS] == 2, "zwei VTable-Eintraege bei Base");
        check(vm.mem[child + MiniForth.NMETHODS] == 3, "drei VTable-Eintraege bei Child");
        int selectorX = vm.selector("x"), selectorY = vm.selector("y"), selectorZ = vm.selector("z");
        check(vm.virtual(base, selectorX) != vm.virtual(child, selectorX), "x ueberschrieben");
        check(vm.virtual(base, selectorY) == vm.virtual(child, selectorY), "y geerbt");
        check(vm.virtual(child, selectorZ) != 0, "z neu");
        check(vm.virtual(base, selectorZ) == 0, "z nicht in Base");
        check(vm.mem[vm.fp + MiniForth.DEPTH] == 1, "Ein Rueckgabewert im main-Stack");
        check(vm.mem[vm.fp + MiniForth.STACK] == 33, "Ueberschriebene Methode liefert 33");
        check(vm.fp == vm.mem.length - MiniForth.FRAME, "Nach RETURN wieder main-Frame");
        int e = vm.member("c", MiniForth.FIELD, child);
        check(vm.mem[e + MiniForth.VALUE] == 2, "Geerbte Felder vor neuen Feldern");
        check(vm.mem[child + MiniForth.VTABLE + MiniForth.ENTRY * MiniForth.MAX_METHODS - 1] == 0,
              "Freie Eintraege anfangs null");
        System.out.println("Interne Speicherprüfungen: " + checks + "/" + checks + " bestanden");
    }
}
