#!/usr/bin/env python3
"""Bindet die Bibliotheken aus bibliothek/ in die Beispiele ein.

MiniForth liest genau eine Quelldatei. Damit jedes Beispiel fuer sich laeuft,
steht der Text der benutzten Bibliotheken woertlich im Beispiel, zwischen
    # >>> bibliothek/NAME.mf
    # <<< bibliothek/NAME.mf
Dieses Skript erneuert diese Bloecke (in beispiele/ und tests/) nach einer Aenderung an einer Bibliothek.
Mit --check wird nur geprueft, ob alle Bloecke aktuell sind.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent
BLOCK = re.compile(r'(# >>> (bibliothek/[\w-]+\.mf)[^\n]*\n)(.*?)(# <<< \2\n)', re.S)


def refresh(text):
    return BLOCK.sub(lambda m: m.group(1) + (ROOT / m.group(2)).read_text(encoding='utf-8')
                     + m.group(4), text)


def main():
    check = '--check' in sys.argv
    stale = []
    for path in sorted(list((ROOT / 'beispiele').glob('*.mf')) + list((ROOT / 'tests').rglob('*.mf'))):
        old = path.read_text(encoding='utf-8')
        new = refresh(old)
        if new != old:
            stale.append(path.name)
            if not check:
                path.write_text(new, encoding='utf-8')
    if check and stale:
        print('Nicht aktuell:', ', '.join(stale))
        return 1
    print('Bibliotheksbloecke ' + ('aktuell' if check else 'erneuert: ' + (', '.join(stale) or 'keine')))
    return 0


if __name__ == '__main__':
    sys.exit(main())
