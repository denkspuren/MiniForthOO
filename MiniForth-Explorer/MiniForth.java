import java.util.Scanner;
import java.io.File;
/** MiniForth: Objekte, Klassen, Bytecode und Frames in einem einzigen int[]. */
public class MiniForth {
    static final int HALT=0, PUSH=1, ADD=2, SUB=3, MUL=4, LESS=5, EQUAL=6,
        PRINT=7, RETURN=8, LOAD=9, STORE=10, GETFIELD=11, PUTFIELD=12,
        GETSTATIC=13, PUTSTATIC=14, NEW=15, VIRTUAL=16, SPECIAL=17,
        CALL=18, IFEQ=19, GOTO=20, STR=21, STRCAT=22, ITOS=23, PRINTS=24;
    static final String[] OPCODES = {"HALT", "PUSH", "ADD", "SUB", "MUL",
        "LESS", "EQUAL", "PRINT", "RETURN", "LOAD", "STORE", "GETFIELD",
        "PUTFIELD", "GETSTATIC", "PUTSTATIC", "NEW", "INVOKEVIRTUAL",
        "INVOKESPECIAL", "INVOKESTATIC", "IFEQ", "GOTO", "STR", "STRCAT",
        "ITOS", "PRINTS"};
    static final String[] PRIM = {"+", "-", "*", "<", "=", ".", ".s", "str", "concat"};
    static final int[] PRIMOP = {ADD, SUB, MUL, LESS, EQUAL, PRINT, PRINTS, ITOS, STRCAT};
    static final int LINK=0, KIND=1, OWNER=2, VALUE=3, LEN=4, CHARS=5;
    static final int CLASS=1, FIELD=2, STATIC=3, METHOD=4, STATIC_METHOD=5,
        LOCAL=6, SELECTOR=7, WORD=8;
    static final int MEMORY=65536, MAX_METHODS=16, MAX_FIELDS=16, MAX_LOCALS=16, OPERAND_CAPACITY=76;
    static final int SUPER=0, NFIELDS=1, NMETHODS=2, VTABLE=3, ENTRY=2;
    static final int NARGS=0, NRESULTS=1, LENGTH=2, CODE=3;
    static final int OLD_FP=0, RET_IP=1, FMETHOD=2, DEPTH=3, LOCAL0=4, STACK=LOCAL0+MAX_LOCALS;
    static final int DICT=128, HEAP=8192, FRAME=STACK+OPERAND_CAPACITY, SCRATCH=1;
    int[] mem = new int[MEMORY];
    String[] strings = new String[1024];
    int stringCount=1, dp=DICT, here=HEAP, ctrl=16, fp=mem.length-FRAME, ip;
    int dictionary, selectors, currentClass, currentMethod, localSlots;
    boolean trace;
    Scanner input;
    int define(String name, int kind, int owner, int value) {
        int start=dp, size=CHARS+name.length();
        if (dp+size>HEAP) throw new IllegalStateException("Dictionary voll");
        mem[dp++]=dictionary; mem[dp++]=kind; mem[dp++]=owner; mem[dp++]=value;
        mem[dp++]=name.length();
        for (int i=0; i<name.length(); i++) mem[dp++]=name.charAt(i); return dictionary=start;
    }
    String nameOf(int e) {
        String s="";
        for (int i=0; i<mem[e+LEN]; i++) s+=(char)mem[e+CHARS+i];
        return s;
    }
    int find(String name, int kind, int owner) {
        for (int e=dictionary; e!=0; e=mem[e+LINK])
            if (mem[e+KIND]==kind && mem[e+OWNER]==owner && nameOf(e).equals(name)) return e;
        return 0;
    }
    int member(String name, int kind, int cls) {
        for (int c=cls; c!=0; c=mem[c+SUPER]) {
            int e=find(name, kind, c); if (e!=0) return e;
        }
        return 0;
    }
    int klass(String name) {
        int e=find(name, CLASS, 0);
        if (e==0) throw new IllegalArgumentException("Unbekannte Klasse: " + name);
        return mem[e+VALUE];
    }
    int selector(String name) {
        int e=find(name, SELECTOR, 0);
        if (e==0) e=define(name, SELECTOR, 0, selectors++);
        return mem[e+VALUE];
    }
    int resolve(String name) {
        if (name.startsWith("this.")) return member(name.substring(5), FIELD, currentClass);
        int dot=name.indexOf('.');
        if (dot>0) {
            int c=klass(name.substring(0,dot));
            String part=name.substring(dot+1); int e=member(part, STATIC, c);
            if (e!=0) return e;
            e=member(part, STATIC_METHOD, c);
            return e!=0 ? e : member(part, METHOD, c);
        }
        int e=find(name, LOCAL, currentMethod);
        if (e!=0) return e;
        e=member(name, FIELD, currentClass); if (e!=0) return e;
        e=member(name, STATIC, currentClass); if (e!=0) return e;
        e=member(name, STATIC_METHOD, currentClass); if (e!=0) return e;
        return find(name, WORD, 0);
    }
    int allot(int cells) {
        if (here+cells>fp) throw new IllegalStateException("Heap und Frames treffen sich");
        for (int i=here; i<here+cells; i++) mem[i]=0; int result=here; here+=cells; return result;
    }
    int intern(String s) {
        for (int i=1; i<stringCount; i++) if (strings[i].equals(s)) return i;
        if (stringCount==strings.length) throw new IllegalStateException("String-Pool voll");
        strings[stringCount]=s; return stringCount++;
    }
    String next() { return input.next(); }
    void emit(int op, int arg) {
        int a=currentMethod==0 ? SCRATCH : allot(2); mem[a]=op; mem[a+1]=arg;
        if (currentMethod==0) { mem[a+2]=HALT; ip=a; run(); }
    }
    void beginClass(String name) {
        currentClass=allot(VTABLE+ENTRY*MAX_METHODS); define(name, CLASS, 0, currentClass);
        if (input.hasNext("extends")) {
            next(); int parent=klass(next()); mem[currentClass+SUPER]=parent;
            mem[currentClass+NFIELDS]=mem[parent+NFIELDS]; mem[currentClass+NMETHODS]=mem[parent+NMETHODS];
            System.arraycopy(mem,parent+VTABLE,mem,currentClass+VTABLE,ENTRY*MAX_METHODS);
        }
    }
    void field() {
        if (currentClass==0) throw new IllegalStateException("field nur in Klassen");
        if (mem[currentClass+NFIELDS]>=MAX_FIELDS) throw new IllegalStateException("Maximal "+MAX_FIELDS+" Instanzfelder");
        define(next(),FIELD,currentClass,mem[currentClass+NFIELDS]++);
    }
    void bindMethod(String name, int m) {
        int id=selector(name), n=mem[currentClass+NMETHODS];
        for (int i=0; i<n; i++) if (mem[currentClass+VTABLE+ENTRY*i]==id) {
            mem[currentClass+VTABLE+ENTRY*i+1]=m; return;
        }
        if (n>=MAX_METHODS) throw new IllegalStateException("Maximal "+MAX_METHODS+" virtuelle Methoden je Klasse");
        mem[currentClass+VTABLE+ENTRY*n]=id; mem[currentClass+VTABLE+ENTRY*n+1]=m; mem[currentClass+NMETHODS]=n+1;
    }
    int virtual(int cls, int id) {
        for (int i=0; i<mem[cls+NMETHODS]; i++) if (mem[cls+VTABLE+ENTRY*i]==id)
            return mem[cls+VTABLE+ENTRY*i+1];
        return 0;
    }
    void beginMethod(String name, boolean staticMethod) {
        int kind=currentClass==0 ? WORD : staticMethod ? STATIC_METHOD : METHOD;
        int e=define(name,kind,currentClass,0);
        int args=0, results=0;
        if (kind==METHOD) define("this", LOCAL, e, args++);
        if (!next().equals("(")) throw new IllegalArgumentException("Erwartet: (");
        String w=next(); while (!w.equals("--")) {
            if (w.equals("this") || find(w,LOCAL,e)!=0) throw new IllegalArgumentException("Ungültiger Parametername: " + w);
            if (args>=MAX_LOCALS) throw new IllegalStateException("Maximal "+MAX_LOCALS+" lokale Slots");
            define(w, LOCAL, e, args++); w=next();
        }
        for (w=next(); !w.equals(")"); w=next()) results++;
        int m=allot(CODE); mem[e+VALUE]=m; mem[m+NARGS]=args; mem[m+NRESULTS]=results;
        if (kind==METHOD) bindMethod(name,m);
        currentMethod=e; localSlots=args;
    }
    void local() {
        if (currentMethod==0) throw new IllegalStateException("local nur innerhalb einer Definition");
        String name=next();
        if (name.equals("this") || find(name, LOCAL, currentMethod)!=0) throw new IllegalArgumentException("Lokaler Name bereits belegt: " + name);
        if (localSlots>=MAX_LOCALS) throw new IllegalStateException("Maximal "+MAX_LOCALS+" lokale Slots");
        define(name, LOCAL, currentMethod, localSlots++);
    }
    void endMethod() {
        emit(RETURN,0); int m=mem[currentMethod+VALUE]; mem[m+LENGTH]=(here-m-CODE)/2;
        currentMethod=0; localSlots=0;
    }
    void control(String w) {
        if (currentMethod==0) throw new IllegalStateException(w+" nur innerhalb einer Methode");
        if (w.equals("if")) { emit(IFEQ,0); mem[ctrl++]=here-1; }
        else if (w.equals("else")) {
            int patch=mem[--ctrl]; emit(GOTO,0); mem[ctrl++]=here-1; mem[patch]=here;
        }
        else if (w.equals("then")) mem[mem[--ctrl]]=here;
    }
    void access(String name, boolean write) {
        int e=resolve(name); if (e==0) throw new IllegalArgumentException("Unbekannter Name: " + name);
        int kind=mem[e+KIND], v=mem[e+VALUE];
        if (kind==LOCAL) emit(write ? STORE : LOAD, v);
        else if (kind==FIELD) {
            if (find("this",LOCAL,currentMethod)==0) throw new IllegalStateException("Instanzfeld nur in Instanzmethoden");
            emit(LOAD,0); emit(write ? PUTFIELD : GETFIELD, v);
        }
        else if (kind==STATIC) emit(write ? PUTSTATIC : GETSTATIC, v);
        else if (!write && (kind==WORD || kind==STATIC_METHOD)) emit(CALL,v);
        else throw new IllegalArgumentException("Kein lesbarer/schreibbarer Name: " + name);
    }
    void word(String w) {
        for (int i=0; i<PRIM.length; i++)
            if (w.equals(PRIM[i])) { emit(PRIMOP[i],0); return; }
        if (w.equals("class")) beginClass(next());
        else if (w.equals("end")) currentClass=0;
        else if (w.equals("field")) field();
        else if (w.equals("static")) define(next(),STATIC,currentClass,allot(1));
        else if (w.equals(":")) beginMethod(next(),false);
        else if (w.equals("::")) beginMethod(next(),true);
        else if (w.equals("local")) local();
        else if (w.equals(";")) endMethod();
        else if (w.equals("if") || w.equals("else") || w.equals("then")) control(w);
        else if (w.equals("new")) emit(NEW,klass(next()));
        else if (w.equals("send")) emit(VIRTUAL,selector(next()));
        else if (w.equals(".o")) { emit(VIRTUAL,selector("toString")); emit(PRINTS,0); }
        else if (w.equals("super")) {
            if (find("this",LOCAL,currentMethod)==0) throw new IllegalStateException("super nur in Instanzmethoden");
            int e=member(next(),METHOD,mem[currentClass+SUPER]);
            if (e==0) throw new IllegalArgumentException("Unbekannte Oberklassenmethode");
            emit(LOAD,0); emit(SPECIAL,mem[e+VALUE]);
        }
        else if (w.equals("to")) access(next(),true);
        else if (w.equals("see")) {
            int e=resolve(next());
            if (e==0) throw new IllegalArgumentException("Unbekanntes Wort bei see");
            see(mem[e+VALUE]);
        }
        else if (w.equals("trace")) trace=!trace;
        else if (w.startsWith("\"")) {
            String text=w.substring(1);
            while (!text.endsWith("\"")) text += " " + next();
            emit(STR,intern(text.substring(0,text.length()-1)));
        }
        else {
            try { emit(PUSH,Integer.parseInt(w)); }
            catch (NumberFormatException ex) { access(w,false); }
        }
    }
    void push(int x) {
        int depth=mem[fp+DEPTH];
        if (depth>=FRAME-STACK) throw new IllegalStateException("Operandenstack voll");
        mem[fp+STACK+depth]=x; mem[fp+DEPTH]++;
    }
    int pop() {
        if (mem[fp+DEPTH]==0) throw new IllegalStateException("Operandenstack leer");
        return mem[fp+STACK+--mem[fp+DEPTH]];
    }
    void invoke(int method, int self) {
        if (method==0) throw new IllegalStateException("Methode fehlt");
        int f=fp-FRAME; if (f<here) throw new IllegalStateException("Callstack voll");
        for (int i=f; i<f+FRAME; i++) mem[i]=0;
        mem[f+OLD_FP]=fp; mem[f+RET_IP]=ip; mem[f+FMETHOD]=method;
        int first=self==0 ? 0 : 1; if (self!=0) mem[f+LOCAL0]=self;
        for (int i=mem[method+NARGS]-1; i>=first; i--) mem[f+LOCAL0+i]=pop();
        fp=f; ip=method+CODE; show("CALL");
    }
    void ret() {
        int method=mem[fp+FMETHOD], count=mem[method+NRESULTS], depth=mem[fp+DEPTH];
        if (depth!=count) throw new IllegalStateException("Falsche Ergebniszahl: " + depth + " statt " + count);
        int old=fp; ip=mem[old+RET_IP]; fp=mem[old+OLD_FP];
        for (int i=0; i<count; i++) push(mem[old+STACK+i]);
        show("RET");
    }
    String methodName(int m) {
        if (m==0) return "main";
        for (int e=dictionary; e!=0; e=mem[e+LINK])
            if (mem[e+VALUE]==m && (mem[e+KIND]==WORD || mem[e+KIND]==METHOD || mem[e+KIND]==STATIC_METHOD)) {
                int owner=mem[e+OWNER];
                for (int c=dictionary; c!=0; c=mem[c+LINK])
                    if (mem[c+KIND]==CLASS && mem[c+VALUE]==owner) return nameOf(c)+"."+nameOf(e);
                return nameOf(e);
            }
        return "?";
    }
    void show(String event) {
        if (!trace) return;
        System.out.println(event+" ip="+ip);
        for (int f=fp; f!=0; f=mem[f+OLD_FP]) {
            int m=mem[f+FMETHOD], n=0;
            if (m!=0) for (int e=dictionary; e!=0; e=mem[e+LINK])
                if (mem[e+KIND]==LOCAL && mem[e+OWNER]!=0 && mem[mem[e+OWNER]+VALUE]==m)
                    n=Math.max(n,mem[e+VALUE]+1);
            System.out.print("  "+methodName(m)+" locals:");
            for (int j=0; j<n; j++) System.out.print(" "+mem[f+LOCAL0+j]);
            System.out.print(" stack:");
            for (int j=0; j<mem[f+DEPTH]; j++) System.out.print(" "+mem[f+STACK+j]);
            System.out.println();
        }
    }
    void see(int m) {
        System.out.println(methodName(m)+" @"+m+":");
        for (int a=m+CODE; a<m+CODE+2*mem[m+LENGTH]; a+=2)
            System.out.println("  "+a+": "+OPCODES[mem[a]]+" "+mem[a+1]);
    }
    void run() {
        while (true) {
            int op=mem[ip++], arg=mem[ip++];
            if (op==HALT) return;
            else if (op==PUSH) push(arg);
            else if (op==ADD) push(pop()+pop());
            else if (op==SUB) { int y=pop(); push(pop()-y); }
            else if (op==MUL) push(pop()*pop());
            else if (op==LESS) { int y=pop(); push(pop()<y ? 1 : 0); }
            else if (op==EQUAL) push(pop()==pop() ? 1 : 0);
            else if (op==PRINT) System.out.println(pop());
            else if (op==LOAD) push(mem[fp+LOCAL0+arg]);
            else if (op==STORE) mem[fp+LOCAL0+arg]=pop();
            else if (op==GETFIELD) { int o=pop(); push(mem[o+1+arg]); }
            else if (op==PUTFIELD) { int o=pop(); mem[o+1+arg]=pop(); }
            else if (op==GETSTATIC) push(mem[arg]);
            else if (op==PUTSTATIC) mem[arg]=pop();
            else if (op==NEW) { int o=allot(1+mem[arg+NFIELDS]); mem[o]=arg; push(o); }
            else if (op==VIRTUAL) {
                int o=pop();
                if (o==0) throw new IllegalStateException("Null-Empfaenger");
                int m=virtual(mem[o],arg); invoke(m,o);
            }
            else if (op==SPECIAL) invoke(arg,pop());
            else if (op==CALL) invoke(arg,0);
            else if (op==IFEQ) { if (pop()==0) ip=arg; }
            else if (op==GOTO) ip=arg;
            else if (op==RETURN) ret();
            else if (op==STR) push(arg);
            else if (op==ITOS) push(intern(""+pop()));
            else if (op==STRCAT) { int r=pop(); push(intern(strings[pop()]+strings[r])); }
            else if (op==PRINTS) System.out.println(strings[pop()]);
        }
    }
    public static void main(String[] args) throws Exception {
        MiniForth vm=new MiniForth();
        vm.input=args.length>0 ? new Scanner(new File(args[0]),"UTF-8") : new Scanner(System.in);
        vm.input.useDelimiter("(\\s|#.*)+"); while (vm.input.hasNext()) vm.word(vm.next()); vm.input.close();
    }
}
