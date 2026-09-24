"""Synthetic, deterministic smoke-test input. Never represents real popn.gg users."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--output", required=True)
args = parser.parse_args()
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)
charts = [{"chartId": i, "songId": i, "level": 47 + (i - 1) // 12} for i in range(1, 49)]
(out / "catalog.json").write_text(json.dumps(charts), encoding="utf-8")
(out / "users.json").write_text(json.dumps(list(range(1, 61))), encoding="utf-8")
with (out / "records.jsonl").open("w", encoding="utf-8") as stream:
    for chart in charts:
        for user in range(1, 61):
            # Sparse level 50 and one entirely unplayed chart exercise coverage reporting.
            if chart["chartId"] == 48 or (chart["level"] == 50 and user > 20):
                continue
            medal = 7 if user > chart["chartId"] else 8
            if user == 1:
                medal = 11
            if user == 2:
                medal = 13
            row = dict(userId=user, chartId=chart["chartId"], songId=chart["songId"],
                       level=chart["level"], medal=medal,
                       score=0 if user == 2 else min(100000, 80000 + user * 300 - chart["chartId"] * 80),
                       userExists=True, profileExists=True, bot=False, hidden=False,
                       chartExists=True, songExists=True, deleted=False, duplicate=False,
                       currentVersion=29, allTimeScoreVersion=28, versionScore=0,
                       versionScoreKnown=False, lastPlayedAt=None,
                       recordUpdatedAt="2026-09-23 00:00:00", lastRenewLogId=None)
            stream.write(json.dumps(row) + "\n")
(out / "FIXTURE_NOT_PRODUCTION.txt").write_text(
    "Synthetic fixture only. These are NOT real popn.gg player statistics.\n", encoding="utf-8")
print(f"Synthetic input written to {out.resolve()}")
