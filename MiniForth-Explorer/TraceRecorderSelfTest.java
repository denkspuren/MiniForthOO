import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Compares ALL 65536 VM cells, registers, String table and output after running a .mf. */
public class TraceRecorderSelfTest {
    static void execute(MiniForth vm, String src) {
        vm.input = new Scanner(src);
        vm.input.useDelimiter("(\\s|#.*)+");
        try {while(vm.input.hasNext())vm.word(vm.next());}
        finally {vm.input.close();}
    }
    static void check(Path srcPath) throws Exception {
        String src=new String(Files.readAllBytes(srcPath),StandardCharsets.UTF_8);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        PrintStream original=System.out;
        MiniForth base=new MiniForth();
        try {System.setOut(new PrintStream(out,true,"UTF-8"));execute(base,src);}
        finally {System.setOut(original);}
        Path tmp=Files.createTempFile("miniforth-recorder-selftest-", ".json");
        TraceRecorder rec=new TraceRecorder(srcPath.getFileName().toString(),src,tmp,20000);
        try {System.setOut(rec.capture);execute(rec,src);}
        finally {System.setOut(original);rec.finish("");}
        if(!Arrays.equals(base.mem,rec.mem)) {
            for(int i=0;i<base.mem.length;i++)if(base.mem[i]!=rec.mem[i])
                throw new AssertionError(srcPath+" Speicherzelle "+i+": "+base.mem[i]+" != "+rec.mem[i]);
        }
        if(!Arrays.equals(base.strings,rec.strings))throw new AssertionError("String-Tabelle: "+srcPath);
        if(!Arrays.equals(out.toByteArray(),rec.output.toByteArray()))throw new AssertionError("Ausgabe: "+srcPath);
        if(base.fp!=rec.fp||base.ip!=rec.ip||base.here!=rec.here||base.dp!=rec.dp||
           base.dictionary!=rec.dictionary||base.currentClass!=rec.currentClass||
           base.currentMethod!=rec.currentMethod||base.localSlots!=rec.localSlots||
           base.ctrl!=rec.ctrl||base.selectors!=rec.selectors||base.stringCount!=rec.stringCount)
           throw new AssertionError("Maschinenzeiger unterschiedlich: "+srcPath);
        Files.delete(tmp);
        System.out.println("OK "+srcPath.getFileName()+": "+rec.steps+
                " Opcodes | 65536 Speicherzellen | alle Zeiger | Strings | Ausgabe identisch");
    }
    public static void main(String[] args) throws Exception {
        check(Paths.get("beispiele/01_counter.mf"));
        check(Paths.get("beispiele/02_vererbung.mf"));
        check(Paths.get("beispiele/03_rekursion.mf"));
        System.out.println("3/3 volle Laufzeitzustände identisch mit MiniForth.java");
    }
}
