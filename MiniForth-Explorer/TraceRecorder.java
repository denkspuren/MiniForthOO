import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** External instruction recorder: MiniForth.java and the .mf source are never edited.
 *  This subclass mirrors ONLY MiniForth.run() so every executed opcode can be observed.
 *  All compiler, dictionary, invoke(), ret(), field and object operations still use
 *  the original MiniForth methods. No dependencies beyond the JDK (Java 8+).
 */
public class TraceRecorder extends MiniForth {
    static class LimitReached extends RuntimeException { private static final long serialVersionUID=1L; }
    final BufferedWriter json;
    final int[] previous = mem.clone();
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintStream capture;
    int prevStrings=stringCount, prevOutput=0, events=0, steps=0, maxEvents;
    int prevIp=ip, prevFp=fp, prevHere=here, prevDp=dp, prevDict=dictionary;
    int prevClass=currentClass, prevMethod=currentMethod, prevLocals=localSlots;
    int prevCtrl=ctrl, prevSelector=selectors;
    String token="(Start)", failure="", source;
    boolean truncated=false, first=true;
    final String sourceName;

    TraceRecorder(String sourceName, String source, Path target, int max) throws IOException {
        this.sourceName=sourceName; this.source=source; this.maxEvents=max;
        this.json=Files.newBufferedWriter(target, StandardCharsets.UTF_8);
        this.capture = new PrintStream(output, true, "UTF-8");
        json.write("{\"format\":\"MiniForth-Trace-v1\",\"programName\":"); quote(json,sourceName);
        json.write(",\"sourceSha256\":");quote(json,sha256(source.getBytes(StandardCharsets.UTF_8)));
        json.write(",\"source\":");quote(json,source);
        json.write(",\"config\":{\"memory\":"+MEMORY+",\"dictionaryStart\":"+DICT+
                ",\"heapStart\":"+HEAP+",\"frameSize\":"+FRAME+
                ",\"localStart\":"+LOCAL0+",\"operandStart\":"+STACK+
                ",\"maxMethods\":"+MAX_METHODS+",\"maxFields\":"+MAX_FIELDS+
                ",\"maxLocals\":"+MAX_LOCALS+",\"opcodes\":[");
        for (int i=0;i<OPCODES.length;i++) {if(i>0)json.write(','); quote(json,OPCODES[i]);}
        json.write("],\"kindNames\":[\"UNUSED\",\"CLASS\",\"FIELD\",\"STATIC\",\"METHOD\",\"STATIC_METHOD\",\"LOCAL\",\"SELECTOR\",\"WORD\"]},\"initial\":{\"state\":");
        state(json);
        json.write(",\"cells\":[]},\"events\":[\n");
    }
    static void quote(Writer w, String s) throws IOException {
        w.write('"');
        for(int i=0;i<s.length();i++) {char c=s.charAt(i);
            if(c=='"'||c=='\\'){w.write('\\');w.write(c);}
            else if(c=='\n')w.write("\\n");else if(c=='\r')w.write("\\r");
            else if(c=='\t')w.write("\\t");else if(c<32)w.write(String.format("\\u%04x",(int)c));
            else w.write(c);
        }w.write('"');
    }
    void state(Writer w) throws IOException {
        w.write("{\"ip\":"+ip+",\"fp\":"+fp+",\"here\":"+here+",\"dp\":"+dp+
              ",\"dictionary\":"+dictionary+",\"currentClass\":"+currentClass+
              ",\"currentMethod\":"+currentMethod+",\"localSlots\":"+localSlots+
              ",\"ctrl\":"+ctrl+",\"selectors\":"+selectors+
              ",\"stringCount\":"+stringCount+"}");
    }
    boolean metadataChanged(){return prevIp!=ip||prevFp!=fp||prevHere!=here||prevDp!=dp||
            prevDict!=dictionary||prevClass!=currentClass||prevMethod!=currentMethod||
            prevLocals!=localSlots||prevCtrl!=ctrl||prevSelector!=selectors;}
    void remember(){prevIp=ip;prevFp=fp;prevHere=here;prevDp=dp;prevDict=dictionary;
        prevClass=currentClass;prevMethod=currentMethod;prevLocals=localSlots;
        prevCtrl=ctrl;prevSelector=selectors;}
    /** A reversible delta: [physical address, old cell value, new cell value]. */
    void snapshot(String kind,String label,int address,int opcode,int argument,int objectAddress,int objectClass,boolean force) {
        try {
            ArrayList<int[]> changes=new ArrayList<int[]>();
            // Every memory write is captured, including the compiler's scratch space and zeroing new frames.
            for(int a=0;a<mem.length;a++)if(previous[a]!=mem[a]) {
                changes.add(new int[]{a,previous[a],mem[a]});previous[a]=mem[a];
            }
            int nStrings=stringCount-prevStrings;
            byte[] bytes=output.toByteArray();
            String added=bytes.length>prevOutput ? new String(bytes,prevOutput,bytes.length-prevOutput,"UTF-8") : "";
            prevOutput=bytes.length;
            if(!force&&changes.isEmpty()&&nStrings==0&&added.isEmpty()&&!metadataChanged())return;
            if(events>=maxEvents){truncated=true;throw new LimitReached();}
            if(!first)json.write(",\n");first=false;
            json.write("{\"type\":");quote(json,kind);
            json.write(",\"label\":");quote(json,label);
            json.write(",\"token\":");quote(json,token);
            json.write(",\"address\":"+address+",\"opcode\":"+opcode+",\"argument\":"+argument);
            if(objectAddress>=0)json.write(",\"allocation\":{\"address\":"+objectAddress+",\"classAddress\":"+objectClass+"}");
            json.write(",\"state\":");state(json);
            json.write(",\"writes\":[");
            for(int i=0;i<changes.size();i++){int[] c=changes.get(i);
                if(i>0)json.write(',');json.write("["+c[0]+","+c[1]+","+c[2]+"]");}
            json.write("],\"strings\":[");
            for(int j=prevStrings;j<stringCount;j++){
                if(j>prevStrings)json.write(',');json.write("["+j+",");quote(json,strings[j]);json.write(']');
            }
            json.write("],\"output\":");quote(json,added);
            json.write('}');
            prevStrings=stringCount;remember();events++;
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    @Override void word(String w){
        token=w;
        try {super.word(w);snapshot("source","Übersetzt: "+w,-1,-1,0,-1,0,false);}
        catch(LimitReached e){throw e;}
        catch(RuntimeException e){snapshot("error","Fehler bei "+w+": "+e.getMessage(),-1,-1,0,-1,0,true);throw e;}
    }
    /** The only duplicated code is run(); it follows MiniForth.run() opcode for opcode. */
    @Override void run(){
        while(true){
            // Emit pending compiler changes BEFORE the instruction (e.g., literal -> scratch program).
            snapshot("prepare","Direktmodus / Vorbereitung",-1,-1,0,-1,0,false);
            int a=ip,op=mem[ip++],arg=mem[ip++],objectAddress=-1,objectClass=0;
            try{
                if(op==HALT){snapshot("instruction","HALT",a,op,arg,-1,0,true);return;}
                else if(op==PUSH)push(arg);
                else if(op==ADD)push(pop()+pop());
                else if(op==SUB){int y=pop();push(pop()-y);}
                else if(op==MUL)push(pop()*pop());
                else if(op==LESS){int y=pop();push(pop()<y?1:0);}
                else if(op==EQUAL)push(pop()==pop()?1:0);
                else if(op==PRINT)System.out.println(pop());
                else if(op==LOAD)push(mem[fp+LOCAL0+arg]);
                else if(op==STORE)mem[fp+LOCAL0+arg]=pop();
                else if(op==GETFIELD){int o=pop();push(mem[o+1+arg]);}
                else if(op==PUTFIELD){int o=pop();mem[o+1+arg]=pop();}
                else if(op==GETSTATIC)push(mem[arg]);
                else if(op==PUTSTATIC)mem[arg]=pop();
                else if(op==NEW){int o=allot(1+mem[arg+NFIELDS]);mem[o]=arg;push(o);
                    objectAddress=o;objectClass=arg;}
                else if(op==VIRTUAL){int o=pop();if(o==0)throw new IllegalStateException("Null-Empfaenger");
                    int m=virtual(mem[o],arg);invoke(m,o);}
                else if(op==SPECIAL)invoke(arg,pop());
                else if(op==CALL)invoke(arg,0);
                else if(op==IFEQ){if(pop()==0)ip=arg;}
                else if(op==GOTO)ip=arg;
                else if(op==RETURN)ret();
                else if(op==STR)push(arg);
                else if(op==ITOS)push(intern(""+pop()));
                else if(op==STRCAT){int r=pop();push(intern(strings[pop()]+strings[r]));}
                else if(op==PRINTS)System.out.println(strings[pop()]);
                else throw new IllegalArgumentException("Unbekannter Opcode: "+op);
                steps++;
                snapshot("instruction",OPCODES[op]+" "+arg,a,op,arg,objectAddress,objectClass,true);
            }catch(LimitReached e){throw e;}
            catch(RuntimeException e){snapshot("error","Opcode "+(op>=0&&op<OPCODES.length?OPCODES[op]:op)+": "+e.getMessage(),a,op,arg,objectAddress,objectClass,true);throw e;}
        }
    }
    void finish(String problem) throws IOException {
        this.failure=problem;
        json.write("\n],\"summary\":{\"events\":"+events+",\"instructions\":"+steps+
            ",\"truncated\":"+truncated+",\"error\":");quote(json,problem);
        json.write(",\"output\":");quote(json,new String(output.toByteArray(),StandardCharsets.UTF_8));
        json.write("}}");json.close();capture.close();
    }
    static String sha256(byte[] contents) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(contents);
            StringBuilder b=new StringBuilder();for(byte v:digest)b.append(String.format("%02x",v&255));return b.toString();
        }catch(Exception ex){throw new IllegalStateException(ex);}
    }
    public static void main(String[] args) throws Exception {
        if(args.length<2||args.length>3){
            System.err.println("Aufruf: java TraceRecorder programm.mf aufzeichnung.json [maxEreignisse=12000]");
            System.exit(2);
        }
        Path input=Paths.get(args[0]),target=Paths.get(args[1]);
        int limit=args.length==3?Integer.parseInt(args[2]):12000;
        if(limit<10||limit>200000)throw new IllegalArgumentException("Limit: 10 bis 200000 Ereignisse");
        String src=new String(Files.readAllBytes(input),StandardCharsets.UTF_8);
        TraceRecorder vm=new TraceRecorder(input.getFileName().toString(),src,target,limit);
        PrintStream original=System.out;String error="";
        try {
            System.setOut(vm.capture);
            vm.input=new Scanner(src);vm.input.useDelimiter("(\\s|#.*)+");
            while(vm.input.hasNext())vm.word(vm.next());
        }catch(LimitReached ex){vm.truncated=true;error="Aufzeichnungslimit von "+limit+" Ereignissen erreicht";}
        catch(Exception ex){error=ex.getClass().getSimpleName()+": "+ex.getMessage();}
        finally{if(vm.input!=null)vm.input.close();System.setOut(original);vm.finish(error);}
        System.err.println("Trace: "+target.toAbsolutePath()+" ("+vm.events+" Ereignisse, "+vm.steps+
                " Opcodes"+(vm.truncated?", begrenzt":"")+")");
        if(!error.isEmpty()&&!vm.truncated){System.err.println("Programmfehler: "+error);System.exit(1);}
    }
}
