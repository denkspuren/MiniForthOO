from pathlib import Path
out=Path(__file__).parent/'beispiele'
examples={
'01_stapel_rechnen':'''# Erste Worte: der Stack wird nur intern gebraucht.
2 3 + .
10 3 - .
4 7 < .
5 5 = .
''',
'02_square_bytecode':''': square ( x -- y )
    x x *
;
see square
3 square .
''',
'03_named_parameters':''': mixing ( a b -- result )
    a b + a b * +
;
2 3 mixing .
''',
'04_local_variable':''': test ( x -- result )
    local twice
    x 2 * to twice
    twice 1 +
;
see test
7 test .
''',
'05_multiple_results':''': pair ( x -- first second )
    x x 1 +
;
: add ( a b -- sum ) a b + ;
9 pair add .
see pair
''',
'06_if_else_patch':''': choose ( n -- result )
    n 0 =
    if 10 else 20 then
;
see choose
0 choose .
1 choose .
''',
'07_nested_if':''': classify ( n -- result )
    n 0 < if -1 else n 0 = if 0 else 1 then then
;
-3 classify .
0 classify .
7 classify .
''',
'08_recursive_fact':''': fact ( n -- result )
    n 1 < if 1 else n n 1 - fact * then
;
5 fact .
''',
'09_trace_recursion':''': down ( n -- result )
    n 0 = if 0 else n 1 - down 1 + then
;
trace
2 down .
trace
''',
'10_object_initialization':'''class Box
    field value
    : init ( n -- self ) n to value this ;
    : get ( -- result ) value ;
end
: demo ( -- )
    local box
    7 new Box send init to box
    box send get .
;
demo
''',
'11_box_fields':'''class Box
    field x
    field y
    : init ( x0 y0 -- self ) x0 to x y0 to y this ;
    : sum ( -- result ) x y + ;
end
: demo ( -- )
    local box
    3 4 new Box send init to box
    box send sum .
;
demo
''',
'12_local_aliasing':'''class Box
    field value
    : set ( n -- ) n to value ;
    : read ( -- n ) value ;
end
: demo ( -- )
    local first
    local second
    new Box to first
    first to second
    21 second send set
    first send read .
;
demo
''',
'13_parameter_shadow':'''class Box
    field x
    : set ( x -- ) x to this.x ;
    : toString ( -- s ) "Box(" x str concat ")" concat ;
end
: demo ( -- )
    local b
    new Box to b
    9 b send set
    b .o
;
demo
''',
'14_make_static_counter':'''class Counter
    field value
    static created
    :: make ( n -- obj )
        local o
        created 1 + to created
        new Counter to o
        n o send init
        o
    ;
    : init ( n -- ) n to value ;
    : toString ( -- s ) "Counter(" value str concat ")" concat ;
end
: demo ( -- )
    local a
    local b
    5 Counter.make to a
    8 Counter.make to b
    a .o
    b .o
    Counter.created .
;
demo
''',
'15_static_inheritance':'''class A
    static made
    :: create ( -- o ) made 1 + to made new A ;
end
class B extends A end
: demo ( -- )
    local a
    A.create to a
    A.made .
    B.made .
;
demo
''',
'16_inheritance_fields':'''class Base
    field a
    field b
    : setBase ( x y -- ) x to a y to b ;
    : baseSum ( -- s ) a b + ;
end
class Child extends Base
    field c
    : setC ( x -- ) x to c ;
    : total ( -- s ) this send baseSum c + ;
end
: demo ( -- )
    local o
    new Child to o
    3 4 o send setBase
    5 o send setC
    o send total .
;
demo
''',
'17_dispatch_virtual':'''class Base
    field n
    : inc ( -- ) n 1 + to n ;
    : twice ( -- ) this send inc this send inc ;
    : read ( -- r ) n ;
end
class Child extends Base
    : inc ( -- ) n 2 + to n ;
end
: demo ( -- )
    local a
    local b
    new Base to a
    new Child to b
    a send twice
    b send twice
    a send read .
    b send read .
;
demo
see Base.twice
demo
''',
'18_special_super':'''class Base
    field n
    : inc ( -- ) n 1 + to n ;
    : read ( -- x ) n ;
end
class Child extends Base
    : inc ( -- ) super inc super inc ;
end
: demo ( -- )
    local o
    new Child to o
    o send inc
    o send read .
;
demo
see Child.inc
demo
''',
'19_polymorphic_string':'''class Animal
    : toString ( -- s ) "Animal" ;
end
class Cat extends Animal
    : toString ( -- s ) "Cat: " super toString concat ;
end
new Animal .o
new Cat .o
see Cat.toString
''',
'20_strings_intern':'''"Hallo" "Hallo" = .
"Hallo" "Hal" "lo" concat = .
"Ergebnis: " 2 3 + str concat .s
''',
'21_local_string':''': hello ( -- )
    local msg
    "Guten Tag" to msg
    msg .s
;
hello
''',
'22_see_field':'''class Box
    field x
    : read ( -- r ) x ;
    : set ( n -- ) n to x ;
end
see Box.read
see Box.set
''',
'23_trace_local':''': helper ( n -- r )
    local tmp
    n 3 * to tmp
    tmp 2 +
;
trace
4 helper .
trace
''',
'24_library_list':'''class List
end
class Nil extends List
    : sum ( -- n ) 0 ;
    : length ( -- n ) 0 ;
    : toString ( -- s ) "Nil" ;
end
class Cons extends List
    field head
    field tail
    :: make ( h t -- o ) h t new Cons send init ;
    : init ( h t -- self ) h to head t to tail this ;
    : sum ( -- n ) head tail send sum + ;
    : length ( -- n ) tail send length 1 + ;
    : toString ( -- s )
        "Cons(" head str concat "," concat
        tail send toString concat ")" concat
    ;
end
: demo ( -- )
    local list
    3 new Nil Cons.make to list
    2 list Cons.make to list
    1 list Cons.make to list
    list send sum .
    list send length .
    list .o
;
demo
''',
'25_expression_tree':'''class Expr end
class Num extends Expr
    field value
    : init ( n -- self ) n to value this ;
    : eval ( -- n ) value ;
    : toString ( -- s ) value str ;
end
class Add extends Expr
    field left
    field right
    : init ( l r -- self ) l to left r to right this ;
    : eval ( -- n ) left send eval right send eval + ;
    : toString ( -- s ) "(" left send toString concat "+" concat right send toString concat ")" concat ;
end
: demo ( -- )
    local a
    local b
    local expr
    2 new Num send init to a
    3 new Num send init to b
    a b new Add send init to expr
    expr send eval .
    expr .o
;
demo
''',
'26_svg_line':''': line ( x1 y1 x2 y2 -- svg )
    "<line x1='" x1 str concat "' y1='" concat
    y1 str concat "' x2='" concat x2 str concat
    "' y2='" concat y2 str concat "' stroke='black'/>" concat
;
"<svg xmlns='http://www.w3.org/2000/svg' width='50' height='50'>" .s
1 2 30 40 line .s
"</svg>" .s
''',
'27_memory_first_word':''': square ( x -- y ) x x * ;
see square
''',
'28_many_local_slots':''': demo ( -- result )
    local a local b local c local d
    1 to a 2 to b 3 to c 4 to d
    a b + c + d +
;
demo .
''',
'29_explicit_this':'''class C
    field x
    : change ( x -- ) x to this.x ;
    : read ( -- n ) this.x ;
end
: demo ( -- )
    local c
    new C to c
    33 c send change
    c send read .
;
demo
''',
'30_methodname_recursion':'''class Math
    :: fib ( n -- r )
        n 2 < if n else n 1 - Math.fib n 2 - Math.fib + then
    ;
end
7 Math.fib .
''',
'31_global_word_and_static':''': triple ( n -- r ) n 3 * ;
class Math
    :: run ( n -- r ) n triple ;
end
6 Math.run .
''',
'32_field_access_more':'''class Coord
    field x
    field y
    : init ( xx yy -- self ) xx to x yy to y this ;
    : move ( dx dy -- ) x dx + to x y dy + to y ;
    : toString ( -- s ) "(" x str concat "," concat y str concat ")" concat ;
end
: demo ( -- )
    local p
    2 5 new Coord send init to p
    3 4 p send move
    p .o
;
demo
''',
'33_return_many':''': distribute ( n -- x y z ) n n 2 * n 3 * ;
: sum3 ( a b c -- n ) a b + c + ;
5 distribute sum3 .
''',
'34_no_shufflers':''': storeResult ( n -- result )
    local saved
    n 2 * to saved
    saved n +
;
8 storeResult .
''',
'35_big_demo':(Path('/mnt/data/MiniForth-64K/demo-ohne-shuffler.mf').read_text()),
}
for name,code in examples.items():
 (out/(name+'.mf')).write_text(code,encoding='utf-8')
print(f'Generated {len(examples)} program examples')
