#!/usr/bin/env python3
"""Bytegenauer Vergleich aller Lehrbeispiele und der Speicherlupe mit dieser 64K-VM."""
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent
sources = [root / "MiniForth.java", root / "MemoryTour.java", root / "MemoryLimitsTests.java"]
failures = []
with tempfile.TemporaryDirectory() as temp:
    subprocess.run(["javac", "--release", "8", "-Xlint:-options", "-encoding", "UTF-8",
                    "-d", temp] + list(map(str, sources)), check=True)
    examples = sorted((root / "beispiele").glob("*.mf"))
    for program in examples:
        result = subprocess.run(["java", "-cp", temp, "MiniForth", str(program)],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
        wanted = program.with_suffix(".out.txt").read_bytes()
        if result.returncode != 0 or result.stdout != wanted:
            failures.append(program.name)
            print("FEHLER", program.name, result.stderr[:150])
    tour = subprocess.run(["java", "-cp", temp, "MemoryTour"],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
    if tour.returncode != 0 or tour.stdout != (root / "MemoryTour.out.txt").read_bytes():
        failures.append("MemoryTour")
    limits = subprocess.run(["java", "-cp", temp, "MemoryLimitsTests"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
    if limits.returncode != 0 or b"24/24" not in limits.stdout:
        failures.append("MemoryLimitsTests")

print(f"Lehrbeispiele: {len(examples)-len([x for x in failures if x.endswith('.mf')])}/{len(examples)}")
print("MemoryTour:", "OK" if "MemoryTour" not in failures else "FEHLER")
print("Interne Speicherpruefungen:", "OK" if "MemoryLimitsTests" not in failures else "FEHLER")
sys.exit(bool(failures))
