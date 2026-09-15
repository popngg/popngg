"""Read official music pages and the public catalog; prepare a guarded genre-only SQL review.

No database writes. Uses only Python's standard library.
"""
import argparse
import concurrent.futures
import csv
import html
import json
import re
import urllib.request
from pathlib import Path

OFFICIAL = "https://p.eagate.573.jp/game/popn/popn29/music/list.html"
API = "https://api.popn.gg/api/v1/charts"


def fetch(url):
    with urllib.request.urlopen(url, timeout=30) as response:
        return response.read().decode("utf-8")


def parse_music(document):
    table = re.search(r'<ul class="mu_list_table">(.*?)</ul>', document, re.S)
    if not table:
        raise ValueError("Official music table is absent")
    rows = re.findall(r'<li><p>(.*?)</p><p>(.*?)</p><p>(.*?)</p></li>', table[1], re.S)
    if not rows:
        raise ValueError("Official music table is empty")
    return [dict(zip(("genre", "title", "artist"),
                     (html.unescape(re.sub(r'<[^>]+>', '', v)).strip() for v in row)))
            for row in rows]


def text_key(value):
    # Keep punctuation and width significant; ambiguous/mismatched metadata needs review.
    return re.sub(r'\s*\(UPPER\)$', '', value, flags=re.I).strip()


def sql(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", type=int, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    base = f"{OFFICIAL}?version={args.version}&sort=music&sort_type=up"
    first = fetch(base)
    pagination = re.search(r'<select name="page_sl".*?</select>', first, re.S)
    if not pagination:
        raise ValueError("Official pagination is absent")
    pages = sorted(set(int(v) for v in re.findall(r'<option value="(\d+)"', pagination[0])))
    if pages != list(range(len(pages))):
        raise ValueError("Unexpected official pagination")
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        documents = [first] + list(pool.map(fetch, [f"{base}&page={p}" for p in pages[1:]]))
    official = [row for document in documents for row in parse_music(document)]
    by_identity = {}
    for row in official:
        key = (text_key(row["title"]), row["artist"])
        by_identity.setdefault(key, set()).add(text_key(row["genre"]))

    first_api = json.loads(fetch(f"{API}?page=1&size=100"))["data"]
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        rest = list(pool.map(fetch, [f"{API}?page={p}&size=100"
                                    for p in range(2, first_api["totalPages"] + 1)]))
    catalog = first_api["items"] + [r for page in rest for r in json.loads(page)["data"]["items"]]
    if len({row["songId"] for row in catalog}) != len(catalog):
        raise ValueError("Catalog pagination returned duplicate song IDs; retry against a stable catalog")
    changes, review = [], []
    for row in catalog:
        if row["version"] != args.version:
            continue
        genres = by_identity.get((text_key(row["title"]), row["artist"]), set())
        if len(genres) != 1:
            review.append({"songId": row["songId"], "title": row["title"], "reason": "missing or ambiguous official identity"})
            continue
        genre = next(iter(genres))
        if text_key(row["genre"]) != genre:
            changes.append(dict(song_id=row["songId"], song_name=row["title"],
                                artist_name=row["artist"], version=row["version"],
                                old_genre=row["genre"], new_genre=genre))
    changes.sort(key=lambda row: row["song_id"])
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "official.json").write_text(json.dumps(official, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (args.output / "changes.csv").open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["song_id", "song_name", "artist_name", "version", "old_genre", "new_genre"])
        writer.writeheader()
        writer.writerows(changes)
    statements = ["-- Genre refresh from " + base,
                  "-- Preserve song/chart IDs, existing song hashes, jacket URLs and all playdata.",
                  "-- Each update requires the reviewed identity and old genre; newer edits are not overwritten."]
    for row in changes:
        statements.append("UPDATE songs SET genre_name = " + sql(row["new_genre"]) + ", updated_at = CURRENT_TIMESTAMP\n"
                          + " WHERE song_id = " + str(row["song_id"]) + " AND version = " + str(row["version"])
                          + "\n   AND BINARY song_name = BINARY " + sql(row["song_name"])
                          + " AND BINARY artist_name = BINARY " + sql(row["artist_name"])
                          + "\n   AND BINARY genre_name = BINARY " + sql(row["old_genre"]) + ";")
    (args.output / "migration.sql").write_text("\n\n".join(statements) + "\n", encoding="utf-8")
    report = dict(version=args.version, official_rows=len(official), catalog_rows=len(catalog),
                  changes=len(changes), review=review)
    (args.output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
