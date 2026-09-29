"""Collect an anonymized Lv48-50 research snapshot from public popn.gg APIs."""
import argparse
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path


def now():
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def get(base, path, attempts=4):
    url = base.rstrip("/") + path
    for attempt in range(attempts):
        try:
            request = urllib.request.Request(url, headers={
                "Accept": "application/json", "User-Agent": "popngg-achievement-research/1.0"})
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)["data"]
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            if attempt + 1 == attempts:
                raise
            time.sleep(.5 * 2 ** attempt)


def collect(output, base="https://api.popn.gg", workers=2):
    output.mkdir(parents=True, exist_ok=True)
    started = now()
    catalog, snapshot_ids = {}, set()
    for level in (48, 49, 50):
        data = get(base, f"/api/v1/ratings/charts?level={level}&metric=CPI")
        snapshot_ids.add(data["snapshotId"])
        for item in [*data.get("rankings", []), *data.get("held", [])]:
            chart = item["chart"]
            catalog[int(chart["chartId"])] = {
                "chartId": int(chart["chartId"]), "songId": int(chart["songId"]),
                "songName": chart["songName"], "genreName": chart["genreName"],
                "jacketUrl": chart.get("jacketUrl"), "level": int(chart["level"]),
                "difficulty": int(chart["difficulty"]), "upper": bool(chart["upper"])
            }
    if len(snapshot_ids) != 1:
        raise RuntimeError("RATING_SNAPSHOT_CHANGED_DURING_CATALOG_COLLECTION")

    users, page = [], 1
    while True:
        data = get(base, "/api/v1/users?" + urllib.parse.urlencode({"page": page, "size": 100}))
        users.extend(item["id"] for item in data["items"])
        if not data["hasNext"]:
            break
        page += 1

    def user_rows(pair):
        user_id, poptomo_id = pair
        data = get(base, "/api/v1/users/" + urllib.parse.quote(poptomo_id, safe="") + "/playdata")
        rows = []
        for record in data["playdata"]:
            chart_id = int(record["chartId"])
            if chart_id not in catalog:
                continue
            all_time = record.get("allTimeBest") or {}
            medal = record.get("medal") or {}
            rows.append({"userId": user_id, "chartId": chart_id,
                         "userExists": True, "profileExists": True,
                         "chartExists": True, "songExists": True,
                         "bot": False, "hidden": False, "deleted": False, "duplicate": False,
                         "medal": medal.get("code"), "score": all_time.get("score"),
                         "allTimeRankCode": all_time.get("rankCode")})
        return user_id, rows

    failures, collected = [], {}
    with ThreadPoolExecutor(max_workers=max(1, min(workers, 4))) as executor:
        futures = {executor.submit(user_rows, pair): pair[0] for pair in enumerate(users, 1)}
        for completed, future in enumerate(as_completed(futures), 1):
            try:
                user_id, rows = future.result(); collected[user_id] = rows
            except Exception as exception:
                failures.append({"userId": futures[future], "error": type(exception).__name__})
            if completed % 50 == 0:
                print(f"users {completed}/{len(users)}", flush=True)
    if failures:
        (output / "collection-failures.json").write_text(json.dumps(failures, indent=2), "utf-8")
        raise RuntimeError(f"API_COLLECTION_INCOMPLETE: {len(failures)} users failed")

    (output / "catalog.json").write_text(json.dumps(list(catalog.values()), ensure_ascii=False, indent=2), "utf-8")
    record_count = 0
    with (output / "records.jsonl").open("w", encoding="utf-8", newline="\n") as stream:
        for user_id in sorted(collected):
            for row in collected[user_id]:
                stream.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
                record_count += 1
    metadata = {"source": "PUBLIC_API", "apiBase": base, "startedAt": started, "finishedAt": now(),
                "ratingSnapshotId": next(iter(snapshot_ids)), "userCount": len(users),
                "chartCount": len(catalog), "recordCount": record_count,
                "identityHandling": "Poptomo IDs and profile names were not persisted; userId is collection-local",
                "limitations": ["Not a transactionally consistent database snapshot",
                                "Only public directory users were collected",
                                "Best owned records are not individual play attempts"]}
    (output / "API_COLLECTION_METADATA.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), "utf-8")
    return metadata


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--api-base", default="https://api.popn.gg")
    parser.add_argument("--workers", type=int, default=2)
    args = parser.parse_args()
    print(json.dumps(collect(args.output, args.api_base, args.workers), ensure_ascii=False, indent=2))
