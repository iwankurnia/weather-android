"""Builds app/src/main/assets/kelurahan.csv: Jabodetabek kelurahan/desa codes with BMKG's own coordinates.

Codes and names: cahyadsn/wilayah db/wilayah.sql (Kepmendagri 2025, MIT licence).
Coordinates: BMKG open data (api.bmkg.go.id), one request per code, kept under 60 requests a minute.
Resumable: rows already in the output file are skipped on a re-run.

Usage: python3 tools/build_kelurahan.py <wilayah.sql> <output.csv>
"""
import csv, json, os, re, subprocess, sys, time

PREFIXES = ("31.", "32.01.", "32.16.", "32.71.", "32.75.", "32.76.", "36.03.", "36.71.", "36.74.")
API = "https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4="
FIELDS = ["code", "kelurahan", "kecamatan", "kota", "lat", "lon"]

def codes(sql_path):
    text = open(sql_path, encoding="utf-8").read()
    for code, name in re.findall(r"\('(\d{2}\.\d{2}\.\d{2}\.\d{4})','([^']*)'\)", text):
        if code.startswith(PREFIXES):
            yield code, name

def fetch(code):
    for attempt in range(4):
        try:
            # curl rather than urllib: macOS's bundled Python TLS is too old for api.bmkg.go.id
            out = subprocess.run(["curl", "-sf", "--max-time", "20", API + code], capture_output=True, check=True).stdout
            return json.loads(out)["lokasi"]
        except Exception as e:  # noqa: BLE001 - retry any network or parse failure
            if attempt == 3:
                print("give up", code, e, file=sys.stderr)
                return None
            time.sleep(5 * (attempt + 1))

def main(sql_path, out_path):
    done = set()
    if os.path.exists(out_path):
        with open(out_path, newline="", encoding="utf-8") as f:
            done = {row["code"] for row in csv.DictReader(f)}
    todo = [(c, n) for c, n in codes(sql_path) if c not in done]
    print(f"{len(done)} done, {len(todo)} to fetch", flush=True)
    new_file = not done
    with open(out_path, "a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        if new_file:
            w.writeheader()
        for i, (code, name) in enumerate(todo, 1):
            started = time.monotonic()
            loc = fetch(code)
            if loc and loc.get("lat") is not None:
                w.writerow({"code": code, "kelurahan": loc.get("desa") or name, "kecamatan": loc.get("kecamatan", ""),
                            "kota": loc.get("kotkab", ""), "lat": round(float(loc["lat"]), 5), "lon": round(float(loc["lon"]), 5)})
                f.flush()
            if i % 50 == 0:
                print(f"{i}/{len(todo)}", flush=True)
            time.sleep(max(0.0, 1.1 - (time.monotonic() - started)))
    print("finished", flush=True)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
