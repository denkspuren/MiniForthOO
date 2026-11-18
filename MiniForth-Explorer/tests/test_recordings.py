"""Standardbibliothek: Original-MiniForth mit externem Recorder vergleichen."""
from pathlib import Path
import hashlib
import json
import subprocess
import tempfile

BASE = Path(__file__).resolve().parents[1]
PROGRAMS = sorted((BASE / 'beispiele').glob('*.mf'))
assert len(PROGRAMS) >= 3
for prog in PROGRAMS:
    original = subprocess.run(['java','-cp',str(BASE/'build'),'MiniForth',str(prog)],
                              capture_output=True, check=True)
    with tempfile.TemporaryDirectory() as tmp:
        trace = Path(tmp)/'test.json'
        r = subprocess.run(['java','-cp',str(BASE/'build'),'TraceRecorder',str(prog),str(trace)],
                           capture_output=True,check=True)
        d = json.loads(trace.read_text())
        assert d['summary']['output'].encode('utf-8') == original.stdout, prog
        assert d['source'] == prog.read_text(), prog
        assert d['sourceSha256'] == hashlib.sha256(prog.read_bytes()).hexdigest(),prog
        assert d['summary']['error'] == '', prog
        assert d['summary']['instructions'] > 0, prog
        print(f"OK {prog.name}: {d['summary']['events']} Ereignisse, "
              f"{d['summary']['instructions']} Befehle, Ausgabe identisch")
print(f'{len(PROGRAMS)}/{len(PROGRAMS)} Demos mit Originalmaschine abgeglichen')
