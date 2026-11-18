"""MiniForth-Locals: isolated regression tests, Python 3 standard library only."""
from pathlib import Path
import subprocess
import tempfile
import sys

ROOT = Path(__file__).resolve().parent
with tempfile.TemporaryDirectory() as outdir:
    compile_result = subprocess.run(['javac', '--release', '8', '-Xlint:-options',
                                     '-encoding', 'UTF-8', '-d', outdir,
                                     str(ROOT / 'MiniForth.java')],
                                     capture_output=True, text=True)
    if compile_result.returncode != 0:
        print('javac failed:', compile_result.stderr)
        sys.exit(2)
    pos = sorted((ROOT / 'tests').glob('*.mf'))
    neg = sorted((ROOT / 'tests' / 'negative').glob('*.mf'))
    fails = []
    for file in pos:
        p = subprocess.run(['java', '-cp', outdir, 'MiniForth', str(file)],
                           capture_output=True, timeout=12)
        want = file.with_suffix('.out.txt')
        if not want.exists():
            print('No output file:', file.name)
            fails.append((file.name, 'missing oracle'))
        elif p.returncode != 0 or p.stdout != want.read_bytes():
            fails.append((file.name, 'expected '+repr(want.read_bytes()[:120])+
                         ' got '+repr(p.stdout[:120])+' error '+repr(p.stderr[:240])))
    for file in neg:
        p = subprocess.run(['java', '-cp', outdir, 'MiniForth', str(file)],
                           capture_output=True, timeout=12)
        marker = file.with_suffix('.err.txt')
        fragment = marker.read_bytes() if marker.exists() else b'Exception'
        if p.returncode == 0 or fragment not in p.stderr:
            fails.append((file.name, 'expected error fragment '+repr(fragment)+
                         ', got '+repr(p.stderr[:200])+repr(p.stdout[:120])))
    print(f'Positive: {len(pos)} | Negative: {len(neg)} | Failed: {len(fails)}')
    for name, reason in fails:
        print('FAILED:', name, reason)
    sys.exit(bool(fails))
