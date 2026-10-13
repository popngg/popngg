"""Prepare the October 2026 catalog correction using read-only public sources."""
import argparse
import concurrent.futures
import csv
import json
import re
from pathlib import Path

from prepare_genre_refresh import API, OFFICIAL, fetch, parse_music, sql, text_key


def official_version(version):
    base = f"{OFFICIAL}?page=0&version={version}&lv=0&bemani=0&category=0&keyword=&sort=music&sort_type=up"
    first = fetch(base)
    pagination = re.search(r'<select name="page_sl".*?</select>', first, re.S)
    if not pagination:
        raise ValueError("Official pagination is absent")
    pages = sorted(set(map(int, re.findall(r'<option value="(\d+)"', pagination[0]))))
    if pages != list(range(len(pages))):
        raise ValueError("Unexpected pagination")
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        documents = [first] + list(pool.map(fetch, [base.replace("page=0", f"page={p}") for p in pages[1:]]))
    rows = []
    for document in documents:
        metadata = parse_music(document)
        table = re.search(r'<ul class="mu_list_table">(.*?)</ul>', document, re.S)[1]
        levels = re.findall(r'<li>((?:<p data-d=.*?</p>){4})</li>', table, re.S)
        if len(metadata) != len(levels):
            raise ValueError("Metadata/level row count mismatch")
        for row, block in zip(metadata, levels):
            row["levels"] = dict(re.findall(r'<span>(LIGHT|NORMAL|HYPER|EX)</span>(\d+|-)', block))
            if len(row["levels"]) != 4:
                raise ValueError("Incomplete difficulty row")
            row["upper"] = bool(re.search(r'\(UPPER\)$', row["title"], re.I))
            rows.append(row)
    return base, documents, rows


def write_csv(path, rows, fields):
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--snapshot", type=Path, help="Use reviewed authenticated TSV and catalog-before.json files offline")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.snapshot:
        catalog = json.loads((args.snapshot / "catalog-before.json").read_text(encoding="utf-8"))
    else:
        first = json.loads(fetch(f"{API}?page=1&size=100"))["data"]
        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
            rest = list(pool.map(fetch, [f"{API}?page={p}&size=100" for p in range(2, first["totalPages"] + 1)]))
        catalog = first["items"] + [row for page in rest for row in json.loads(page)["data"]["items"]]
    if len({(r["songId"], r["isUpper"]) for r in catalog}) != len(catalog):
        raise ValueError("Duplicate catalog identities")
    selected = [r for r in catalog if r["version"] in (28, 29)]
    (args.output / "catalog-before.json").write_text(json.dumps(selected, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    genres, levels, review, summaries, artists = [], [], [], [], []
    statements = ["-- Official catalog correction verified on 2026-10-13.",
                  "-- Preserve IDs, hashes, jackets, scores and history. Guard reviewed identities and old values."]
    labels = {1: "LIGHT", 2: "NORMAL", 3: "HYPER", 4: "EX"}
    for version in (28, 29):
        if args.snapshot:
            base = f"{OFFICIAL}?version={version}&sort=music&sort_type=up"
            documents, official = [], []
            for line in (args.snapshot / f"authenticated-{version}.tsv").read_text(encoding="utf-8").splitlines():
                fields = line.split("\t")
                row = dict(zip(("genre", "title", "artist"), fields[:3]))
                row["upper"] = bool(re.search(r'\(UPPER\)$', row["title"], re.I))
                row["levels"] = dict(zip(labels.values(), fields[3].split(","))) if len(fields) == 4 else {}
                official.append(row)
            supplemental = args.snapshot / f"user-confirmed-{version}.tsv"
            if supplemental.exists():
                for line in supplemental.read_text(encoding="utf-8").splitlines():
                    genre, title, artist = line.split("\t")
                    official.append(dict(genre=genre, title=title, artist=artist, upper=False, levels={}, user_confirmed=True))
        else:
            base, documents, official = official_version(version)
        for page, document in enumerate(documents):
            (args.output / f"official-{version}-{page}.html").write_text(document, encoding="utf-8")
        (args.output / f"official-{version}.json").write_text(json.dumps(official, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        by_identity = {}
        for row in official:
            key = (text_key(row["title"]), row["artist"], row["upper"])
            by_identity.setdefault(key, []).append(row)
        matched = 0
        compared_charts = 0
        for row in selected:
            if row["version"] != version:
                continue
            matches = by_identity.get((text_key(row["title"]), row["artist"], row["isUpper"]), [])
            if not matches and not row["artist"]:
                matches = [s for s in official if text_key(s["title"]) == text_key(row["title"]) and s["upper"] == row["isUpper"]]
            if len(matches) != 1:
                review.append(dict(version=version, song_id=row["songId"], title=row["title"], reason="missing or ambiguous official title/artist/UPPER identity"))
                continue
            matched += 1
            source = matches[0]
            identity = (f"s.song_id = {row['songId']} AND s.version = {version}"
                        f" AND BINARY s.song_name = BINARY {sql(row['title'])}"
                        f" AND BINARY COALESCE(s.artist_name, '') = BINARY {sql(row['artist'])}")
            genre_changed = text_key(row["genre"]) != text_key(source["genre"])
            artist_changed = row["artist"] != source["artist"]
            if genre_changed:
                genres.append(dict(song_id=row["songId"], song_name=row["title"], artist_name=row["artist"], version=version, old_genre=row["genre"], new_genre=text_key(source["genre"])))
            if artist_changed:
                artists.append(dict(song_id=row["songId"], song_name=row["title"], old_artist=row["artist"], new_artist=source["artist"]))
            if genre_changed or artist_changed:
                statements.append(f"UPDATE songs s SET s.genre_name = {sql(text_key(source['genre']))}, s.artist_name = {sql(source['artist'])}, s.updated_at = CURRENT_TIMESTAMP\n WHERE {identity}\n   AND BINARY s.genre_name = BINARY {sql(row['genre'])};")
            if version != 29:
                continue
            if not source["levels"]:
                review.append(dict(version=version, song_id=row["songId"], title=row["title"], reason="User-confirmed genre; levels not available in authenticated official list"))
                continue
            for chart in row["charts"]:
                label = labels[chart["difficulty"]]
                new_level = source["levels"].get(label, "-")
                if new_level == "-":
                    review.append(dict(version=version, song_id=row["songId"], title=row["title"], reason=f"Official {label} chart absent"))
                    continue
                compared_charts += 1
                if chart["level"] == int(new_level):
                    continue
                levels.append(dict(song_id=row["songId"], song_name=row["title"], artist_name=row["artist"], chart_id=chart["chartId"], difficulty=label, old_level=chart["level"], new_level=int(new_level), is_upper=row["isUpper"]))
                statements.append(f"UPDATE charts c JOIN songs s ON s.song_id = c.song_id\n SET c.level = {int(new_level)}, c.updated_at = CURRENT_TIMESTAMP\n WHERE {identity}\n   AND c.chart_id = {chart['chartId']} AND c.difficulty_code = {chart['difficulty']}"
                                  f" AND c.chart_version = {chart['chartVersion']} AND c.is_upper = {int(row['isUpper'])}"
                                  f" AND c.is_deleted = FALSE AND c.level = {chart['level']};")
        summaries.append(dict(version=version, source=base, pages=(4 if version == 28 else 6) if args.snapshot else len(documents), authenticated=bool(args.snapshot), official_rows=sum(not r.get('user_confirmed', False) for r in official), user_confirmed_rows=sum(r.get('user_confirmed', False) for r in official), catalog_rows=sum(r['version'] == version for r in selected), matched=matched, compared_charts=compared_charts))
    write_csv(args.output / "changes.csv", genres, ["song_id", "song_name", "artist_name", "version", "old_genre", "new_genre"])
    write_csv(args.output / "level-changes.csv", levels, ["song_id", "song_name", "artist_name", "chart_id", "difficulty", "old_level", "new_level", "is_upper"])
    write_csv(args.output / "artist-changes.csv", artists, ["song_id", "song_name", "old_artist", "new_artist"])
    (args.output / "migration.sql").write_text("\n\n".join(statements) + "\n", encoding="utf-8")
    report = dict(sources=summaries, genre_changes=len(genres), artist_changes=len(artists), level_changes=levels, review=review)
    (args.output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(dict(sources=summaries, genre_changes=len(genres), level_changes=levels, review=review), ensure_ascii=False))


if __name__ == "__main__":
    main()
