#!/usr/bin/env python3
"""Testet die MiniForth-Anwendungen; braucht nur Java und die Python-Standardbibliothek.

1. Die eingebundenen Bibliotheksbloecke sind aktuell (einbinden.py --check).
2. Jedes Beispiel liefert bytegenau seine Sollausgabe (beispiele/*.out.*).
3. Unabhaengige Gegenrechnungen in Python: Division, Trigonometrie, Brueche,
   Ausdrucksbaum, SVG-Struktur, Nim-Zaehlungen.
4. Falls jshell vorhanden ist: Nim gegen das Java-Original tests/nim-java/nim.java.

Aufruf:  python3 run_tests.py            alles pruefen
         python3 run_tests.py --write    Sollausgaben neu schreiben (danach pruefen!)
"""
from fractions import Fraction
from functools import lru_cache
from itertools import product
from math import cos, factorial, radians, sin
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
EX = ROOT / 'beispiele'
results = []


def report(name, ok, detail=''):
    results.append(ok)
    print(f'{"ok    " if ok else "FEHLER"}  {name}' + (f'  ({detail})' if detail else ''))


def run_mf(path, stdin=''):
    proc = subprocess.run(['java', '-cp', str(ROOT), 'MiniForth', str(path)], input=stdin,
                          capture_output=True, text=True, encoding='utf-8', timeout=120)
    if proc.returncode:
        raise RuntimeError(proc.stderr.strip().splitlines()[-1] if proc.stderr.strip() else 'Abbruch')
    return proc.stdout


def run_text(source):
    with tempfile.NamedTemporaryFile('w', suffix='.mf', delete=False, encoding='utf-8') as f:
        f.write(source)
    try:
        return run_mf(f.name)
    finally:
        Path(f.name).unlink()


def java_div(a, b):
    q = abs(a) // abs(b)
    q = q if (a < 0) == (b < 0) else -q
    return q, a - q * b


def numbers(text):
    return [int(x) for x in text.split()]


# ---------------------------------------------------------------- Beispiele
def check_examples(write):
    for src in sorted(EX.glob('*.mf')):
        expected = src.with_name(src.stem + ('.out.svg' if 'turtle' in src.stem else '.out.txt'))
        try:
            out = run_mf(src)
        except RuntimeError as e:
            report(f'Beispiel {src.name}', False, str(e))
            continue
        if write:
            expected.write_text(out, encoding='utf-8')
        report(f'Beispiel {src.name}', expected.read_text(encoding='utf-8') == out,
               f'{len(out.splitlines())} Zeilen')


def check_interactive():
    moves = '3 4 5 leer row row row neu\nboard help\nboard 0 1 move show\nboard play\n'
    src = (EX / '08-nim-negamax.mf').read_text(encoding='utf-8')
    proc = subprocess.run(['java', '-cp', str(ROOT), 'MiniForth'], input=src + moves,
                          capture_output=True, text=True, encoding='utf-8', timeout=60)
    tail = proc.stdout.splitlines()[-9:]
    want = ['Dies ist eine Gewinnposition.', 'Vorschlag: Entferne 2 Sticks aus Reihe 0.',
            '0: I I', '1: I I I I', '2: I I I I I', 'Computer entfernt 1 Stick aus Reihe 0.',
            '0: I', '1: I I I I', '2: I I I I I']
    report('Nim interaktiv ueber die Standardeingabe', tail == want)


# ---------------------------------------------------- unabhaengige Pruefungen
def check_arith():
    lines = run_mf(ROOT / 'tests' / 'arith-test.mf').split('\n')[:-1]
    bad = [l for l in lines if (lambda a, b, q, r: java_div(a, b) != (q, r))(*numbers(l))]
    report('Division und Rest wie in Java', not bad and len(lines) == 26, f'{len(lines)} Faelle')


def check_trig():
    v = numbers(run_mf(ROOT / 'tests' / 'trig-test.mf'))
    rows = [v[i:i + 5] for i in range(0, len(v), 5)]
    err = {}
    for k, name, f in [(1, 'sin Bhaskara', sin), (2, 'sin Taylor', sin), (3, 'cos Bhaskara', cos),
                       (4, 'cos Taylor', cos)]:
        err[name] = max(abs(r[k] / 10000 - f(radians(r[0]))) for r in rows)
    ok = len(rows) == 721 and max(err['sin Bhaskara'], err['cos Bhaskara']) < 0.0017 \
        and max(err['sin Taylor'], err['cos Taylor']) < 0.0003
    report('Sinus und Kosinus gegen math.sin/cos', ok,
           'groesster Fehler Bhaskara %.5f, Taylor %.5f' % (err['sin Bhaskara'], err['sin Taylor']))


def decimal(fr, k):
    sign = '-' if fr < 0 else ''
    n, d = abs(fr.numerator), fr.denominator
    digits = ''.join(str(n % d * 10 ** (i + 1) // d % 10) for i in range(k))
    return f'{sign}{n // d}.{digits}'


def show(fr):
    return str(fr.numerator) if fr.denominator == 1 else f'{fr.numerator}/{fr.denominator}'


def check_fractions():
    F = Fraction
    h = sum(F(1, k) for k in range(1, 11))
    e = sum(F(1, factorial(k)) for k in range(13))
    xs = [F(1)]
    for _ in range(4):
        xs.append((xs[-1] + 2 / xs[-1]) / 2)
    want = ['Grundrechenarten:', show(F(6, 8)), show(F(2, -4)), show(F(1, 2) + F(1, 3)),
            show(F(1, 2) - F(3, 4)), show(F(3, 4) * F(2, 3)), show(F(1, 2) / F(1, 4)), '1',
            'Harmonische Zahl H(10) = 1 + 1/2 + ... + 1/10:', show(h), decimal(h, 6),
            'Eulersche Zahl e als Summe 1/0! + 1/1! + ... + 1/12!:', show(e), decimal(e, 9),
            'Wurzel aus 2 nach Newton: x -> (x + 2/x) / 2'] + [show(x) for x in xs] + [decimal(xs[-1], 11)]
    report('Brueche gegen fractions.Fraction', run_mf(EX / '04-brueche.mf').splitlines() == want)


def check_expressions():
    out = run_mf(EX / '02-ausdrucksbaum.mf').splitlines()
    f = lambda x: 3 * x * x + 2 * x + 1
    table = [f'f({x}) = {f(x)}' for x in range(5)]
    ok = out[2:7] == table and out[out.index("f'(2) =") + 1] == str(6 * 2 + 2) \
        and out[out.index('Zweite Ableitung:') + 1] == '6'
    report('Ausdrucksbaum: Werte und Ableitungen', ok)


def check_svg():
    path = EX / '06-turtle.out.svg'
    root = ET.parse(path).getroot()
    paths = [p for p in root if p.tag.endswith('path')]
    tokens = [t for p in paths for t in p.get('d').split()]
    coords = [int(t) for t in tokens if t not in 'ML']
    ok = len(paths) == 4 and tokens.count('L') == 1474 and \
        all(0 <= c <= 900 for c in coords[0::2]) and all(0 <= c <= 640 for c in coords[1::2])
    report('SVG wohlgeformt, 4 Pfade, Koordinaten im Bild', ok, f'{tokens.count("L")} Linien')


def check_nim_counts():
    @lru_cache(None)
    def wins(pos):
        return any(not wins(tuple(sorted(pos[:i] + (pos[i] - k,) + pos[i + 1:])))
                   for i in range(len(pos)) for k in range(1, pos[i] + 1))
    boards = [b for b in product(range(8), repeat=4) if sum(b) <= 16]
    bouton = sum(wins(tuple(sorted(b))) == ((b[0] ^ b[1] ^ b[2] ^ b[3]) != 0) for b in boards)
    positions = len({tuple(sorted(x for x in b if x)) for b in boards}) - 1
    out = run_mf(EX / '09-nim-vergleich.mf').splitlines()
    got = [int(out[i]) for i in (3, 5, 9)]
    report('Nim-Zaehlungen gegen Python-Minimax', got == [len(boards), bouton, positions],
           f'{len(boards)} Bretter, {positions} Stellungen')


def check_java():
    if not shutil.which('jshell'):
        report('Nim gegen nim.java', True, 'uebersprungen: jshell nicht gefunden')
        return
    d = ROOT / 'tests' / 'nim-java'
    java = subprocess.run(['jshell', '-q', '-R-ea', 'alle.jsh'], cwd=d, capture_output=True, text=True)
    expected = [numbers(l) for l in java.stdout.splitlines() if re.match(r'^\d+( -?\d+){7}$', l)]
    base = (d / 'alle.mf').read_text(encoding='utf-8')
    for strategy in ('Negamax', 'Xor'):
        v = numbers(run_text(base + f'new {strategy} to E.e demo\n'))
        got = [v[i:i + 8] for i in range(0, len(v), 8)]
        report(f'Nim gegen nim.java ({strategy})', len(expected) == 2870 and got == expected,
               f'{len(expected)} Bretter: Zug, Bewertung, Schluessel')


def main():
    write = '--write' in sys.argv
    javac = subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(ROOT), str(ROOT / 'MiniForth.java')],
                           capture_output=True, text=True)
    if javac.returncode:
        print(javac.stderr)
        return 2
    inc = subprocess.run([sys.executable, str(ROOT / 'einbinden.py'), '--check'], capture_output=True, text=True)
    report('Bibliotheksbloecke aktuell', inc.returncode == 0, inc.stdout.strip())
    check_examples(write)
    check_interactive()
    for test in (check_arith, check_trig, check_fractions, check_expressions, check_svg,
                 check_nim_counts, check_java):
        try:
            test()
        except Exception as e:                       # ein Fehler soll die anderen Tests nicht verdecken
            report(test.__name__, False, repr(e))
    print(f'\nGesamt: {sum(results)}/{len(results)} bestanden')
    return 0 if all(results) else 1


if __name__ == '__main__':
    sys.exit(main())
