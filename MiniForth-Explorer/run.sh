#!/usr/bin/env bash
# MiniForth Explorer – macOS/Linux, JDK 8+, ohne Maven, Gradle oder Server.
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
  echo 'Bitte Java JDK 8 oder neuer installieren (java und javac werden benötigt).' >&2
  exit 2
fi
mkdir -p build
javac -encoding UTF-8 -d build MiniForth.java TraceRecorder.java MemoryTour.java TraceRecorderSelfTest.java

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'HELP'
MiniForth Explorer – Offline-Aufzeichnung

  ./run.sh
       Zeichnet die eingebaute Vererbungsdemo neu auf.
       Öffnet viewer.html auf macOS im Browser.

  ./run.sh mein-programm.mf [ausgabe.json] [maxEreignisse]
       Erstellt eine Aufzeichnung. Das .mf bleibt unverändert.
       Standardlimit: 12000 Ereignisse.

  ./run.sh --memory
       Zeigt die ursprüngliche MemoryTour im Terminal.

  ./run.sh --test
       Vergleicht Original-VM und Recorder für die drei Demos,
       einschließlich aller 65536 Speicherzellen (nur Java).

Im Browser: viewer.html öffnen und die entstandene JSON-Datei auswählen.
Alles bleibt lokal. Ein Server wird nicht benötigt.
HELP
  exit 0
fi
if [[ "${1:-}" == "--memory" ]]; then
  java -cp build MemoryTour
  exit 0
fi
if [[ "${1:-}" == "--test" ]]; then
  java -cp build TraceRecorderSelfTest
  exit 0
fi
input="${1:-beispiele/02_vererbung.mf}"
if [[ ! -f "$input" ]]; then
  echo "Programm nicht gefunden: $input" >&2
  exit 2
fi
out="${2:-${input%.mf}.json}"
limit="${3:-12000}"
java -cp build TraceRecorder "$input" "$out" "$limit"
echo
echo "Öffne viewer.html im Browser, lade dort: $out"
if [[ $# -eq 0 ]] && [[ "$(uname -s)" == 'Darwin' ]]; then
  open "viewer.html"
fi
