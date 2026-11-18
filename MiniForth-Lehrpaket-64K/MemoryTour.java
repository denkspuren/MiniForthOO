import java.util.Scanner;
/** Kleinste 'Speicherlupe' fuer die Vorlesung – keine Aenderung an MiniForth. */
public class MemoryTour {
    static void printCell(MiniForth vm, int address, String meaning) {
        System.out.println(address + "  ->  " + vm.mem[address] + "  " + meaning);
    }
    public static void main(String[] args) {
        MiniForth vm = new MiniForth();
        vm.input = new Scanner("class Base field x : read ( -- n ) x ; end " +
              "class Child extends Base field y : sum ( -- n ) x y + ; end new Child");
        while (vm.input.hasNext()) vm.word(vm.next());
        vm.input.close();
        int base = vm.klass("Base"), child = vm.klass("Child");
        int o = vm.mem[vm.fp + MiniForth.STACK];
        System.out.println("MEMORY=" + MiniForth.MEMORY + " DICT=" + MiniForth.DICT +
            " HEAP=" + MiniForth.HEAP + " FRAME=" + MiniForth.FRAME +
            " STACK=" + MiniForth.STACK + " fp=" + vm.fp);
        System.out.println("Klassen: Base=" + base + " Child=" + child);
        printCell(vm, base + MiniForth.SUPER, "Base.SUPER");
        printCell(vm, base + MiniForth.NFIELDS, "Base.NFIELDS");
        printCell(vm, base + MiniForth.NMETHODS, "Base.NMETHODS");
        printCell(vm, base + MiniForth.VTABLE, "Base.selector[0]");
        printCell(vm, base + MiniForth.VTABLE + 1, "Base.code[0]");
        printCell(vm, child + MiniForth.SUPER, "Child.SUPER");
        printCell(vm, child + MiniForth.NFIELDS, "Child.NFIELDS");
        printCell(vm, child + MiniForth.NMETHODS, "Child.NMETHODS");
        printCell(vm, child + MiniForth.VTABLE, "Child.selector[0]");
        printCell(vm, child + MiniForth.VTABLE + 1, "Child.code[0]");
        printCell(vm, child + MiniForth.VTABLE + 2, "Child.selector[1]");
        printCell(vm, child + MiniForth.VTABLE + 3, "Child.code[1]");
        System.out.println("Objektadresse=" + o);
        printCell(vm, o, "Objekt: Klassenzeiger");
        printCell(vm, o + 1, "Objekt: geerbtes x");
        printCell(vm, o + 2, "Objekt: eigenes y");
        System.out.println("Frame main=" + vm.fp + " depth=" + vm.mem[vm.fp+MiniForth.DEPTH]);
        System.out.println("Frame.Stack[0]=" + vm.mem[vm.fp+MiniForth.STACK]);
        int entry=vm.find("Base", MiniForth.CLASS,0);
        System.out.println("Dictionary.Base.Entry=" + entry + " Groesse=" +
            (MiniForth.CHARS + vm.mem[entry+MiniForth.LEN]));
        printCell(vm, entry+MiniForth.LINK, "Dictionary.Base.LINK");
        printCell(vm, entry+MiniForth.KIND, "Dictionary.Base.KIND");
        printCell(vm, entry+MiniForth.OWNER, "Dictionary.Base.OWNER");
        printCell(vm, entry+MiniForth.VALUE, "Dictionary.Base.VALUE");
        printCell(vm, entry+MiniForth.LEN, "Dictionary.Base.LEN");
        System.out.println("Dictionary.Base.Zeichen:");
        for (int i=0; i<vm.mem[entry+MiniForth.LEN]; i++)
            printCell(vm, entry+MiniForth.CHARS+i, "Zeichen '"+
                      (char)vm.mem[entry+MiniForth.CHARS+i]+"'");
        System.out.println("Zeiger: dp="+vm.dp+" here="+vm.here+
                           " ctrl="+vm.ctrl+" dictionary="+vm.dictionary);
        int method=vm.virtual(base,vm.selector("read"));
        System.out.println("Methode Base.read @"+method);
        printCell(vm, method+MiniForth.NARGS, "Methode NARGS (this)");
        printCell(vm, method+MiniForth.NRESULTS, "Methode NRESULTS");
        printCell(vm, method+MiniForth.LENGTH, "Methode LENGTH");
        System.out.println("Bytecode von Base.read:");
        vm.see(method);
        printCell(vm, vm.fp+MiniForth.OLD_FP, "Frame main OLD_FP");
        printCell(vm, vm.fp+MiniForth.RET_IP, "Frame main RET_IP");
        printCell(vm, vm.fp+MiniForth.FMETHOD, "Frame main FMETHOD");
        printCell(vm, vm.fp+MiniForth.DEPTH, "Frame main DEPTH");
        printCell(vm, vm.fp+MiniForth.LOCAL0, "Frame main LOCAL0");
        printCell(vm, vm.fp+MiniForth.STACK, "Frame main STACK0");
    }
}
