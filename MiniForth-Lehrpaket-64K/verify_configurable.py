"""Prüft, dass drei MAX-Parameter ohne weitere Java-Änderung auf 24 wachsen."""
from pathlib import Path
import subprocess
import tempfile

source = Path(__file__).with_name('MiniForth.java').read_text(encoding='utf-8')
old = 'MAX_METHODS=16, MAX_FIELDS=16, MAX_LOCALS=16'
new = 'MAX_METHODS=24, MAX_FIELDS=24, MAX_LOCALS=24'
if old not in source:
    raise SystemExit('Einstellungszeile nicht gefunden')
program = ('class A\n'
           + ''.join(' field f%d\n' % i for i in range(20))
           + ''.join(' : m%d ( -- n ) %d ;\n' % (i, i) for i in range(20))
           + ': test ( -- n )\n'
           + ''.join(' local v%d\n' % i for i in range(20))
           + ' 77 to v19 v19 ;\nend\n'
           + 'new A send m19 .\nnew A send test .\n')
with tempfile.TemporaryDirectory() as directory:
    dest = Path(directory)
    (dest / 'MiniForth.java').write_text(source.replace(old, new), encoding='utf-8')
    (dest / 'test.mf').write_text(program, encoding='utf-8')
    subprocess.run(['javac', '--release', '8', '-Xlint:-options',
                    '-encoding', 'UTF-8', '-d', str(dest), str(dest / 'MiniForth.java')],
                   check=True)
    result = subprocess.run(['java', '-cp', str(dest), 'MiniForth', str(dest / 'test.mf')],
                            capture_output=True, text=True, check=True)
    assert result.stdout == '19\n77\n', result.stdout
print('Konfiguration 24/24/24 erfolgreich: 20 Felder, 20 Methoden, 20 lokale Variablen')
