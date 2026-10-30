#!/usr/bin/env bash
# Drives the Pleos Connect Emulator v2.1.0 vehicle state through the Virtual Console HTTP API,
# for repeatable QA and demo scenarios (see docs/API_FACTS.md). Runs in Git Bash, macOS, Linux.
#
#   tools/vehicle-sim.sh park
#   tools/vehicle-sim.sh drive             # gear D, speed 0 -> platform "idling", game blocked
#   tools/vehicle-sim.sh speed 60          # km/h, needs gear D -> "moving"
#   tools/vehicle-sim.sh odometer 12345    # total distance in km
#   tools/vehicle-sim.sh soc 80            # EV battery %
#   tools/vehicle-sim.sh charge on|off
#   tools/vehicle-sim.sh location 37.5665 126.9780   # latitude longitude (emulator GPS)
#   tools/vehicle-sim.sh state
set -euo pipefail

BASE="http://127.0.0.1:8088"

# The console rejects writes with 503 unless a browser session has sent a heartbeat within ~3s.
# Values written during the session persist after it lapses.
heartbeat() { curl -s -o /dev/null -X POST "$BASE/browser/session?client=vehicle-sim"; }

put() {
  local code
  heartbeat
  code=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE$1")
  if [ "$code" != "200" ]; then echo "FAIL PUT $1 -> HTTP $code" >&2; exit 1; fi
  echo "OK  PUT $1"
}

need() { if [ -z "${1:-}" ]; then echo "missing value; see usage at the top of $0" >&2; exit 2; fi; }

cmd="${1:-}"; v1="${2:-}"; v2="${3:-}"

# The Virtual Console listens inside the emulator; the forward is lost on emulator restart.
[ "$cmd" != "location" ] && adb forward tcp:8088 tcp:8088 > /dev/null

case "$cmd" in
  park)
    put "/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0"
    put "/vehicle/gear?gear=Park" ;;
  drive)    put "/vehicle/gear?gear=Drive" ;;
  speed)    need "$v1"; put "/vehicle/controls?speed=${v1}&accelerator=0&brake=0&steering=0" ;;
  odometer) need "$v1"; put "/vehicle/odometer?traveled_distance_km=${v1}" ;;
  soc)      need "$v1"; put "/vehicle/ev-battery?field=soc&value=${v1}" ;;
  charge)
    need "$v1"; [ "$v1" = "on" ] && on=true || on=false
    put "/vehicle/ev-battery?field=charge_switch&value=${on}" ;;
  location)
    need "$v1"; need "$v2"
    adb emu geo fix "$v2" "$v1" > /dev/null   # emulator console expects longitude first
    echo "OK  GPS -> lat $v1, lon $v2" ;;
  state)
    curl -s "$BASE/vehicle/state" | python -c '
import json, sys
s = json.load(sys.stdin)
print("gear={} speed={}km/h odometer={}km soc={}% charging={}".format(
    s["gear"]["actual"]["value"], s["controls"]["speed_kph"], s["odometer"]["traveled_distance_km"],
    s["ev_battery"]["state_of_charge"], s["ev_battery"]["charging_activated"]))'
    adb shell dumpsys car_service --services CarDrivingStateService | grep "Current Driving State" | sed 's/^ *//; s/$/  (0=parked 1=idling 2=moving)/'
    adb shell dumpsys car_service --services CarUxRestrictionsManagerService | grep -m1 "UXR:" | sed 's/^ *//' ;;
  *)
    sed -n '2,13p' "$0"; exit 2 ;;
esac
